export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    CLOSE: () => available(sdk.Screen?.close),
  },
  calls: {
    closeScreen: () => () => { requireCapability('CLOSE'); return sdk.Screen.close(); },
  },
});
