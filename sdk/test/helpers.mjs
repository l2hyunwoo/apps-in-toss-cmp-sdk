export const BRIDGE = '../src/webMain/resources/ait-cmp-bridge';
export const base = () => ({Environment: {environment: 'sandbox'}});
export const callBridge = (b, method, payload) => new Promise((resolve, reject) => b.call(method, JSON.stringify(payload), s => resolve(JSON.parse(s)), e => reject(JSON.parse(e))));
export const tick = () => new Promise(setImmediate);
