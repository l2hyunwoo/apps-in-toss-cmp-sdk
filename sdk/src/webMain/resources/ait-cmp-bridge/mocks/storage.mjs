const memory = new Map();
// Prefix keys so the mock never collides with app-owned localStorage entries.
const local = (() => { try { return globalThis.localStorage ?? null; } catch { return null; } })();
const key = k => `ait-mock-storage:${k}`;

export default {
  Storage: {
    getItem: async k => local ? local.getItem(key(k)) : memory.get(k) ?? null,
    setItem: async (k, v) => { if (local) local.setItem(key(k), v); else memory.set(k, v); },
    removeItem: async k => { if (local) local.removeItem(key(k)); else memory.delete(k); },
  },
};
