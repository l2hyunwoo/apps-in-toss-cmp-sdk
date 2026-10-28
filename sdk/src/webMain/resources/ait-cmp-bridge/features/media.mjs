import { guarded } from './permissions.mjs';

const TYPES = ['PHOTO', 'VIDEO'];

function positive(value, name) {
  if (!Number.isInteger(value) || value <= 0) throw new TypeError(`${name} must be a positive integer`);
  return value;
}
function flag(value) {
  if (typeof value !== 'boolean') throw new TypeError('base64 must be a boolean');
  return value;
}
const gated = fn => typeof fn === 'function' && typeof fn.getPermission === 'function';

export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    PHOTOS: () => gated(sdk.Device?.getPhotos),
    ALBUM: () => available(sdk.Device?.getAlbumItems),
    CAMERA: () => gated(sdk.Device?.openCamera),
  },
  calls: {
    photosGet: ({maxCount, maxWidth, base64}) => {
      const options = {maxCount: positive(maxCount, 'maxCount'), maxWidth: positive(maxWidth, 'maxWidth'), base64: flag(base64)};
      return () => { requireCapability('PHOTOS'); return guarded(sdk, sdk.Device.getPhotos, () => sdk.Device.getPhotos(options)); };
    },
    albumItemsGet: ({types, maxCount, maxWidth, base64}) => {
      if (!Array.isArray(types) || !types.length || !types.every(t => TYPES.includes(t))) throw new TypeError('Unknown album item type');
      const options = {types, maxCount: positive(maxCount, 'maxCount'), maxWidth: positive(maxWidth, 'maxWidth'), base64: flag(base64)};
      return () => {
        requireCapability('ALBUM');
        // getAlbumItems has no getPermission member; it reports denial with the NOT_ALLOWED code.
        return Promise.resolve(sdk.Device.getAlbumItems(options)).then(value => ({permission: 'allowed', value}), error => {
          if (error?.code === 'NOT_ALLOWED') return {permission: 'denied'};
          throw error;
        });
      };
    },
    cameraOpen: ({maxWidth, base64}) => {
      const options = {maxWidth: positive(maxWidth, 'maxWidth'), base64: flag(base64)};
      return () => { requireCapability('CAMERA'); return guarded(sdk, sdk.Device.openCamera, () => sdk.Device.openCamera(options)); };
    },
  },
});
