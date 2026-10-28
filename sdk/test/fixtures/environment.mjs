// Environment stubs must keep `environment`, so these fixtures run the standard checks themselves.
const environment = (settle, extra = {}) => ({
  Environment: {
    environment: 'sandbox', deviceId: 'device', groupId: 'group', tossAppVersion: '5.270.0', deploymentId: 'local', initialURL: 'intoss://app',
    getNetworkStatus: async () => settle('WIFI'),
    getServerTime: Object.assign(async () => settle(1767225600000), {isSupported: () => true}),
    ...extra,
  },
  isMinVersionSupported: () => settle(true),
});

const check = (method, payload, capability, expect, sdk = environment) => async ({createBridge, base, callBridge, tick, assert}) => {
  const missing = createBridge(base());
  assert.equal(missing.supports(capability), false);
  await assert.rejects(callBridge(missing, method, payload), {code: 'UNSUPPORTED'});

  const ok = createBridge(sdk(v => v));
  assert.equal(ok.supports(capability), true);
  assert.deepEqual(await callBridge(ok, method, payload), expect);

  let release;
  const late = createBridge(sdk(v => new Promise(resolve => { release = () => resolve(v); })));
  late.call(method, JSON.stringify(payload), () => assert.fail('late success'), () => assert.fail('late failure'))();
  release?.();
  await tick();

  const browser = createBridge(sdk(v => v, {environment: 'web'}));
  assert.equal(browser.supports(capability), false);
  await assert.rejects(callBridge(browser, method, payload), {code: 'HOST_UNAVAILABLE'});
};

const deviceIdLater = (settle, extra) => environment(settle, {get deviceId() { return settle('device'); }, ...extra});

export default {
  calls: {
    environmentValue: {payload: {name: 'deviceId'}, invalid: [{}, {name: 'getServerTime'}], run: check('environmentValue', {name: 'deviceId'}, 'ENVIRONMENT_INFO', 'device', deviceIdLater)},
    isMinVersionSupported: {payload: {android: '5.0.0', ios: 'always'}, invalid: [{}, {android: '5.0', ios: 'never'}, {android: ['5.0.0'], ios: 'never'}], run: check('isMinVersionSupported', {android: '5.0.0', ios: 'always'}, 'ENVIRONMENT_INFO', true)},
    networkStatus: {payload: {}, run: check('networkStatus', {}, 'NETWORK_STATUS', 'WIFI')},
    serverTime: {payload: {}, run: check('serverTime', {}, 'SERVER_TIME', 1767225600000)},
  },
  streams: {},
};
