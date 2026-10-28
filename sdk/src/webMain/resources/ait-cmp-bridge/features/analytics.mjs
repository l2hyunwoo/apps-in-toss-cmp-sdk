const LOG_TYPES = new Set(['debug', 'info', 'warn', 'error', 'event', 'screen', 'impression', 'click', 'popup']);

function requireParams(params) {
  if (params === null || typeof params !== 'object' || Array.isArray(params)) throw new TypeError('Log params must be an object');
  for (const value of Object.values(params)) {
    const ok = value === null || typeof value === 'string' || typeof value === 'boolean' || (typeof value === 'number' && Number.isFinite(value));
    if (!ok) throw new TypeError('Log param values must be string, finite number, boolean or null');
  }
}

function requireName(name) {
  if (typeof name !== 'string' || !name.trim()) throw new TypeError('Log name must be a non-blank string');
}

export default ({sdk, requireCapability}) => {
  // screen/impression/click take log_name next to the params, so a param cannot use that key.
  const logger = name => ({logName, params}) => {
    if (logName !== undefined) requireName(logName);
    requireParams(params);
    if (Object.hasOwn(params, 'log_name')) throw new TypeError('Use logName instead of a log_name param');
    return () => {
      requireCapability('ANALYTICS');
      return sdk.Analytics[name](logName === undefined ? params : {...params, log_name: logName});
    };
  };
  return {
    capabilities: {
      ANALYTICS: () => ['log', 'screen', 'impression', 'click'].every(name => typeof sdk.Analytics?.[name] === 'function'),
    },
    calls: {
      analyticsLog: ({logName, logType, params}) => {
        requireName(logName);
        if (!LOG_TYPES.has(logType)) throw new TypeError('Unknown log type');
        requireParams(params);
        return () => { requireCapability('ANALYTICS'); return sdk.Analytics.log({log_name: logName, log_type: logType, params}); };
      },
      analyticsScreen: logger('screen'),
      analyticsImpression: logger('impression'),
      analyticsClick: logger('click'),
    },
  };
};
