export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    NOTIFICATION_AGREEMENT: () => available(sdk.Notification?.requestAgreement),
  },
  streams: {
    notificationAgreement: ({templateCode}) => {
      if (typeof templateCode !== 'string' || !templateCode.trim()) throw new TypeError('Template code is required');
      return ({emit, fail}) => {
        requireCapability('NOTIFICATION_AGREEMENT');
        return sdk.Notification.requestAgreement({options: {templateCode}, onEvent: result => emit({type: result?.type}), onError: fail});
      };
    },
  },
});
