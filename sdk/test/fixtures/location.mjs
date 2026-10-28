import assert from 'node:assert/strict';

const gated = (fn, status = 'allowed') => Object.assign(fn, {getPermission: async () => status, openPermissionDialog: async () => 'allowed'});
const here = {timestamp: 1, coords: {latitude: 37.5, longitude: 127, altitude: 0, accuracy: 5, altitudeAccuracy: 0, heading: 0}};
const location = settle => ({Device: {getLocation: gated(async () => settle(here))}});
const options = {accuracy: 3, timeInterval: 1000, distanceInterval: 10};

export default {
  calls: {
    locationGet: {payload: {accuracy: 3}, invalid: [{accuracy: 0}, {accuracy: 7}, {accuracy: '3'}, {}], capability: 'LOCATION', sdk: location, expect: {permission: 'allowed', value: here}},
  },
  streams: {
    locationSubscribe: {
      payload: options,
      run: async ({createBridge, base, tick}) => {
        for (const invalid of [{...options, accuracy: 9}, {...options, timeInterval: -1}, {accuracy: 3, timeInterval: 1000}]) {
          assert.throws(() => createBridge(base()).stream('locationSubscribe', JSON.stringify(invalid), () => {}, () => {}), TypeError);
        }
        let error;
        createBridge(base()).stream('locationSubscribe', JSON.stringify(options), () => assert.fail(), e => { error = JSON.parse(e); });
        assert.equal(error.code, 'UNSUPPORTED');

        class PermissionError extends Error {}
        let listener; let received; let removed = 0;
        const subscribe = status => ({...base(), PermissionError, Device: {subscribeLocation: gated(params => { listener = params; return () => removed++; }, status)}});
        const events = [];
        const stop = createBridge(subscribe('allowed')).stream('locationSubscribe', JSON.stringify(options), e => events.push(JSON.parse(e)), () => assert.fail());
        await tick();
        received = listener.options;
        assert.deepEqual(received, options);
        listener.onEvent(here);
        listener.onError(new PermissionError());
        assert.deepEqual(events, [{permission: 'allowed', value: here}, {permission: 'denied'}]);
        stop(); stop();
        listener.onEvent(here);
        assert.equal(events.length, 2);
        assert.equal(removed, 1);

        listener = null;
        const denied = [];
        createBridge(subscribe('denied')).stream('locationSubscribe', JSON.stringify(options), e => denied.push(JSON.parse(e)), () => assert.fail());
        await tick();
        assert.deepEqual(denied, [{permission: 'denied'}]);
        assert.equal(listener, null, 'subscribeLocation must not run when permission is denied');

        const early = createBridge(subscribe('allowed')).stream('locationSubscribe', JSON.stringify(options), () => assert.fail(), () => assert.fail());
        early();
        await tick();
        assert.equal(listener, null, 'stopping before the permission check resolves must not subscribe');

        createBridge(subscribe('allowed')).stream('locationSubscribe', JSON.stringify(options), () => {}, e => { error = JSON.parse(e); });
        await tick();
        listener.onError(Error('private'));
        assert.deepEqual(error, {code: 'SDK_FAILURE'});
        assert.equal(removed, 2);
      },
    },
  },
};
