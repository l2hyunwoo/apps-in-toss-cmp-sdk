// User.getConsentedData의 문서화된 실패다. `error.code`와 함께 throw된다.
const CONSENTED_DATA_ERRORS = new Set(['USER_DECLINED', 'UNAVAILABLE', 'TERMS_NOT_SET', 'INVALID_REQUEST', 'CANCELED', 'CONSENTED_USER_DATA_AGREEMENT_FAILED', 'CONSENTED_USER_DATA_INVALID_DATA']);

export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    USER_ID: () => typeof sdk.User?.getAnonymousKey === 'function' && sdk.User.getAnonymousKey.isSupported?.() === true,
    USER_KEY_AUTH_CODE: () => available(sdk.User?.createAnonymousKeyAuthCode),
    CONSENTED_DATA: () => available(sdk.User?.getConsentedData),
    DECLARED_AGE_RANGE: () => available(sdk.User?.getDeclaredAgeRange),
  },
  calls: {
    anonymousKey: () => () => { requireCapability('USER_ID'); return sdk.User.getAnonymousKey(); },
    anonymousKeyAuthCode: () => () => { requireCapability('USER_KEY_AUTH_CODE'); return sdk.User.createAnonymousKeyAuthCode(); },
    consentedData: ({consentedUserDataKey, shouldRequestAgreementWhenUserDeclined}) => {
      if (typeof consentedUserDataKey !== 'string' || !consentedUserDataKey.trim()) throw new TypeError('consentedUserDataKey is required');
      if (shouldRequestAgreementWhenUserDeclined !== undefined && typeof shouldRequestAgreementWhenUserDeclined !== 'boolean') throw new TypeError('shouldRequestAgreementWhenUserDeclined must be a boolean');
      const options = shouldRequestAgreementWhenUserDeclined === undefined ? {consentedUserDataKey} : {consentedUserDataKey, shouldRequestAgreementWhenUserDeclined};
      return () => {
        requireCapability('CONSENTED_DATA');
        return sdk.User.getConsentedData(options).then(data => {
          if (data == null) throw {code:'UNSUPPORTED'};
          return {data};
        }, error => {
          if (CONSENTED_DATA_ERRORS.has(error?.code)) return {errorCode: error.code};
          throw error;
        });
      };
    },
    declaredAgeRange: ({ageGates}) => {
      if (!Array.isArray(ageGates) || !ageGates.every(Number.isInteger)) throw new TypeError('ageGates must be an integer array');
      return () => { requireCapability('DECLARED_AGE_RANGE'); return sdk.User.getDeclaredAgeRange({ageGates}); };
    },
  },
});
