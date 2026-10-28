import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createBridge} from '../src/webMain/resources/ait-cmp-bridge/core.mjs';
import {callBridge} from './helpers.mjs';

test('plain browser is unavailable, never silently mocked', async () => {
  const b = createBridge({Environment: {get environment() { throw Error('no host'); }}});
  assert.equal(b.host(), 'browser');
  await assert.rejects(callBridge(b, 'login', {}), {code: 'HOST_UNAVAILABLE'});
});

test('duplicate feature entries are rejected', async () => {
  const {FEATURES} = await import('../src/webMain/resources/ait-cmp-bridge/features/index.mjs');
  FEATURES.push(() => ({calls: {login: () => () => null}}));
  try {
    assert.throws(() => createBridge({}), /Duplicate bridge entry: login/);
  } finally {
    FEATURES.pop();
  }
});

test('minimum SDK reports APIs it does not have as unsupported', async () => {
  const {FIXTURES} = await import('./fixtures/index.mjs');
  const min = await import('toss-sdk-min');
  const b = createBridge({...min, Environment: {environment: 'sandbox'}});
  // Fixtures list capabilities whose official API is missing in 3.0.1 as `absentInMin`.
  for (const capability of FIXTURES.flatMap(f => f.absentInMin ?? [])) {
    assert.equal(b.supports(capability), false, capability);
  }
});

for (const name of ['toss-sdk-min', '@apps-in-toss/web-framework']) {
  test(`official SDK public API shape: ${name}`, async () => {
    const sdk = await import(name);
    for (const [namespace, method] of [['TossAuth','login'],['User','getAnonymousKey'],['Share','sendMessage'],['Screen','close'],['SafeArea','get'],['SafeArea','subscribe'],['graniteEvent','addEventListener'],['Storage','getItem'],['Storage','setItem'],['Storage','removeItem'],['Game','getUserProfile'],['Game','setLeaderboardScore'],['Game','openLeaderboard'],['Device','triggerHaptic']]) {
      assert.equal(typeof sdk[namespace]?.[method], 'function', `${namespace}.${method}`);
    }
    for (const method of ['getUserProfile','setLeaderboardScore','openLeaderboard']) assert.equal(typeof sdk.Game[method].isSupported, 'function', method);
    assert.equal(typeof sdk.User.getAnonymousKey.isSupported, 'function');
  });
}

test('bundled SDK resources initialize their host and keep mock out of production', async () => {
  const {build} = await import('esbuild');
  const {runInNewContext} = await import('node:vm');
  for (const entry of ['mock', 'index']) {
    const bundled = await build({entryPoints: [`sdk/src/webMain/resources/ait-cmp-bridge/${entry}.mjs`], bundle: true, write: false, format: 'iife', platform: 'browser', metafile: true, define: {'process.env.NODE_ENV': '"production"'}});
    const context = {console: {info() {}}, setTimeout, clearTimeout, URL, TextEncoder, TextDecoder};
    runInNewContext(bundled.outputFiles[0].text, context);
    assert.equal(context.aitCmpBridge.host(), entry === 'mock' ? 'mock' : 'browser');
    if (entry === 'index') assert.ok(Object.keys(bundled.metafile.inputs).every(p => !/\/mocks?[/.]/.test(p)));
    else {
      let result;
      context.aitCmpBridge.call('login', '{}', s => { result = JSON.parse(s); }, () => assert.fail());
      await new Promise(setImmediate);
      assert.equal(result.authorizationCode, 'mock-not-for-server');
    }
  }
});
