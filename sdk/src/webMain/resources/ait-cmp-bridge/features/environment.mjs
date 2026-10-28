const ENVIRONMENT_VALUES = new Set(['deviceId', 'groupId', 'environment', 'tossAppVersion', 'deploymentId', 'initialURL']);
const VERSION = /^(\d+\.\d+\.\d+|always|never)$/;

export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    // `in` checks the getters without reading them; reading outside a host throws.
    ENVIRONMENT_INFO: () => typeof sdk.Environment === 'object' && [...ENVIRONMENT_VALUES].every(name => name in sdk.Environment) && typeof sdk.isMinVersionSupported === 'function',
    NETWORK_STATUS: () => available(sdk.Environment?.getNetworkStatus),
    SERVER_TIME: () => available(sdk.Environment?.getServerTime),
  },
  calls: {
    environmentValue: ({name}) => {
      if (!ENVIRONMENT_VALUES.has(name)) throw new TypeError('Unknown environment value');
      return () => { requireCapability('ENVIRONMENT_INFO'); return sdk.Environment[name]; };
    },
    isMinVersionSupported: ({android, ios}) => {
      if (![android, ios].every(v => typeof v === 'string' && VERSION.test(v))) throw new TypeError('Invalid minimum version');
      return () => { requireCapability('ENVIRONMENT_INFO'); return sdk.isMinVersionSupported({android, ios}); };
    },
    networkStatus: () => () => { requireCapability('NETWORK_STATUS'); return sdk.Environment.getNetworkStatus(); },
    serverTime: () => () => { requireCapability('SERVER_TIME'); return sdk.Environment.getServerTime(); },
  },
});
