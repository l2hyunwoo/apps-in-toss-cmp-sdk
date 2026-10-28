class PermissionError extends Error {}
const withPermission = (fn, status = 'allowed') => Object.assign(fn, {getPermission: async () => status, openPermissionDialog: async () => status});
const clipboard = settle => ({PermissionError, Clipboard: {
  getText: withPermission(async () => settle('copied')),
  setText: withPermission(async () => { await settle(undefined); throw new PermissionError('private'); }),
}});

export default {
  calls: {
    clipboardGetText: {payload: {}, capability: 'CLIPBOARD', sdk: clipboard, expect: {permission: 'allowed', value: 'copied'}},
    // stub은 쓰기를 거부한다. 권한 거부는 실패가 아니라 결과로 와야 한다.
    clipboardSetText: {payload: {text: 't'}, invalid: [{}, {text: 1}], capability: 'CLIPBOARD', sdk: clipboard, expect: {permission: 'denied'}},
  },
  streams: {},
};
