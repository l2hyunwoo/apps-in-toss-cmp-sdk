class PermissionError extends Error {}
const withPermission = (fn, status = 'allowed') => Object.assign(fn, {getPermission: async () => status, openPermissionDialog: async () => status});
const clipboard = settle => ({PermissionError, Clipboard: {
  getText: withPermission(async () => settle('copied')),
  setText: withPermission(async () => { await settle(undefined); throw new PermissionError('private'); }),
}});

export default {
  calls: {
    clipboardGetText: {payload: {}, capability: 'CLIPBOARD', sdk: clipboard, expect: {permission: 'allowed', value: 'copied'}},
    // The stub denies writes: permission denial must arrive as a result, not a failure.
    clipboardSetText: {payload: {text: 't'}, invalid: [{}, {text: 1}], capability: 'CLIPBOARD', sdk: clipboard, expect: {permission: 'denied'}},
  },
  streams: {},
};
