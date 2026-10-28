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
  },
  streams: {},
  absentInMin: ['NAVIGATION_BAR'],
};
