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

// A8: 거부된 권한은 결과이고, 보호된 공식 함수는 호출되지 않는다.
for (const [method, official, payload] of cases) {
  test(`${method}는 Device.${official}를 호출하지 않고 거부 결과를 반환한다`, async () => {
    const calls = [];
    const b = createBridge({...base(), PermissionError, Device: {[official]: protectedFn('denied', calls)}});
    assert.deepEqual(await callBridge(b, method, payload), {permission: 'denied'});
    assert.deepEqual(calls, []);
  });

  test(`${method}는 공식 권한 오류를 거부 결과로 바꾼다`, async () => {
    const calls = [];
    const b = createBridge({...base(), PermissionError, Device: {[official]: protectedFn('notDetermined', calls)}});
    assert.deepEqual(await callBridge(b, method, payload), {permission: 'denied'});
    assert.deepEqual(calls, ['called']);
  });

  test(`${method}는 다른 공식 오류를 실패로 유지한다`, async () => {
    const b = createBridge({...base(), PermissionError, Device: {[official]: protectedFn('allowed', [], () => { throw Error('private'); })}});
    await assert.rejects(callBridge(b, method, payload), {code: 'SDK_FAILURE'});
  });
}

test('albumItemsGet은 NOT_ALLOWED를 거부 결과로 바꾼다', async () => {
  const getAlbumItems = Object.assign(async () => { throw Object.assign(Error('private'), {code: 'NOT_ALLOWED'}); }, {isSupported: () => true});
  const b = createBridge({...base(), Device: {getAlbumItems}});
  assert.deepEqual(await callBridge(b, 'albumItemsGet', {types: ['PHOTO'], maxCount: 1, maxWidth: 360, base64: false}), {permission: 'denied'});
});
