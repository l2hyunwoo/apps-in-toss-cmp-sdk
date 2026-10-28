const supported = fn => Object.assign(fn, {isSupported: () => true});

export default {
  calls: {
    login: {payload: {}, capability: 'TOSS_LOGIN', sdk: settle => ({TossAuth: {login: async () => settle({authorizationCode: 'c', referrer: 'SANDBOX'})}}), expect: {authorizationCode: 'c', referrer: 'SANDBOX'}},
    isIntegrated: {payload: {}, capability: 'TOSS_LOGIN_INTEGRATION', sdk: settle => ({TossAuth: {isIntegrated: supported(async () => settle(false))}}), expect: false},
    sign: {payload: {txId: 'tx'}, invalid: [{txId: ' '}, {}], capability: 'TOSS_SIGN', sdk: settle => ({TossAuth: {sign: supported(async () => settle(undefined))}}), expect: null},
  },
  streams: {},
};
