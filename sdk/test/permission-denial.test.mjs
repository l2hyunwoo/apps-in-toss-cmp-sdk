import {test} from 'node:test';
import assert from 'node:assert/strict';
import {createBridge} from '../src/webMain/resources/ait-cmp-bridge/core.mjs';
import {base, callBridge} from './helpers.mjs';

class PermissionError extends Error {}
const protectedFn = (status, calls, result = () => { throw new PermissionError(); }) => Object.assign(async () => { calls.push('called'); return result(); }, {getPermission: async () => status, openPermissionDialog: async () => 'denied'});
const cases = [
  ['photosGet', 'getPhotos', {maxCount: 1, maxWidth: 360, base64: true}],
  ['cameraOpen', 'openCamera', {maxWidth: 1024, base64: false}],
  ['contactsGet', 'getContacts', {size: 10, offset: 0}],
  ['locationGet', 'getLocation', {accuracy: 3}],
];

// A8: a denied permission is a result, and the protected official function is not called.
for (const [method, official, payload] of cases) {
  test(`${method} returns the denial result without calling Device.${official}`, async () => {
    const calls = [];
    const b = createBridge({...base(), PermissionError, Device: {[official]: protectedFn('denied', calls)}});
    assert.deepEqual(await callBridge(b, method, payload), {permission: 'denied'});
    assert.deepEqual(calls, []);
  });

  test(`${method} maps an official permission error to the denial result`, async () => {
    const calls = [];
    const b = createBridge({...base(), PermissionError, Device: {[official]: protectedFn('notDetermined', calls)}});
    assert.deepEqual(await callBridge(b, method, payload), {permission: 'denied'});
    assert.deepEqual(calls, ['called']);
  });

  test(`${method} keeps other official errors as failures`, async () => {
    const b = createBridge({...base(), PermissionError, Device: {[official]: protectedFn('allowed', [], () => { throw Error('private'); })}});
    await assert.rejects(callBridge(b, method, payload), {code: 'SDK_FAILURE'});
  });
}

test('albumItemsGet maps NOT_ALLOWED to the denial result', async () => {
  const getAlbumItems = Object.assign(async () => { throw Object.assign(Error('private'), {code: 'NOT_ALLOWED'}); }, {isSupported: () => true});
  const b = createBridge({...base(), Device: {getAlbumItems}});
  assert.deepEqual(await callBridge(b, 'albumItemsGet', {types: ['PHOTO'], maxCount: 1, maxWidth: 360, base64: false}), {permission: 'denied'});
});
