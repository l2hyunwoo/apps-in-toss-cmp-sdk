export default {
  Environment: {environment:'sandbox'},
  SafeArea: {get: () => ({top:0,left:0,right:0,bottom:0}), subscribe: () => () => {}},
  graniteEvent: {addEventListener: () => () => {}},
};
