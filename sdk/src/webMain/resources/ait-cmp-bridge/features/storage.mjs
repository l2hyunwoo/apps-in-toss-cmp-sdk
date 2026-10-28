function requireKey(key) {
  if (typeof key !== 'string' || !key.trim()) throw new TypeError('Storage key is required');
}

export default ({sdk, requireCapability}) => ({
  capabilities: {
    STORAGE: () => ['getItem', 'setItem', 'removeItem'].every(name => typeof sdk.Storage?.[name] === 'function'),
  },
  calls: {
    storageGet: ({key}) => { requireKey(key); return () => { requireCapability('STORAGE'); return sdk.Storage.getItem(key); }; },
    storageSet: ({key, value}) => {
      requireKey(key);
      if (typeof value !== 'string') throw new TypeError('Storage value must be a string');
      return () => { requireCapability('STORAGE'); return sdk.Storage.setItem(key, value); };
    },
    storageRemove: ({key}) => { requireKey(key); return () => { requireCapability('STORAGE'); return sdk.Storage.removeItem(key); }; },
  },
});
