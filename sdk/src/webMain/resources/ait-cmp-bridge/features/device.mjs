const HAPTIC_TYPES = new Set(['tickWeak', 'tap', 'tickMedium', 'softMedium', 'basicWeak', 'basicMedium', 'success', 'error', 'wiggle', 'confetti']);
const DEVICE_VALUES = new Set(['locale', 'os']);

export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    HAPTIC: () => available(sdk.Device?.triggerHaptic),
    // `in`은 getter를 읽지 않고 확인한다. host 밖에서 읽으면 예외가 난다.
    DEVICE_INFO: () => typeof sdk.Device === 'object' && [...DEVICE_VALUES].every(name => name in sdk.Device),
    OPEN_URL: () => available(sdk.Device?.openURL),
  },
  calls: {
    haptic: ({type}) => {
      if (!HAPTIC_TYPES.has(type)) throw new TypeError('Unknown haptic type');
      return () => { requireCapability('HAPTIC'); return sdk.Device.triggerHaptic({type}); };
    },
    deviceValue: ({name}) => {
      if (!DEVICE_VALUES.has(name)) throw new TypeError('Unknown device value');
      return () => { requireCapability('DEVICE_INFO'); return sdk.Device[name]; };
    },
    openURL: ({url}) => {
      if (typeof url !== 'string' || !url.trim()) throw new TypeError('URL is required');
      return () => { requireCapability('OPEN_URL'); return sdk.Device.openURL(url); };
    },
  },
});
