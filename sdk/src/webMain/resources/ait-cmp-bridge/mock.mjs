import { createBridge } from './core.mjs';
import { MOCKS } from './mocks/index.mjs';

const sdk = {};
// Namespaces from different feature mocks are merged member by member.
for (const part of MOCKS) {
  for (const [name, value] of Object.entries(part)) {
    sdk[name] = typeof value === 'function' || typeof sdk[name] !== 'object' ? value : {...sdk[name], ...value};
  }
}
export const bridge = createBridge(sdk, {mock:true});
globalThis.aitCmpBridge = bridge;
console.info('[CMP] Explicit mock bridge. This is not a Toss host test.');
