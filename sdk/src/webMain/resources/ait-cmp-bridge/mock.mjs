import { createBridge } from './core.mjs';
const sdk = {
  Environment: {environment:'sandbox'},
  User: {getAnonymousKey: Object.assign(async () => ({type:'HASH',hash:'mock-local-user'}), {isSupported:()=>true})},
  TossAuth: {login: async () => ({authorizationCode:'mock-not-for-server', referrer:'SANDBOX'})},
  Share: {sendMessage: async () => {}},
  Screen: {close: async () => {}},
  SafeArea: {get: () => ({top:0,left:0,right:0,bottom:0}), subscribe: () => () => {}},
  graniteEvent: {addEventListener: () => () => {}}
};
export const bridge = createBridge(sdk, {mock:true});
globalThis.aitCmpBridge = bridge;
console.info('[CMP] Explicit mock bridge. This is not a Toss host test.');
