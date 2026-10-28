import assert from 'node:assert/strict';

export default {
  calls: {
    reply: {
      payload: {replyId: 'unknown', value: true},
      invalid: [{value: true}],
      run: async ({createBridge, base, callBridge}) => {
        assert.equal(await callBridge(createBridge(base()), 'reply', {replyId: 'unknown', value: true}), null);
      },
    },
  },
  streams: {
    hostEvents: {
      payload: {},
      run: async ({createBridge, base}) => {
        const removed = []; const listeners = {};
        const sdk = {...base(),
          SafeArea: {get: () => ({top: 10, left: 0, right: 0, bottom: 20}), subscribe: ({onEvent}) => { listeners.safe = onEvent; return () => removed.push('safe'); }},
          graniteEvent: {addEventListener: (name, {onEvent}) => { listeners[name] = onEvent; return () => removed.push(name); }}};
        const b = createBridge(sdk);
        assert.equal(b.supports('HOST_EVENTS'), true);
        const events = [];
        const stop = b.stream('hostEvents', '{}', e => events.push(JSON.parse(e)), () => assert.fail());
        assert.equal(events[0].top, 10);
        listeners.backEvent(); assert.equal(events[1].type, 'back');
        stop(); stop(); listeners.backEvent();
        assert.equal(events.length, 2);
        assert.deepEqual(removed, ['backEvent', 'homeEvent', 'safe']);

        const partial = {...sdk, SafeArea: {...sdk.SafeArea, subscribe: () => { throw Error('private'); }}};
        removed.length = 0;
        let error;
        createBridge(partial).stream('hostEvents', '{}', () => assert.fail(), e => { error = JSON.parse(e); });
        assert.deepEqual(error, {code: 'SDK_FAILURE'});
        assert.deepEqual(removed, ['backEvent', 'homeEvent']);

        createBridge(base()).stream('hostEvents', '{}', () => assert.fail(), e => { error = JSON.parse(e); });
        assert.equal(error.code, 'UNSUPPORTED');
      },
    },
  },
};
