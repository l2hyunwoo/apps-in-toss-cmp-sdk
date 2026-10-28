export default {
  Environment: {
    deviceId: 'mock-device-id',
    groupId: 'mock-group-id',
    tossAppVersion: '5.270.0',
    deploymentId: 'local',
    initialURL: 'intoss://mock',
    getNetworkStatus: async () => 'WIFI',
    getServerTime: Object.assign(async () => 1767225600000, {isSupported: () => true}),
  },
  isMinVersionSupported: () => true,
};
