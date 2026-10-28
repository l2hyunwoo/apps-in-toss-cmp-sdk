export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    TOSS_LOGIN: () => available(sdk.TossAuth?.login),
    TOSS_LOGIN_INTEGRATION: () => available(sdk.TossAuth?.isIntegrated),
    TOSS_SIGN: () => available(sdk.TossAuth?.sign),
  },
  calls: {
    login: () => () => { requireCapability('TOSS_LOGIN'); return sdk.TossAuth.login(); },
    isIntegrated: () => () => {
      requireCapability('TOSS_LOGIN_INTEGRATION');
      return Promise.resolve(sdk.TossAuth.isIntegrated()).then(result => {
        if (typeof result !== 'boolean') throw {code:'UNSUPPORTED'};
        return result;
      });
    },
    sign: ({txId}) => {
      if (typeof txId !== 'string' || !txId.trim()) throw new TypeError('txId is required');
      return () => { requireCapability('TOSS_SIGN'); return sdk.TossAuth.sign({txId}); };
    },
  },
});
