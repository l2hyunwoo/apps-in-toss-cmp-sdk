function requireBoolean(value, name) {
  if (typeof value !== 'boolean') throw new TypeError(`${name} must be a boolean`);
}

export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    CLOSE: () => available(sdk.Screen?.close),
    SCREEN_AWAKE: () => available(sdk.Screen?.setAwakeMode),
    SECURE_SCREEN: () => available(sdk.Screen?.setSecure),
    IOS_SWIPE_BACK: () => available(sdk.Screen?.setIosSwipeBack),
    ORIENTATION: () => available(sdk.Screen?.setOrientation),
  },
  calls: {
    closeScreen: () => () => { requireCapability('CLOSE'); return sdk.Screen.close(); },
    screenAwakeMode: ({enabled}) => {
      requireBoolean(enabled, 'enabled');
      return () => { requireCapability('SCREEN_AWAKE'); return sdk.Screen.setAwakeMode({enabled}); };
    },
    screenSecure: ({enabled}) => {
      requireBoolean(enabled, 'enabled');
      return () => { requireCapability('SECURE_SCREEN'); return sdk.Screen.setSecure({enabled}); };
    },
    screenIosSwipeBack: ({isEnabled}) => {
      requireBoolean(isEnabled, 'isEnabled');
      return () => { requireCapability('IOS_SWIPE_BACK'); return sdk.Screen.setIosSwipeBack({isEnabled}); };
    },
    screenOrientation: ({type}) => {
      if (type !== 'portrait' && type !== 'landscape') throw new TypeError('type must be portrait or landscape');
      return () => { requireCapability('ORIENTATION'); return sdk.Screen.setOrientation({type}); };
    },
  },
});
