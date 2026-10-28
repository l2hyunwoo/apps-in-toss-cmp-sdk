import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createBridge} from '../src/webMain/resources/ait-cmp-bridge/core.mjs';
import {FIXTURES} from './fixtures/index.mjs';
import {base, callBridge, tick} from './helpers.mjs';

const fixtures = {calls: {}, streams: {}};
for (const part of FIXTURES) for (const kind of ['calls', 'streams']) Object.assign(fixtures[kind], part[kind]);
const {methods} = createBridge(base());
const ctx = {createBridge, base, callBridge, tick, assert};

test('모든 bridge 메서드에 fixture가 있다', () => {
  for (const kind of ['calls', 'streams']) {
    const missing = methods[kind].filter(name => !fixtures[kind][name]);
    assert.deepEqual(missing, [], `${kind} without fixture`);
    assert.deepEqual(Object.keys(fixtures[kind]).filter(name => !methods[kind].includes(name)), [], `${kind} fixtures without bridge method`);
  }
});

for (const [method, f] of Object.entries(fixtures.calls)) {
  test(`${method} 호출`, async () => {
    for (const payload of f.invalid ?? []) {
      assert.throws(() => createBridge({...base(), ...f.sdk?.(v => v)}).call(method, JSON.stringify(payload), () => {}, () => {}), TypeError, JSON.stringify(payload));
    }
    if (f.run) { await f.run(ctx); return; }

    const missing = createBridge(base());
    assert.equal(missing.supports(f.capability), false);
    await assert.rejects(callBridge(missing, method, f.payload), {code: 'UNSUPPORTED'});

    const ok = createBridge({...base(), ...f.sdk(v => v)});
    assert.equal(ok.supports(f.capability), true);
    const result = await callBridge(ok, method, f.payload);
    if (typeof f.expect === 'function') f.expect(result); else assert.deepEqual(result, f.expect ?? null);

    let release;
    const late = createBridge({...base(), ...f.sdk(v => new Promise(resolve => { release = () => resolve(v); }))});
    const stop = late.call(method, JSON.stringify(f.payload), () => assert.fail('late success'), () => assert.fail('late failure'));
    stop();
    release?.();
    await tick();

    const browser = createBridge({Environment: {environment: 'web'}, ...f.sdk(v => v)});
    assert.equal(browser.supports(f.capability), false);
    await assert.rejects(callBridge(browser, method, f.payload), {code: 'HOST_UNAVAILABLE'});
  });
}

for (const [method, f] of Object.entries(fixtures.streams)) {
  test(`${method} stream`, async () => {
    let error;
    createBridge({Environment: {environment: 'web'}}).stream(method, JSON.stringify(f.payload ?? {}), () => assert.fail(), e => { error = JSON.parse(e); });
    assert.equal(error.code, 'HOST_UNAVAILABLE');
    await f.run(ctx);
  });
}

test('알 수 없는 메서드 이름과 상속된 메서드 이름을 거부한다', () => {
  const b = createBridge(base());
  for (const name of ['nope', 'constructor', '__proto__']) {
    assert.throws(() => b.call(name, '{}', () => {}, () => {}), TypeError);
    assert.throws(() => b.stream(name, '{}', () => {}, () => {}), TypeError);
  }
});

test('stream host 질문은 stop 시 fallback으로 끝나고 reply 시 resolve된다', async () => {
  const b = createBridge(base());
  // 일회용 stream 항목으로 core ask/reply를 실행한다.
  const answers = [];
  const probe = {streams: {}};
  probe.streams.probe = () => ({ask}) => { ask({type: 'q1'}, false).then(v => answers.push(v)); ask({type: 'q2'}, false).then(v => answers.push(v)); return () => {}; };
  const {FEATURES} = await import('../src/webMain/resources/ait-cmp-bridge/features/index.mjs');
  FEATURES.push(() => probe);
  try {
    const bridge = createBridge(base());
    const asked = [];
    const stop = bridge.stream('probe', '{}', e => asked.push(JSON.parse(e)), () => assert.fail());
    assert.deepEqual(asked.map(e => e.type), ['q1', 'q2']);
    await callBridge(bridge, 'reply', {replyId: asked[0].replyId, value: true});
    await tick();
    stop();
    await tick();
    assert.deepEqual(answers, [true, false]);
    await callBridge(bridge, 'reply', {replyId: asked[1].replyId, value: true});
    assert.deepEqual(answers, [true, false]);
  } finally {
    FEATURES.pop();
  }
  assert.equal(b.methods.streams.includes('probe'), false);
});
