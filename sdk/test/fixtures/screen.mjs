const supported = fn => Object.assign(fn, {isSupported: () => true});
const screen = settle => ({Screen: {
  close: async () => settle(undefined),
  setAwakeMode: async ({enabled}) => settle({enabled}),
  setSecure: async ({enabled}) => settle({enabled}),
  setIosSwipeBack: async () => settle(undefined),
  setOrientation: supported(async () => settle(undefined)),
}});

export default {
  calls: {
    closeScreen: {payload: {}, capability: 'CLOSE', sdk: screen, expect: null},
    screenAwakeMode: {payload: {enabled: true}, invalid: [{}, {enabled: 'true'}], capability: 'SCREEN_AWAKE', sdk: screen, expect: {enabled: true}},
    screenSecure: {payload: {enabled: false}, invalid: [{}, {enabled: 0}], capability: 'SECURE_SCREEN', sdk: screen, expect: {enabled: false}},
    screenIosSwipeBack: {payload: {isEnabled: false}, invalid: [{enabled: false}], capability: 'IOS_SWIPE_BACK', sdk: screen, expect: null},
    screenOrientation: {payload: {type: 'landscape'}, invalid: [{type: 'LANDSCAPE'}, {}], capability: 'ORIENTATION', sdk: screen, expect: null},
  },
  streams: {},
};
