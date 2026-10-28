const memory = new Map();
// Prefix keys so the mock never collides with app-owned localStorage entries.
const local = (() => { try { return globalThis.localStorage ?? null; } catch { return null; } })();
const key = k => `ait-mock-storage:${k}`;

const storage = {
  getItem: async k => local ? local.getItem(key(k)) : memory.get(k) ?? null,
  setItem: async (k, v) => { if (local) local.setItem(key(k), v); else memory.set(k, v); },
  removeItem: async k => { if (local) local.removeItem(key(k)); else memory.delete(k); },
  clearItems: async () => {
    memory.clear();
    if (local) for (const k of Object.keys(local).filter(k => k.startsWith(key('')))) local.removeItem(k);
  },
  getItems: Object.assign(async keys => Object.fromEntries(await Promise.all(keys.map(async k => [k, await storage.getItem(k)]))), {isSupported: () => true}),
  setItems: Object.assign(async items => { for (const [k, v] of Object.entries(items)) await storage.setItem(k, v); }, {isSupported: () => true}),
};

export default {Storage: storage};
