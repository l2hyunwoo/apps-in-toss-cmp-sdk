let text = '';
const allowed = fn => Object.assign(fn, {getPermission: async () => 'allowed', openPermissionDialog: async () => 'allowed'});

export default {
  Clipboard: {
    getText: allowed(async () => text),
    setText: allowed(async value => { text = value; }),
  },
};
