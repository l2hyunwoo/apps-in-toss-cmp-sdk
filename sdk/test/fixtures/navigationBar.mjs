import assert from 'node:assert/strict';

const accessory = {accessibilityLabel: '찜하기', icon: {name: 'icon-heart-mono'}};

export default {
  calls: {
    navigationBarOptions: {
      payload: {withBackButton: false, backgroundColor: null, theme: 'dark'},
      invalid: [{withTitle: 'yes'}, {backgroundColor: 'red'}, {backgroundColor: ['#ffffff']}, {theme: 'blue'}],
      capability: 'NAVIGATION_BAR',
      sdk: settle => ({NavigationBar: {setOptions: async options => settle(options)}}),
      // fake는 입력을 그대로 돌려준다. 생략한 필드는 생략된 채로 두고 null은 전달한다.
      expect: {withBackButton: false, backgroundColor: null, theme: 'dark'},
    },
    navigationBarAccessoryAdd: {
      payload: accessory,
      invalid: [{...accessory, accessibilityLabel: ' '}, {...accessory, icon: {name: ''}}, {...accessory, icon: 'icon-heart-mono'}, {accessibilityLabel: 1, icon: {name: 'x'}}, {}],
      capability: 'NAVIGATION_BAR_ACCESSORY_BUTTON',
      sdk: settle => ({NavigationBar: {addAccessoryButton: async options => settle(options)}}),
      expect: accessory,
    },
    navigationBarAccessoryRemove: {
      payload: {},
      capability: 'NAVIGATION_BAR_ACCESSORY_BUTTON',
      sdk: settle => ({NavigationBar: {addAccessoryButton: async () => {}, removeAccessoryButton: async () => settle('removed')}}),
      expect: 'removed',
    },
  },
  streams: {
    navigationBarAccessoryClicks: {
      payload: {},
      run: async ({createBridge, base}) => {
        let error;
        createBridge(base()).stream('navigationBarAccessoryClicks', '{}', () => assert.fail(), e => { error = JSON.parse(e); });
        assert.equal(error.code, 'UNSUPPORTED');

        let listener; let removed = 0;
        const sdk = {...base(), NavigationBar: {addAccessoryButton: async () => {}, subscribeAccessoryButton: params => { listener = params; return () => removed++; }}};
        const events = [];
        const stop = createBridge(sdk).stream('navigationBarAccessoryClicks', '{}', e => events.push(e), () => assert.fail());
        listener.onEvent();
        listener.onEvent();
        // Kotlin stream이 JSON 객체를 기대하므로 원문이 `{}`여야 한다.
        assert.deepEqual(events, ['{}', '{}']);
        stop(); stop();
        listener.onEvent();
        assert.equal(events.length, 2);
        assert.equal(removed, 1);

        createBridge(sdk).stream('navigationBarAccessoryClicks', '{}', () => {}, e => { error = JSON.parse(e); });
        listener.onError(Error('private'));
        assert.deepEqual(error, {code: 'SDK_FAILURE'});
        listener.onEvent();
        assert.equal(removed, 2);
      },
    },
  },
  absentInMin: ['NAVIGATION_BAR', 'NAVIGATION_BAR_ACCESSORY_BUTTON'],
};
