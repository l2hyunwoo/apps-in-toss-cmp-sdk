import {test} from 'node:test';
import assert from 'node:assert/strict';
import {FIXTURES} from './fixtures/index.mjs';
import {tick} from './helpers.mjs';

test('mock bridge answers every method with a result or UNSUPPORTED', async () => {
  const info = console.info; console.info = () => {};
  try {
    const {bridge} = await import('../src/webMain/resources/ait-cmp-bridge/mock.mjs');
    assert.equal(bridge.host(), 'mock');
    for (const part of FIXTURES) {
      for (const [method, f] of Object.entries(part.calls)) {
        if (method === 'reply') continue;
        const outcome = await new Promise(resolve => bridge.call(method, JSON.stringify(f.payload), () => resolve('ok'), e => resolve(JSON.parse(e).code)));
        assert.ok(['ok', 'UNSUPPORTED'].includes(outcome), `${method}: ${outcome}`);
      }
      for (const [method, f] of Object.entries(part.streams)) {
        let failure;
        const stop = bridge.stream(method, JSON.stringify(f.payload ?? {}), () => {}, e => { failure = JSON.parse(e).code; });
        await tick();
        stop();
        assert.ok(failure === undefined || failure === 'UNSUPPORTED', `${method}: ${failure}`);
      }
    }
  } finally {
    console.info = info;
  }
});
