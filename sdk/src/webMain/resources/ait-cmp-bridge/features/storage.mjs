function requireKey(key) {
  if (typeof key !== 'string' || !key.trim()) throw new TypeError('Storage key is required');
}

export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    STORAGE: () => ['getItem', 'setItem', 'removeItem', 'clearItems'].every(name => typeof sdk.Storage?.[name] === 'function'),
    STORAGE_BATCH: () => available(sdk.Storage?.getItems) && available(sdk.Storage?.setItems),
  },
  calls: {
    storageGet: ({key}) => { requireKey(key); return () => { requireCapability('STORAGE'); return sdk.Storage.getItem(key); }; },
    storageSet: ({key, value}) => {
      requireKey(key);
      if (typeof value !== 'string') throw new TypeError('Storage value must be a string');
      return () => { requireCapability('STORAGE'); return sdk.Storage.setItem(key, value); };
    },
    storageRemove: ({key}) => { requireKey(key); return () => { requireCapability('STORAGE'); return sdk.Storage.removeItem(key); }; },
    storageClear: () => () => { requireCapability('STORAGE'); return sdk.Storage.clearItems(); },
    storageGetItems: ({keys}) => {
      if (!Array.isArray(keys)) throw new TypeError('Storage keys must be an array');
      keys.forEach(requireKey);
      return () => { requireCapability('STORAGE_BATCH'); return sdk.Storage.getItems(keys); };
    },
    storageSetItems: ({items}) => {
      if (typeof items !== 'object' || items === null || Array.isArray(items)) throw new TypeError('Storage items must be an object');
      for (const [key, value] of Object.entries(items)) {
        requireKey(key);
        if (typeof value !== 'string') throw new TypeError('Storage value must be a string');
      }
      return () => { requireCapability('STORAGE_BATCH'); return sdk.Storage.setItems(items); };
    },
  },
});
