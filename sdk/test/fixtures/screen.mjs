export default {
  calls: {
    closeScreen: {payload: {}, capability: 'CLOSE', sdk: settle => ({Screen: {close: async () => settle(undefined)}}), expect: null},
  },
  streams: {},
};
