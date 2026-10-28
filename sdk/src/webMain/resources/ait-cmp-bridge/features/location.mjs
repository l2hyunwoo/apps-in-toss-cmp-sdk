import { guarded, isPermissionError } from './permissions.mjs';

function accuracy(value) {
  // 공식 Accuracy enum 값: Lowest = 1 ... BestForNavigation = 6.
  if (!Number.isInteger(value) || value < 1 || value > 6) throw new TypeError('Unknown accuracy');
  return value;
}
function interval(value, name) {
  if (typeof value !== 'number' || !Number.isFinite(value) || value < 0) throw new TypeError(`${name} must be a non-negative number`);
  return value;
}
const gated = fn => typeof fn === 'function' && typeof fn.getPermission === 'function';

export default ({sdk, requireCapability}) => ({
  capabilities: {
    LOCATION: () => gated(sdk.Device?.getLocation),
    LOCATION_UPDATES: () => gated(sdk.Device?.subscribeLocation),
  },
  calls: {
    locationGet: payload => {
      const options = {accuracy: accuracy(payload.accuracy)};
      return () => { requireCapability('LOCATION'); return guarded(sdk, sdk.Device.getLocation, () => sdk.Device.getLocation(options)); };
    },
  },
  streams: {
    locationSubscribe: payload => {
      const options = {accuracy: accuracy(payload.accuracy), timeInterval: interval(payload.timeInterval, 'timeInterval'), distanceInterval: interval(payload.distanceInterval, 'distanceInterval')};
      return ({emit, fail}) => {
        requireCapability('LOCATION_UPDATES');
        const subscribe = sdk.Device.subscribeLocation;
        let stopped = false;
        let release = null;
        Promise.resolve(subscribe.getPermission()).then(status => {
          if (stopped) return;
          if (status === 'denied') { emit({permission: 'denied'}); return; }
          release = sdk.Device.subscribeLocation({
            options,
            onEvent: value => emit({permission: 'allowed', value}),
            onError: error => { if (isPermissionError(sdk, error)) emit({permission: 'denied'}); else fail(error); },
          });
        }).catch(fail);
        return () => { stopped = true; const cleanup = release; release = null; cleanup?.(); };
      };
    },
  },
});
