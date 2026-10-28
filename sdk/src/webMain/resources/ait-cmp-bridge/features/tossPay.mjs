function requireToken(token) {
  if (typeof token !== 'string' || !token.trim()) throw new TypeError('Token is required');
}

export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    TOSS_PAY: () => available(sdk.TossPay?.authorize),
    TOSS_PAY_SUBSCRIPTION: () => available(sdk.TossPay?.authorizeSubscription),
  },
  calls: {
    tossPayAuthorize: ({payToken}) => { requireToken(payToken); return () => { requireCapability('TOSS_PAY'); return sdk.TossPay.authorize({payToken}); }; },
    tossPayAuthorizeSubscription: ({wrappedToken}) => {
      requireToken(wrappedToken);
      return () => { requireCapability('TOSS_PAY_SUBSCRIPTION'); return sdk.TossPay.authorizeSubscription({wrappedToken}); };
    },
  },
});
