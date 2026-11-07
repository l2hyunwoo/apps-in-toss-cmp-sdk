const FLAGS = ['withBackButton', 'withHomeButton', 'withTitle', 'transparentBackground'];
const COLOR = /^#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$/;

export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    NAVIGATION_BAR: () => available(sdk.NavigationBar?.setOptions),
    NAVIGATION_BAR_ACCESSORY_BUTTON: () => available(sdk.NavigationBar?.addAccessoryButton),
  },
  calls: {
    navigationBarOptions: payload => {
      const options = {};
      for (const name of FLAGS) {
        if (payload[name] === undefined) continue;
        if (typeof payload[name] !== 'boolean') throw new TypeError(`${name} must be a boolean`);
        options[name] = payload[name];
      }
      // `null`에는 의미가 있다. 배경을 테마 기본값으로 되돌린다.
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
    navigationBarAccessoryAdd: payload => {
      const accessibilityLabel = text(payload.accessibilityLabel, 'accessibilityLabel');
      const name = text(payload.icon?.name, 'icon.name');
      return () => { requireCapability('NAVIGATION_BAR_ACCESSORY_BUTTON'); return sdk.NavigationBar.addAccessoryButton({accessibilityLabel, icon: {name}}); };
    },
    navigationBarAccessoryRemove: () => () => { requireCapability('NAVIGATION_BAR_ACCESSORY_BUTTON'); return sdk.NavigationBar.removeAccessoryButton(); },
  },
  streams: {
    // 공식 클릭 이벤트에는 값이 없다. Kotlin stream은 JSON 객체를 기대하므로 빈 객체를 보낸다.
    navigationBarAccessoryClicks: () => ({emit, fail}) => {
      requireCapability('NAVIGATION_BAR_ACCESSORY_BUTTON');
      return sdk.NavigationBar.subscribeAccessoryButton({onEvent: () => emit({}), onError: fail});
    },
  },
});

function text(value, name) {
  if (typeof value !== 'string' || value.trim() === '') throw new TypeError(`${name} must be a non-blank string`);
  return value;
}
