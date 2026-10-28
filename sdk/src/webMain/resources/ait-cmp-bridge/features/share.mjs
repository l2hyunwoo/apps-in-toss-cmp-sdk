export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    SHARE: () => available(sdk.Share?.sendMessage),
    SHARE_LINK: () => available(sdk.Share?.createLink),
  },
  calls: {
    share: ({message}) => {
      if (typeof message !== 'string' || !message.trim()) throw new TypeError('Share text is required');
      return () => { requireCapability('SHARE'); return sdk.Share.sendMessage({message}); };
    },
    shareLink: ({path, ogImageUrl}) => {
      if (typeof path !== 'string' || !path.startsWith('intoss://')) throw new TypeError('path must start with intoss://');
      if (ogImageUrl !== undefined && (typeof ogImageUrl !== 'string' || !ogImageUrl.trim())) throw new TypeError('ogImageUrl must be a non-blank string');
      const params = ogImageUrl === undefined ? {path} : {path, ogImageUrl};
      return () => { requireCapability('SHARE_LINK'); return sdk.Share.createLink(params); };
    },
  },
});
