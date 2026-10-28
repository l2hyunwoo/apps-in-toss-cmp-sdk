const storage = settle => ({Storage: {getItem: async () => settle('v'), setItem: async () => settle(undefined), removeItem: async () => settle(undefined)}});

export default {
  calls: {
    storageGet: {payload: {key: 'k'}, invalid: [{key: ' '}, {}], capability: 'STORAGE', sdk: storage, expect: 'v'},
    storageSet: {payload: {key: 'k', value: 'v'}, invalid: [{key: ' ', value: 'v'}, {key: 'k', value: 1}], capability: 'STORAGE', sdk: storage, expect: null},
    storageRemove: {payload: {key: 'k'}, invalid: [{key: ''}], capability: 'STORAGE', sdk: storage, expect: null},
  },
  streams: {},
};
