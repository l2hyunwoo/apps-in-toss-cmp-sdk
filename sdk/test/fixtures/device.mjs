export default {
  calls: {
    haptic: {payload: {type: 'confetti'}, invalid: [{type: 'boom'}], capability: 'HAPTIC', sdk: settle => ({Device: {triggerHaptic: async () => settle(undefined)}}), expect: null},
  },
  streams: {},
};
