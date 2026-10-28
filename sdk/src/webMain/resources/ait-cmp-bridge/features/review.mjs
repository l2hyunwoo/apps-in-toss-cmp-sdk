export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    REVIEW: () => available(sdk.Review?.request),
  },
  calls: {
    reviewRequest: () => () => { requireCapability('REVIEW'); return sdk.Review.request(); },
  },
});
