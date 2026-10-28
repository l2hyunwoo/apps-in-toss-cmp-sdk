export default {
  calls: {
    anonymousKey: {payload: {}, capability: 'USER_ID', sdk: settle => ({User: {getAnonymousKey: Object.assign(async () => settle({type: 'HASH', hash: 'id'}), {isSupported: () => true})}}), expect: {type: 'HASH', hash: 'id'}},
  },
  streams: {},
};
