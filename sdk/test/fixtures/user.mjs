const supported = fn => Object.assign(fn, {isSupported: () => true});
const consented = (settle, result = async () => ({USER_NAME: 'n'})) => ({User: {getConsentedData: supported(async () => settle(await result()))}});

export default {
  calls: {
    anonymousKey: {payload: {}, capability: 'USER_ID', sdk: settle => ({User: {getAnonymousKey: supported(async () => settle({type: 'HASH', hash: 'id'}))}}), expect: {type: 'HASH', hash: 'id'}},
    anonymousKeyAuthCode: {payload: {}, capability: 'USER_KEY_AUTH_CODE', sdk: settle => ({User: {createAnonymousKeyAuthCode: async () => settle({code: 'c', expiresIn: 300})}}), expect: {code: 'c', expiresIn: 300}},
    consentedData: {
      payload: {consentedUserDataKey: 'cud_delivery', shouldRequestAgreementWhenUserDeclined: true},
      invalid: [{consentedUserDataKey: ' '}, {}, {consentedUserDataKey: 'k', shouldRequestAgreementWhenUserDeclined: 'yes'}],
      run: async ({createBridge, base, callBridge, assert}) => {
        const bridge = result => createBridge({...base(), ...consented(v => v, result)});
        const payload = {consentedUserDataKey: 'k'};
        assert.equal(createBridge(base()).supports('CONSENTED_DATA'), false);
        await assert.rejects(callBridge(createBridge(base()), 'consentedData', payload), {code: 'UNSUPPORTED'});
        assert.deepEqual(await callBridge(bridge(), 'consentedData', payload), {data: {USER_NAME: 'n'}});
        // 문서화된 실패는 결과가 되고, 다른 오류는 세부 정보 없이 실패로 넘긴다.
        assert.deepEqual(await callBridge(bridge(async () => { throw {code: 'USER_DECLINED', message: 'private'}; }), 'consentedData', payload), {errorCode: 'USER_DECLINED'});
        await assert.rejects(callBridge(bridge(async () => { throw {code: 'OTHER', message: 'private'}; }), 'consentedData', payload), {code: 'SDK_FAILURE'});
        await assert.rejects(callBridge(bridge(async () => undefined), 'consentedData', payload), {code: 'UNSUPPORTED'});
        let release;
        const stop = bridge(() => new Promise(resolve => { release = resolve; })).call('consentedData', JSON.stringify(payload), () => assert.fail('late success'), () => assert.fail('late failure'));
        stop();
        release({USER_NAME: 'n'});
        await new Promise(setImmediate);
      },
    },
    declaredAgeRange: {
      payload: {ageGates: [13, 18]},
      invalid: [{ageGates: [13.5]}, {ageGates: '13'}, {}],
      capability: 'DECLARED_AGE_RANGE',
      sdk: settle => ({User: {getDeclaredAgeRange: supported(async () => settle({status: 'SHARING', lowerBound: 18, activeParentalControls: []}))}}),
      expect: {status: 'SHARING', lowerBound: 18, activeParentalControls: []},
    },
  },
  streams: {},
  absentInMin: ['USER_KEY_AUTH_CODE'],
};

