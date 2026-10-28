export default {
  calls: {
    haptic: {payload: {type: 'confetti'}, invalid: [{type: 'boom'}], capability: 'HAPTIC', sdk: settle => ({Device: {triggerHaptic: async () => settle(undefined)}}), expect: null},
    deviceValue: {payload: {name: 'locale'}, invalid: [{}, {name: 'getPhotos'}], capability: 'DEVICE_INFO', sdk: settle => ({Device: {get locale() { return settle('ko-KR'); }, get os() { throw Error('unread'); }}}), expect: 'ko-KR'},
    openURL: {payload: {url: 'https://toss.im'}, invalid: [{}, {url: ' '}, {url: 1}], capability: 'OPEN_URL', sdk: settle => ({Device: {openURL: async () => settle(undefined)}}), expect: null},
  },
  streams: {},
};
