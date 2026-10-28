import { guarded } from './permissions.mjs';

const protectedFn = fn => typeof fn === 'function' && typeof fn.getPermission === 'function';

export default ({sdk, requireCapability}) => ({
  capabilities: {
    CLIPBOARD: () => protectedFn(sdk.Clipboard?.getText) && protectedFn(sdk.Clipboard?.setText),
  },
  calls: {
    clipboardGetText: () => () => { requireCapability('CLIPBOARD'); return guarded(sdk, sdk.Clipboard.getText, () => sdk.Clipboard.getText()); },
    clipboardSetText: ({text}) => {
      if (typeof text !== 'string') throw new TypeError('Clipboard text must be a string');
      return () => { requireCapability('CLIPBOARD'); return guarded(sdk, sdk.Clipboard.setText, () => sdk.Clipboard.setText(text).then(() => null)); };
    },
  },
});
