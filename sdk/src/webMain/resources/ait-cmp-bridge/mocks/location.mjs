const gated = fn => Object.assign(fn, {getPermission: async () => 'allowed', openPermissionDialog: async () => 'allowed'});
// Seoul City Hall; a fixed point keeps the mock deterministic.
const location = () => ({timestamp: Date.now(), coords: {latitude: 37.5663, longitude: 126.9779, altitude: 0, accuracy: 10, altitudeAccuracy: 0, heading: 0}});

export default {
  Device: {
    getLocation: gated(async () => location()),
    subscribeLocation: gated(({onEvent}) => {
      console.info('[CMP] Mock location updates emit one fixed location.');
      const timer = setTimeout(() => onEvent(location()), 0);
      return () => clearTimeout(timer);
    }),
  },
};
