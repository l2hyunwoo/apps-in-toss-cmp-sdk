const tossPay = settle => ({TossPay: {
  authorize: async () => settle({success: true}),
  authorizeSubscription: Object.assign(async () => settle({success: false, reason: 'declined'}), {isSupported: () => true}),
}});

export default {
  calls: {
    tossPayAuthorize: {payload: {payToken: 't'}, invalid: [{}, {payToken: ' '}, {payToken: 1}], capability: 'TOSS_PAY', sdk: tossPay, expect: {success: true}},
    tossPayAuthorizeSubscription: {payload: {wrappedToken: 'w'}, invalid: [{}, {wrappedToken: ''}], capability: 'TOSS_PAY_SUBSCRIPTION', sdk: tossPay, expect: {success: false, reason: 'declined'}},
  },
  streams: {},
};
