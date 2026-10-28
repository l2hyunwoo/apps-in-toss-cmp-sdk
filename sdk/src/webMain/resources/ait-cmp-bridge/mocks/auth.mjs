export default {
  TossAuth: {
    login: async () => ({authorizationCode:'mock-not-for-server', referrer:'SANDBOX'}),
    isIntegrated: async () => true,
    sign: async () => { console.info('[CMP] Mock Toss certificate signing completed without a real signature.'); },
  },
};
