const dump = origin => ({
  origin,
  localStorage: {a: '1', b: null},
  indexedDB: [{name: 'db', version: 2, objectStores: [{
    name: 's', keyPath: ['a', 'b'], autoIncrement: false,
    indexes: [{name: 'i', keyPath: 'x', multiEntry: false, unique: true}],
    records: [{key: 1, primaryKey: 1, value: {n: [1, 'x', null]}}, {key: 2, primaryKey: 2, value: new Map()}],
  }]}],
  opfs: {directories: ['d'], files: [{path: 'd/f.txt', type: 'text/plain', lastModified: 5, data: new TextEncoder().encode('hi').buffer}]},
  errors: [{storage: 'opfs', message: 'm'}],
});
const migration = settle => ({Migration: {getOriginStorage: async () => settle({previous: dump('p'), current: dump('c')})}});
const expected = origin => ({
  origin,
  localStorage: {a: '1', b: null},
  indexedDB: [{name: 'db', version: 2, objectStores: [{
    name: 's', keyPath: ['a', 'b'], autoIncrement: false,
    indexes: [{name: 'i', keyPath: 'x', multiEntry: false, unique: true}],
    records: [{key: 1, primaryKey: 1, value: {n: [1, 'x', null]}}],
  }]}],
  opfs: {directories: ['d'], files: [{path: 'd/f.txt', type: 'text/plain', lastModified: 5, data: 'aGk='}]},
  errors: [{storage: 'opfs', message: 'm'}, {storage: 'indexedDB', message: 'db/s: skipped 1 records that are not JSON values'}],
});

export default {
  calls: {
    migrationOriginStorage: {payload: {}, capability: 'ORIGIN_STORAGE_MIGRATION', sdk: migration, expect: {previous: expected('p'), current: expected('c')}},
  },
  streams: {},
  absentInMin: ['ORIGIN_STORAGE_MIGRATION'],
};
