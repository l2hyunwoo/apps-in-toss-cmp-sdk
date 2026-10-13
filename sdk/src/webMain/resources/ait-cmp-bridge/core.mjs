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
      const method = {TOSS_LOGIN: sdk.TossAuth?.login, SHARE: sdk.Share?.sendMessage, CLOSE: sdk.Screen?.close}[capability];
      return typeof method === 'function' && (typeof method.isSupported !== 'function' || method.isSupported());
    } catch { return false; }
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
