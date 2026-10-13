import * as sdk from '@apps-in-toss/web-framework';
import { createBridge } from './core.mjs';
export const bridge = createBridge(sdk);
globalThis.aitCmpBridge = bridge;
