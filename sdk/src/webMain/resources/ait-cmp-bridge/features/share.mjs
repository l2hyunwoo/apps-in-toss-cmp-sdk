export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    SHARE: () => available(sdk.Share?.sendMessage),
  },
  calls: {
    share: ({message}) => {
      if (typeof message !== 'string' || !message.trim()) throw new TypeError('Share text is required');
      return () => { requireCapability('SHARE'); return sdk.Share.sendMessage({message}); };
    },
  },
});
