import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readdir, readFile} from 'node:fs/promises';
import {createBridge} from '../src/webMain/resources/ait-cmp-bridge/core.mjs';
import {base} from './helpers.mjs';

const dir = new URL('./coverage/', import.meta.url);
const TOP_LEVEL = new Set(['host', 'supports', 'call', 'stream']);

/** Lists official exports without invoking getters (they read `window`). */
export function officialNames(sdk) {
  const names = [];
  for (const [key, descriptor] of Object.entries(Object.getOwnPropertyDescriptors(sdk))) {
    const value = descriptor.value;
    const members = value && typeof value === 'object'
      ? Object.entries(Object.getOwnPropertyDescriptors(value)).filter(([, d]) => d.get || typeof d.value === 'function')
      : [];
    if (members.length) names.push(...members.map(([member]) => `${key}.${member}`));
    else names.push(key);
  }
  return names;
}

export function checkCoverage(manifest, names, bridgeMethods) {
  const problems = [];
  for (const name of names) if (!manifest[name]) problems.push(`unclassified: ${name}`);
  for (const [name, entry] of Object.entries(manifest)) {
    if (!names.includes(name)) problems.push(`not exported: ${name}`);
    const kinds = ['bridge', 'replaced', 'excluded', 'planned'].filter(k => k in entry);
    if (kinds.length !== 1) problems.push(`needs exactly one classification: ${name}`);
    if (entry.bridge && !bridgeMethods.includes(entry.bridge) && !TOP_LEVEL.has(entry.bridge)) problems.push(`missing bridge method ${entry.bridge}: ${name}`);
    if (entry.bridge && !entry.capability) problems.push(`missing capability: ${name}`);
    if (entry.replaced && !manifest[entry.replaced]) problems.push(`replacement not classified: ${name} -> ${entry.replaced}`);
  }
  return problems;
}

async function loadManifest() {
  const manifest = {};
  for (const file of (await readdir(dir)).filter(f => f.endsWith('.json')).sort()) {
    for (const [name, entry] of Object.entries(JSON.parse(await readFile(new URL(file, dir), 'utf8')))) {
      assert.ok(!manifest[name], `${name} classified twice (${file})`);
      manifest[name] = entry;
    }
  }
  return manifest;
}

const bridge = createBridge(base());
const bridgeMethods = [...bridge.methods.calls, ...bridge.methods.streams];

test('every official 3.9.0 export is classified', async () => {
  const manifest = await loadManifest();
  const names = officialNames(await import('@apps-in-toss/web-framework'));
  assert.deepEqual(checkCoverage(manifest, names, bridgeMethods), []);
  const planned = Object.entries(manifest).filter(([, e]) => e.planned).map(([n, e]) => `${e.planned} ${n}`);
  if (planned.length) console.info(`[coverage] ${planned.length} planned entries remain`);
});

test('coverage check reports a removed entry', async () => {
  const manifest = await loadManifest();
  const names = officialNames(await import('@apps-in-toss/web-framework'));
  const [first] = Object.keys(manifest).filter(n => manifest[n].bridge);
  const copy = {...manifest};
  delete copy[first];
  assert.ok(checkCoverage(copy, names, bridgeMethods).includes(`unclassified: ${first}`));
  assert.ok(checkCoverage({...manifest, [first]: {bridge: 'nope', capability: 'X'}}, names, bridgeMethods).some(p => p.startsWith('missing bridge method')));
});

test('every bridge method is referenced by the manifest', async () => {
  const manifest = await loadManifest();
  const used = new Set(Object.values(manifest).map(e => e.bridge).filter(Boolean));
  assert.deepEqual(bridgeMethods.filter(m => m !== 'reply' && !used.has(m)), []);
});
