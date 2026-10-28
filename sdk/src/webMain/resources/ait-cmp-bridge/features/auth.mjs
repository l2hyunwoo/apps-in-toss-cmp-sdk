export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    TOSS_LOGIN: () => available(sdk.TossAuth?.login),
  },
  calls: {
    login: () => () => { requireCapability('TOSS_LOGIN'); return sdk.TossAuth.login(); },
  },
});
