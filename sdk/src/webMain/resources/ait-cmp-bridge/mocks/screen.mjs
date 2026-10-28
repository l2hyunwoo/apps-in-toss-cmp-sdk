export default {
  Screen: {
    close: async () => {},
    setAwakeMode: async ({enabled}) => ({enabled}),
    setSecure: async ({enabled}) => ({enabled}),
    setIosSwipeBack: async () => {},
    setOrientation: async ({type}) => { console.info(`[CMP] Mock orientation ${type} is not applied.`); },
  },
};
