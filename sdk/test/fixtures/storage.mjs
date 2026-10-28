const batch = fn => Object.assign(fn, {isSupported: () => true});
const storage = settle => ({Storage: {
  getItem: async () => settle('v'), setItem: async () => settle(undefined), removeItem: async () => settle(undefined), clearItems: async () => settle(undefined),
  getItems: batch(async keys => settle(Object.fromEntries(keys.map(k => [k, null])))), setItems: batch(async () => settle(undefined)),
}});

export default {
  calls: {
    storageGet: {payload: {key: 'k'}, invalid: [{key: ' '}, {}], capability: 'STORAGE', sdk: storage, expect: 'v'},
    storageSet: {payload: {key: 'k', value: 'v'}, invalid: [{key: ' ', value: 'v'}, {key: 'k', value: 1}], capability: 'STORAGE', sdk: storage, expect: null},
    storageRemove: {payload: {key: 'k'}, invalid: [{key: ''}], capability: 'STORAGE', sdk: storage, expect: null},
    storageClear: {payload: {}, capability: 'STORAGE', sdk: storage, expect: null},
    storageGetItems: {payload: {keys: ['a', 'b']}, invalid: [{}, {keys: 'a'}, {keys: ['a', ' ']}], capability: 'STORAGE_BATCH', sdk: storage, expect: {a: null, b: null}},
    storageSetItems: {payload: {items: {a: '1'}}, invalid: [{}, {items: ['1']}, {items: {a: 1}}, {items: {' ': '1'}}], capability: 'STORAGE_BATCH', sdk: storage, expect: null},
  },
  streams: {},
  absentInMin: ['STORAGE_BATCH'],
};
