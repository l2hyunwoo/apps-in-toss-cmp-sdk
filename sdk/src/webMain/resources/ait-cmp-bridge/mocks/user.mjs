export default {
  User: {
    getAnonymousKey: Object.assign(async () => ({type:'HASH',hash:'mock-local-user'}), {isSupported:()=>true}),
    createAnonymousKeyAuthCode: async () => ({code:'mock-not-for-server', expiresIn:300}),
    getConsentedData: async ({consentedUserDataKey}) => {
      console.info(`[CMP] Mock consented data for ${consentedUserDataKey}.`);
      return {USER_NAME:'모의 사용자'};
    },
    getDeclaredAgeRange: async () => ({status:'NOT_AVAILABLE', activeParentalControls:[]}),
  },
};
