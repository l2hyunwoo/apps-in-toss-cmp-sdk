import assert from 'node:assert/strict';

export default {
  calls: {},
  streams: {
    notificationAgreement: {
      payload: {templateCode: 't'},
      run: async ({createBridge, base}) => {
        assert.throws(() => createBridge(base()).stream('notificationAgreement', '{"templateCode":" "}', () => {}, () => {}), TypeError);
        let error;
        createBridge(base()).stream('notificationAgreement', '{"templateCode":"t"}', () => assert.fail(), e => { error = JSON.parse(e); });
        assert.equal(error.code, 'UNSUPPORTED');

        const calls = []; let removed = 0;
        const sdk = {...base(), Notification: {requestAgreement: Object.assign(params => { calls.push(params); return () => removed++; }, {isSupported: () => true})}};
        const b = createBridge(sdk);
        assert.equal(b.supports('NOTIFICATION_AGREEMENT'), true);
        const events = [];
        const stop = b.stream('notificationAgreement', '{"templateCode":"t"}', e => events.push(JSON.parse(e)), () => assert.fail());
        assert.deepEqual(calls[0].options, {templateCode: 't'});
        calls[0].onEvent({type: 'alreadyAgreed'});
        assert.deepEqual(events, [{type: 'alreadyAgreed'}]);
        stop(); stop();
        calls[0].onEvent({type: 'newAgreement'});
        assert.equal(events.length, 1);
        assert.equal(removed, 1);

        error = undefined;
        createBridge(sdk).stream('notificationAgreement', '{"templateCode":"t"}', () => assert.fail(), e => { error = JSON.parse(e); });
        calls[1].onError(Object.assign(Error('private'), {code: 'X'}));
        assert.deepEqual(error, {code: 'SDK_FAILURE'});
        assert.equal(removed, 2);
      },
    },
  },
};
