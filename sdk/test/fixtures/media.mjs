const gated = fn => Object.assign(fn, {getPermission: async () => 'allowed', openPermissionDialog: async () => 'allowed'});
const image = {id: 'p1', dataUri: 'aGk='};
const media = settle => ({Device: {
  getPhotos: gated(async () => settle([image])),
  getAlbumItems: Object.assign(async () => settle([{...image, type: 'VIDEO'}]), {isSupported: () => true}),
  openCamera: gated(async () => settle(image)),
}});

export default {
  calls: {
    photosGet: {payload: {maxCount: 1, maxWidth: 360, base64: true}, invalid: [{maxCount: 0, maxWidth: 360, base64: true}, {maxCount: 1, maxWidth: 1.5, base64: true}, {maxCount: 1, maxWidth: 360}], capability: 'PHOTOS', sdk: media, expect: {permission: 'allowed', value: [image]}},
    albumItemsGet: {payload: {types: ['PHOTO', 'VIDEO'], maxCount: 1, maxWidth: 360, base64: false}, invalid: [{types: [], maxCount: 1, maxWidth: 360, base64: false}, {types: ['AUDIO'], maxCount: 1, maxWidth: 360, base64: false}], capability: 'ALBUM', sdk: media, expect: {permission: 'allowed', value: [{...image, type: 'VIDEO'}]}},
    cameraOpen: {payload: {maxWidth: 1024, base64: false}, invalid: [{maxWidth: -1, base64: false}, {maxWidth: 1024, base64: 'no'}], capability: 'CAMERA', sdk: media, expect: {permission: 'allowed', value: image}},
  },
  streams: {},
};
