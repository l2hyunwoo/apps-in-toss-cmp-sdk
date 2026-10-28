const FLAGS = ['withBackButton', 'withHomeButton', 'withTitle', 'transparentBackground'];
const COLOR = /^#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/;

export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    NAVIGATION_BAR: () => available(sdk.NavigationBar?.setOptions),
  },
  calls: {
    navigationBarOptions: payload => {
      const options = {};
      for (const name of FLAGS) {
        if (payload[name] === undefined) continue;
        if (typeof payload[name] !== 'boolean') throw new TypeError(`${name} must be a boolean`);
        options[name] = payload[name];
      }
      // `null` is meaningful: it resets the background to the theme default.
      if (payload.backgroundColor !== undefined) {
        if (payload.backgroundColor !== null && (typeof payload.backgroundColor !== 'string' || !COLOR.test(payload.backgroundColor))) throw new TypeError('backgroundColor must be #RRGGBB or #RRGGBBAA');
        options.backgroundColor = payload.backgroundColor;
      }
      if (payload.theme !== undefined) {
        if (payload.theme !== 'light' && payload.theme !== 'dark') throw new TypeError('theme must be light or dark');
        options.theme = payload.theme;
      }
      return () => { requireCapability('NAVIGATION_BAR'); return sdk.NavigationBar.setOptions(options); };
    },
  },
});
