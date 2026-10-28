export default {
  calls: {
    login: {payload: {}, capability: 'TOSS_LOGIN', sdk: settle => ({TossAuth: {login: async () => settle({authorizationCode: 'c', referrer: 'SANDBOX'})}}), expect: {authorizationCode: 'c', referrer: 'SANDBOX'}},
  },
  streams: {},
};
