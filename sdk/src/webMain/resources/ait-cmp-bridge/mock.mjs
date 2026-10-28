import { createBridge } from './core.mjs';
const sdk = {
  Environment: {environment:'sandbox'},
  User: {getAnonymousKey: Object.assign(async () => ({type:'HASH',hash:'mock-local-user'}), {isSupported:()=>true})},
  TossAuth: {login: async () => ({authorizationCode:'mock-not-for-server', referrer:'SANDBOX'})},
  Share: {sendMessage: async () => {}},
  Screen: {close: async () => {}},
  SafeArea: {get: () => ({top:0,left:0,right:0,bottom:0}), subscribe: () => () => {}},
  graniteEvent: {addEventListener: () => () => {}},
  generateHapticFeedback: async () => {},
  Storage: mockStorage(),
  Game: {
    getUserProfile: Object.assign(async () => ({statusCode:'SUCCESS', nickname:'모의 사용자', profileImageUri:''}), {isSupported:()=>true}),
    setLeaderboardScore: Object.assign(async () => ({statusCode:'SUCCESS'}), {isSupported:()=>true}),
    openLeaderboard: Object.assign(async () => { console.info('[CMP] Mock leaderboard opened.'); }, {isSupported:()=>true})
  }
};
function mockStorage() {
  const memory = new Map();
  // Prefix keys so the mock never collides with app-owned localStorage entries.
  const local = (() => { try { return globalThis.localStorage ?? null; } catch { return null; } })();
  const key = k => `ait-mock-storage:${k}`;
  return {
    getItem: async k => local ? local.getItem(key(k)) : memory.get(k) ?? null,
    setItem: async (k, v) => { if (local) local.setItem(key(k), v); else memory.set(k, v); },
    removeItem: async k => { if (local) local.removeItem(key(k)); else memory.delete(k); }
  };
}
export const bridge = createBridge(sdk, {mock:true});
globalThis.aitCmpBridge = bridge;
console.info('[CMP] Explicit mock bridge. This is not a Toss host test.');
