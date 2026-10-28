const empty = origin => ({origin, localStorage: {}, indexedDB: [], opfs: {directories: [], files: []}, errors: []});

export default {
  Migration: {
    getOriginStorage: async () => {
      console.info('[CMP] Mock origin storage is empty.');
      return {previous: empty('https://web.tossmini.com'), current: empty('https://apps.tossmini.com')};
    },
  },
};
