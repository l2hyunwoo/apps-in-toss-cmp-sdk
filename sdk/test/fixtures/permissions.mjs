const permissions = settle => ({getPermission: async () => settle('denied'), requestPermission: async () => settle('allowed'), openPermissionDialog: async () => settle('allowed')});
const payload = {name: 'photos', access: 'read'};
const invalid = [{name: 'photos', access: 'access'}, {name: 'location', access: 'access'}, {name: 'constructor', access: 'read'}, {}];

export default {
  calls: {
    permissionGet: {payload, invalid, capability: 'PERMISSIONS', sdk: permissions, expect: 'denied'},
    permissionRequest: {payload, invalid, capability: 'PERMISSIONS', sdk: permissions, expect: 'allowed'},
    permissionDialog: {payload: {name: 'camera', access: 'access'}, invalid, capability: 'PERMISSIONS', sdk: permissions, expect: 'allowed'},
  },
  streams: {},
};
