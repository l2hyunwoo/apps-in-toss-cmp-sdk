// Permission denial is a documented outcome, so it resolves as a result instead of failing.
const denied = sdk => error => {
  if (typeof sdk.PermissionError === 'function' && error instanceof sdk.PermissionError) return {permission: 'denied'};
  throw error;
};

export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    CLIPBOARD: () => available(sdk.Clipboard?.getText) && available(sdk.Clipboard?.setText),
  },
  calls: {
    clipboardGetText: () => () => { requireCapability('CLIPBOARD'); return sdk.Clipboard.getText().then(text => ({text}), denied(sdk)); },
    clipboardSetText: ({text}) => {
      if (typeof text !== 'string') throw new TypeError('Clipboard text must be a string');
      return () => { requireCapability('CLIPBOARD'); return sdk.Clipboard.setText(text).then(() => ({}), denied(sdk)); };
    },
  },
});
