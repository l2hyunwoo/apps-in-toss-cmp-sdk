// IndexedDB 키와 값은 structured-clone 데이터이고, 일반 JSON 값만 bridge를 넘을 수 있다.
function jsonSafe(value) {
  if (value === null || typeof value === 'string' || typeof value === 'boolean') return true;
  if (typeof value === 'number') return Number.isFinite(value);
  if (Array.isArray(value)) return value.every(jsonSafe);
  if (typeof value !== 'object') return false;
  const proto = Object.getPrototypeOf(value);
  return (proto === Object.prototype || proto === null) && Object.values(value).every(jsonSafe);
}

function base64(buffer) {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.length; i += 0x8000) binary += String.fromCharCode(...bytes.subarray(i, i + 0x8000));
  return btoa(binary);
}

// 건너뛴 레코드를 `errors`에 알려서, 마이그레이션 전에 오류를 확인하는 앱이 일부만 있는 저장소를 복사하지 않게 한다.
function dump({origin, localStorage, indexedDB, opfs, errors}) {
  const problems = errors.map(({storage, message}) => ({storage, message}));
  return {
    origin,
    localStorage,
    indexedDB: indexedDB.map(db => ({
      name: db.name,
      version: db.version,
      objectStores: db.objectStores.map(store => {
        const records = store.records.filter(r => jsonSafe(r.key) && jsonSafe(r.primaryKey) && jsonSafe(r.value));
        const skipped = store.records.length - records.length;
        if (skipped) problems.push({storage: 'indexedDB', message: `${db.name}/${store.name}: skipped ${skipped} records that are not JSON values`});
        return {...store, records: records.map(({key, primaryKey, value}) => ({key, primaryKey, value}))};
      }),
    })),
    opfs: {directories: opfs.directories, files: opfs.files.map(f => ({path: f.path, type: f.type, lastModified: f.lastModified, data: base64(f.data)}))},
    errors: problems,
  };
}

export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    ORIGIN_STORAGE_MIGRATION: () => available(sdk.Migration?.getOriginStorage),
  },
  calls: {
    migrationOriginStorage: () => () => {
      requireCapability('ORIGIN_STORAGE_MIGRATION');
      return Promise.resolve(sdk.Migration.getOriginStorage()).then(({previous, current}) => ({previous: dump(previous), current: dump(current)}));
    },
  },
});
