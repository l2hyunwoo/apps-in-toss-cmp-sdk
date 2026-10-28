const HAPTIC_TYPES = new Set(['tickWeak', 'tap', 'tickMedium', 'softMedium', 'basicWeak', 'basicMedium', 'success', 'error', 'wiggle', 'confetti']);

export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    HAPTIC: () => available(sdk.Device?.triggerHaptic),
  },
  calls: {
    haptic: ({type}) => {
      if (!HAPTIC_TYPES.has(type)) throw new TypeError('Unknown haptic type');
      return () => { requireCapability('HAPTIC'); return sdk.Device.triggerHaptic({type}); };
    },
  },
});
