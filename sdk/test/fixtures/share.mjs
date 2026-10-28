const share = settle => ({Share: {sendMessage: async () => settle(undefined), createLink: async () => settle('https://toss.im/l')}});

export default {
  calls: {
    share: {payload: {message: 'hi'}, invalid: [{message: ' '}, {}], capability: 'SHARE', sdk: share, expect: null},
    shareLink: {payload: {path: 'intoss://app', ogImageUrl: 'https://img'}, invalid: [{path: 'https://app'}, {}, {path: 'intoss://app', ogImageUrl: 1}], capability: 'SHARE_LINK', sdk: share, expect: 'https://toss.im/l'},
  },
  streams: {},
};
