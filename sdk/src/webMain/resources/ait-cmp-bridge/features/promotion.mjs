// 문서화된 지급 실패는 숫자 코드나 UNKNOWN_ERROR다. 그 밖의 것은 bridge 실패로 남긴다.
const REWARD_ERROR = /^(\d+|UNKNOWN_ERROR)$/;

export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    PROMOTION_REWARD: () => available(sdk.Promotion?.grantReward),
  },
  calls: {
    promotionGrantReward: ({promotionCode, amount}) => {
      if (typeof promotionCode !== 'string' || !promotionCode.trim()) throw new TypeError('Promotion code is required');
      if (typeof amount !== 'number' || !Number.isFinite(amount)) throw new TypeError('Amount must be a finite number');
      return () => {
        requireCapability('PROMOTION_REWARD');
        return Promise.resolve(sdk.Promotion.grantReward({promotionCode, amount})).then(
          ({key}) => ({key}),
          error => {
            if (typeof error?.code === 'string' && REWARD_ERROR.test(error.code)) return {errorCode: error.code};
            throw error;
          });
      };
    },
  },
});
