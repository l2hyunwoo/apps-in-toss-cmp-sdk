export default ({sdk, requireCapability}) => ({
  capabilities: {
    USER_ID: () => typeof sdk.User?.getAnonymousKey === 'function' && sdk.User.getAnonymousKey.isSupported?.() === true,
  },
  calls: {
    anonymousKey: () => () => { requireCapability('USER_ID'); return sdk.User.getAnonymousKey(); },
  },
});
