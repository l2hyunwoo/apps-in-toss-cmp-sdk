// 1x1 transparent PNG.
const PIXEL = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNkYAAAAAYAAjCB0C8AAAAASUVORK5CYII=';
const image = (id, base64) => ({id, dataUri: base64 ? PIXEL : `data:image/png;base64,${PIXEL}`});
const gated = fn => Object.assign(fn, {getPermission: async () => 'allowed', openPermissionDialog: async () => 'allowed'});

export default {
  Device: {
    getPhotos: gated(async ({maxCount = 10, base64 = false} = {}) => Array.from({length: Math.min(maxCount, 3)}, (_, i) => image(`mock-photo-${i}`, base64))),
    getAlbumItems: Object.assign(async ({maxCount = 10, base64 = false} = {}) => Array.from({length: Math.min(maxCount, 2)}, (_, i) => ({...image(`mock-album-${i}`, base64), type: 'PHOTO'})), {isSupported: () => true}),
    openCamera: gated(async ({base64 = false} = {}) => { console.info('[CMP] Mock camera returns a fixed image.'); return image('mock-camera', base64); }),
  },
};
