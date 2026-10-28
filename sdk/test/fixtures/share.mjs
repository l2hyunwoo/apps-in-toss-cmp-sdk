export default {
  calls: {
    share: {payload: {message: 'hi'}, invalid: [{message: ' '}, {}], capability: 'SHARE', sdk: settle => ({Share: {sendMessage: async () => settle(undefined)}}), expect: null},
  },
  streams: {},
};
