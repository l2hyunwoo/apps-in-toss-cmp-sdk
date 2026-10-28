class PermissionError extends Error {}
const clipboard = settle => ({PermissionError, Clipboard: {
  getText: async () => settle('copied'),
  setText: async () => { await settle(undefined); throw new PermissionError('private'); },
}});

export default {
  calls: {
    clipboardGetText: {payload: {}, capability: 'CLIPBOARD', sdk: clipboard, expect: {text: 'copied'}},
    // The stub denies writes: permission denial must arrive as a result, not a failure.
    clipboardSetText: {payload: {text: 't'}, invalid: [{}, {text: 1}], capability: 'CLIPBOARD', sdk: clipboard, expect: {permission: 'denied'}},
  },
  streams: {},
};
