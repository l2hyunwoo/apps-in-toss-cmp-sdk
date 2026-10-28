import assert from 'node:assert/strict';

const promotion = grant => ({Promotion: {grantReward: Object.assign(grant, {isSupported: () => true})}});
const payload = {promotionCode: 'P', amount: 10};

export default {
  calls: {
    promotionGrantReward: {
      payload,
      invalid: [{promotionCode: ' ', amount: 10}, {promotionCode: 'P', amount: '10'}, {promotionCode: 'P', amount: Infinity}],
      run: async ({createBridge, base, callBridge, tick}) => {
        await assert.rejects(callBridge(createBridge(base()), 'promotionGrantReward', payload), {code: 'UNSUPPORTED'});
        const sent = [];
        const ok = createBridge({...base(), ...promotion(async params => { sent.push(params); return {key: 'k', extra: 1}; })});
        assert.deepEqual(await callBridge(ok, 'promotionGrantReward', payload), {key: 'k'});
        assert.deepEqual(sent, [payload]);
        for (const code of ['4100', 'UNKNOWN_ERROR']) {
          const failed = createBridge({...base(), ...promotion(async () => { throw Object.assign(Error('private'), {code}); })});
          assert.deepEqual(await callBridge(failed, 'promotionGrantReward', payload), {errorCode: code});
        }
        const other = createBridge({...base(), ...promotion(async () => { throw Object.assign(Error('private'), {code: 'OTHER'}); })});
        await assert.rejects(callBridge(other, 'promotionGrantReward', payload), {code: 'SDK_FAILURE'});
        let release;
        const late = createBridge({...base(), ...promotion(() => new Promise(resolve => { release = () => resolve({key: 'k'}); }))});
        late.call('promotionGrantReward', JSON.stringify(payload), () => assert.fail('late success'), () => assert.fail('late failure'))();
        release();
        await tick();
      },
    },
  },
  streams: {},
};
