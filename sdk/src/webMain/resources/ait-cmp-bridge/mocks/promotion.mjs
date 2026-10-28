export default {
  Promotion: {
    grantReward: async ({promotionCode, amount}) => { console.info(`[CMP] Mock reward ${amount} granted for ${promotionCode}.`); return {key: 'mock-reward-key'}; },
  },
};
