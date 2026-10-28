import { createBridge } from './core.mjs';
import { MOCKS } from './mocks/index.mjs';

const sdk = {};
// 여러 feature mock의 namespace를 멤버 단위로 병합한다.
for (const part of MOCKS) {
  for (const [name, value] of Object.entries(part)) {
    sdk[name] = typeof value === 'function' || typeof sdk[name] !== 'object' ? value : {...sdk[name], ...value};
  }
}
export const bridge = createBridge(sdk, {mock:true});
globalThis.aitCmpBridge = bridge;
console.info('[CMP] Explicit mock bridge. This is not a Toss host test.');
