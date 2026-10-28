import { FEATURES } from './features/index.mjs';

const KNOWN_ERRORS = new Set(['PERMISSION_DENIED', 'USER_CANCELLED', 'HOST_UNAVAILABLE', 'UNSUPPORTED']);

export function errorPayload(error) {
  // SDK 메시지에는 민감한 데이터가 들어 있을 수 있으므로 인식한 코드만 남긴다.
  return JSON.stringify({ code: KNOWN_ERRORS.has(error?.code) ? error.code : 'SDK_FAILURE' });
}

export function createBridge(sdk, { mock = false } = {}) {
  function host() {
    try {
      const env = sdk.Environment.environment;
      return env === 'toss' || env === 'sandbox' ? (mock ? 'mock' : env) : 'browser';
    } catch { return 'browser'; }
  }
  function available(method) {
    return typeof method === 'function' && (typeof method.isSupported !== 'function' || method.isSupported());
  }
  const capabilities = {};
  const calls = {};
  const streams = {};
  function supported(capability) {
    if (host() === 'browser' || !Object.hasOwn(capabilities, capability)) return false;
    try { return capabilities[capability]() === true; } catch { return false; }
  }
  function requireCapability(capability) {
    if (!supported(capability)) throw {code:'UNSUPPORTED'};
  }
  const context = { sdk, available, requireCapability };
  for (const feature of FEATURES) {
    const part = feature(context);
    for (const [target, entries] of [[capabilities, part.capabilities], [calls, part.calls], [streams, part.streams]]) {
      for (const [name, value] of Object.entries(entries ?? {})) {
        if (Object.hasOwn(target, name)) throw Error(`Duplicate bridge entry: ${name}`);
        target[name] = value;
      }
    }
  }

  const replies = new Map();
  let nextReply = 0;
  calls.reply = ({replyId, value}) => {
    if (typeof replyId !== 'string') throw new TypeError('replyId is required');
    return () => {
      const pending = replies.get(replyId);
      if (pending) { replies.delete(replyId); pending.resolve(value ?? pending.fallback); }
      return null;
    };
  };

  function request(action, success, failure) {
    let active = true;
    if (host() === 'browser') { failure(JSON.stringify({code:'HOST_UNAVAILABLE'})); return () => {}; }
    try {
      Promise.resolve(action()).then(value => {
        if (active) { active = false; success(JSON.stringify(value ?? null)); }
      }, error => { if (active) { active = false; failure(errorPayload(error)); } });
    } catch (error) { active = false; failure(errorPayload(error)); }
    return () => { active = false; };
  }

  function lookup(table, method) {
    const entry = Object.hasOwn(table, method) ? table[method] : null;
    if (!entry) throw new TypeError('Unknown bridge method');
    return entry;
  }

  return {
    host,
    supports: supported,
    methods: { calls: Object.keys(calls), streams: Object.keys(streams) },
    call(method, payload, success, failure) {
      return request(lookup(calls, method)(JSON.parse(payload)), success, failure);
    },
    stream(method, payload, onEvent, failure) {
      const start = lookup(streams, method)(JSON.parse(payload));
      if (host() === 'browser') { failure(JSON.stringify({code:'HOST_UNAVAILABLE'})); return () => {}; }
      let active = true;
      let release = null;
      const owned = new Set();
      const stop = () => {
        if (!active) return;
        active = false;
        // 대기 중인 host 질문은 fallback으로 끝내서 공식 SDK가 영원히 기다리지 않게 한다.
        for (const id of owned) { const pending = replies.get(id); replies.delete(id); pending?.resolve(pending.fallback); }
        owned.clear();
        const cleanup = release; release = null;
        try { cleanup?.(); } catch {}
      };
      const io = {
        emit: event => { if (active) onEvent(JSON.stringify(event)); },
        fail: error => { if (active) { stop(); failure(errorPayload(error)); } },
        ask: (event, fallback) => new Promise(resolve => {
          if (!active) { resolve(fallback); return; }
          const replyId = `r${++nextReply}`;
          owned.add(replyId);
          replies.set(replyId, { fallback, resolve: value => { owned.delete(replyId); resolve(value); } });
          onEvent(JSON.stringify({ ...event, replyId }));
        }),
      };
      try {
        const cleanup = start(io);
        if (active) release = cleanup; else try { cleanup?.(); } catch {}
      } catch (error) { io.fail(error); }
      return stop;
    },
  };
}
