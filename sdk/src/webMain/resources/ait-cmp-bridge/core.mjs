const HAPTIC_TYPES = new Set(['tickWeak', 'tap', 'tickMedium', 'softMedium', 'basicWeak', 'basicMedium', 'success', 'error', 'wiggle', 'confetti']);

export function createBridge(sdk, { mock = false } = {}) {
  function host() {
    try {
      const env = sdk.Environment.environment;
      return env === 'toss' || env === 'sandbox' ? (mock ? 'mock' : env) : 'browser';
    } catch { return 'browser'; }
  }
  function errorPayload(error) {
    // Preserve recognized codes only; SDK messages can contain sensitive data.
    const known = new Set(['PERMISSION_DENIED', 'USER_CANCELLED', 'HOST_UNAVAILABLE', 'UNSUPPORTED']);
    return JSON.stringify({ code: known.has(error?.code) ? error.code : 'SDK_FAILURE' });
  }
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
  function supported(capability) {
    if (host() === 'browser') return false;
    try {
      if (capability === 'USER_ID') return typeof sdk.User?.getAnonymousKey === 'function' && sdk.User.getAnonymousKey.isSupported?.() === true;
      if (capability === 'STORAGE') return ['getItem', 'setItem', 'removeItem'].every(name => typeof sdk.Storage?.[name] === 'function');
      if (capability === 'LEADERBOARD') return [sdk.Game?.setLeaderboardScore, sdk.Game?.openLeaderboard].every(available);
      const method = {TOSS_LOGIN: sdk.TossAuth?.login, SHARE: sdk.Share?.sendMessage, CLOSE: sdk.Screen?.close, HAPTIC: sdk.generateHapticFeedback, GAME_PROFILE: sdk.Game?.getUserProfile}[capability];
      return available(method);
    } catch { return false; }
  }
  function available(method) {
    return typeof method === 'function' && (typeof method.isSupported !== 'function' || method.isSupported());
  }
  // Each entry validates its JSON payload, checks the capability, then calls the official SDK.
  const calls = {
    haptic: ({type}) => {
      if (!HAPTIC_TYPES.has(type)) throw new TypeError('Unknown haptic type');
      return () => { requireCapability('HAPTIC'); return sdk.generateHapticFeedback({type}); };
    },
    storageGet: ({key}) => { requireKey(key); return () => { requireCapability('STORAGE'); return sdk.Storage.getItem(key); }; },
    storageSet: ({key, value}) => {
      requireKey(key);
      if (typeof value !== 'string') throw new TypeError('Storage value must be a string');
      return () => { requireCapability('STORAGE'); return sdk.Storage.setItem(key, value); };
    },
    storageRemove: ({key}) => { requireKey(key); return () => { requireCapability('STORAGE'); return sdk.Storage.removeItem(key); }; },
    gameProfile: () => () => {
      requireCapability('GAME_PROFILE');
      return Promise.resolve(sdk.Game.getUserProfile()).then(result => { if (result == null) throw {code:'UNSUPPORTED'}; return result; });
    },
    leaderboardSubmit: ({score}) => {
      if (typeof score !== 'string' || !Number.isFinite(Number(score))) throw new TypeError('Score must be a finite number string');
      return () => {
        requireCapability('LEADERBOARD');
        return Promise.resolve(sdk.Game.setLeaderboardScore({score})).then(result => { if (result == null) throw {code:'UNSUPPORTED'}; return result; });
      };
    },
    leaderboardOpen: () => () => { requireCapability('LEADERBOARD'); return sdk.Game.openLeaderboard(); },
  };
  function requireKey(key) {
    if (typeof key !== 'string' || !key.trim()) throw new TypeError('Storage key is required');
  }
  function requireCapability(capability) {
    if (!supported(capability)) throw {code:'UNSUPPORTED'};
  }
  return {
    host,
    supports: supported,
    anonymousKey: (success, failure) => {
      return request(() => {
        requireCapability('USER_ID');
        return sdk.User.getAnonymousKey();
      }, success, failure);
    },
    login: (success, failure) => request(() => { requireCapability('TOSS_LOGIN'); return sdk.TossAuth.login(); }, success, failure),
    share: (text, success, failure) => {
      if (typeof text !== 'string' || !text.trim()) throw new TypeError('Share text is required');
      return request(() => { requireCapability('SHARE'); return sdk.Share.sendMessage({message: text}); }, success, failure);
    },
    closeScreen: (success, failure) => request(() => { requireCapability('CLOSE'); return sdk.Screen.close(); }, success, failure),
    call(method, payload, success, failure) {
      const prepare = Object.hasOwn(calls, method) ? calls[method] : null;
      if (!prepare) throw new TypeError('Unknown bridge method');
      return request(prepare(JSON.parse(payload)), success, failure);
    },
    subscribe(listener, failure) {
      let active = true;
      const removers = [];
      const emit = event => { if (active) listener(JSON.stringify(event)); };
      if (host() === 'browser') { failure(JSON.stringify({code:'HOST_UNAVAILABLE'})); return () => {}; }
      const cleanup = () => { active = false; for (const remove of removers.splice(0)) { try { remove(); } catch {} } };
      try {
        if (typeof sdk.graniteEvent?.addEventListener !== 'function' || typeof sdk.SafeArea?.subscribe !== 'function' || typeof sdk.SafeArea?.get !== 'function') throw {code:'UNSUPPORTED'};
        for (const [name, type] of [['backEvent','back'], ['homeEvent','home']]) {
          removers.push(sdk.graniteEvent.addEventListener(name, {onEvent: () => emit({type}), onError: e => { if (active) failure(errorPayload(e)); }}));
        }
        removers.push(sdk.SafeArea.subscribe({onEvent: insets => emit({type:'safeArea', ...insets})}));
        emit({type:'safeArea', ...sdk.SafeArea.get()});
      } catch (error) { cleanup(); failure(errorPayload(error)); }
      return cleanup;
    }
  };
}
