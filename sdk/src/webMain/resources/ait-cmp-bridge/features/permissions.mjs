const ACCESS = {clipboard: ['read', 'write'], geolocation: ['access'], contacts: ['read', 'write'], photos: ['read', 'write'], camera: ['access'], microphone: ['access']};

function permission({name, access}) {
  if (!Object.hasOwn(ACCESS, name) || !ACCESS[name].includes(access)) throw new TypeError('Unknown permission');
  return {name, access};
}

export function isPermissionError(sdk, error) {
  return typeof sdk.PermissionError === 'function' && error instanceof sdk.PermissionError;
}

/** 거부는 결과다. 권한이 거부되면 `run` 전에 반환하므로 보호된 함수는 호출되지 않는다. */
export async function guarded(sdk, fn, run) {
  if (await fn.getPermission() === 'denied') return {permission: 'denied'};
  try {
    return {permission: 'allowed', value: await run()};
  } catch (error) {
    if (isPermissionError(sdk, error)) return {permission: 'denied'};
    throw error;
  }
}

export default ({sdk, requireCapability}) => ({
  capabilities: {
    PERMISSIONS: () => [sdk.getPermission, sdk.requestPermission, sdk.openPermissionDialog].every(fn => typeof fn === 'function'),
  },
  calls: {
    permissionGet: payload => { const p = permission(payload); return () => { requireCapability('PERMISSIONS'); return sdk.getPermission(p); }; },
    permissionRequest: payload => { const p = permission(payload); return () => { requireCapability('PERMISSIONS'); return sdk.requestPermission(p); }; },
    permissionDialog: payload => { const p = permission(payload); return () => { requireCapability('PERMISSIONS'); return sdk.openPermissionDialog(p); }; },
  },
});
