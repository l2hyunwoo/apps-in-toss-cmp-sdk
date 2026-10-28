export default {
  calls: {
    navigationBarOptions: {
      payload: {withBackButton: false, backgroundColor: null, theme: 'dark'},
      invalid: [{withTitle: 'yes'}, {backgroundColor: 'red'}, {backgroundColor: ['#ffffff']}, {theme: 'blue'}],
      capability: 'NAVIGATION_BAR',
      sdk: settle => ({NavigationBar: {setOptions: async options => settle(options)}}),
      // The fake echoes its input: omitted fields stay omitted and null is forwarded.
      expect: {withBackButton: false, backgroundColor: null, theme: 'dark'},
    },
  },
  streams: {},
  absentInMin: ['NAVIGATION_BAR'],
};
