import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createBridge} from '../src/webMain/resources/ait-cmp-bridge/core.mjs';
import {callBridge} from './helpers.mjs';

test('일반 브라우저에서는 사용할 수 없고 몰래 mock으로 바뀌지 않는다', async () => {
  const b = createBridge({Environment: {get environment() { throw Error('no host'); }}});
  assert.equal(b.host(), 'browser');
  await assert.rejects(callBridge(b, 'login', {}), {code: 'HOST_UNAVAILABLE'});
});

test('중복 feature 항목을 거부한다', async () => {
  const {FEATURES} = await import('../src/webMain/resources/ait-cmp-bridge/features/index.mjs');
  FEATURES.push(() => ({calls: {login: () => () => null}}));
  try {
    assert.throws(() => createBridge({}), /Duplicate bridge entry: login/);
  } finally {
    FEATURES.pop();
  }
});

test('최소 SDK는 없는 API를 unsupported로 보고한다', async () => {
  const {FIXTURES} = await import('./fixtures/index.mjs');
  const min = await import('toss-sdk-min');
  const b = createBridge({...min, Environment: {environment: 'sandbox'}});
  // fixture는 3.0.1에 공식 API가 없는 capability를 `absentInMin`에 나열한다.
  for (const capability of FIXTURES.flatMap(f => f.absentInMin ?? [])) {
    assert.equal(b.supports(capability), false, capability);
  }
});

for (const name of ['toss-sdk-min', '@apps-in-toss/web-framework']) {
  test(`공식 SDK 공개 API 형태: ${name}`, async () => {
    const sdk = await import(name);
    for (const [namespace, method] of [['TossAuth','login'],['User','getAnonymousKey'],['Share','sendMessage'],['Screen','close'],['SafeArea','get'],['SafeArea','subscribe'],['graniteEvent','addEventListener'],['Storage','getItem'],['Storage','setItem'],['Storage','removeItem'],['Game','getUserProfile'],['Game','setLeaderboardScore'],['Game','openLeaderboard'],['Device','triggerHaptic']]) {
      assert.equal(typeof sdk[namespace]?.[method], 'function', `${namespace}.${method}`);
    }
    for (const method of ['getUserProfile','setLeaderboardScore','openLeaderboard']) assert.equal(typeof sdk.Game[method].isSupported, 'function', method);
    assert.equal(typeof sdk.User.getAnonymousKey.isSupported, 'function');
  });
}

test('번들된 SDK 리소스는 host를 초기화하고 production에 mock을 넣지 않는다', async () => {
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
