export default {
  Device: {
    triggerHaptic: async () => {},
    locale: 'ko-KR',
    os: 'android',
    openURL: async url => { console.info(`[CMP] Mock openURL: ${url}`); },
  },
};
