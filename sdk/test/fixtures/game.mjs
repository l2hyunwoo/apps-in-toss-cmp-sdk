const supported = fn => Object.assign(fn, {isSupported: () => true});
const game = settle => ({Game: {
  getUserProfile: supported(async () => settle({statusCode: 'SUCCESS', nickname: 'n', profileImageUri: ''})),
  setLeaderboardScore: supported(async () => settle({statusCode: 'SUCCESS'})),
  openLeaderboard: supported(async () => settle(undefined)),
}});

export default {
  calls: {
    gameProfile: {payload: {}, capability: 'GAME_PROFILE', sdk: game, expect: {statusCode: 'SUCCESS', nickname: 'n', profileImageUri: ''}},
    leaderboardSubmit: {payload: {score: '7'}, invalid: [{score: 'abc'}, {score: 7}], capability: 'LEADERBOARD', sdk: game, expect: {statusCode: 'SUCCESS'}},
    leaderboardOpen: {payload: {}, capability: 'LEADERBOARD', sdk: game, expect: null},
  },
  streams: {
    gameInfoOverlayHidden: {
      payload: {},
      run: async ({createBridge, base, assert}) => {
        const removed = []; let listener;
        const sdk = {...base(), Game: {subscribeInfoOverlayHidden: ({onEvent, onError}) => { listener = {onEvent, onError}; return () => removed.push('overlay'); }}};
        const b = createBridge(sdk);
        assert.equal(b.supports('GAME_INFO_OVERLAY'), true);
        const events = [];
        const stop = b.stream('gameInfoOverlayHidden', '{}', e => events.push(JSON.parse(e)), () => assert.fail());
        listener.onEvent(); listener.onEvent();
        stop(); stop(); listener.onEvent();
        assert.deepEqual(events, [{}, {}]);
        assert.deepEqual(removed, ['overlay']);

        let error;
        createBridge(sdk).stream('gameInfoOverlayHidden', '{}', () => assert.fail(), e => { error = JSON.parse(e); });
        listener.onError(Error('private'));
        assert.deepEqual(error, {code: 'SDK_FAILURE'});
        assert.deepEqual(removed, ['overlay', 'overlay']);

        createBridge(base()).stream('gameInfoOverlayHidden', '{}', () => assert.fail(), e => { error = JSON.parse(e); });
        assert.equal(error.code, 'UNSUPPORTED');
      },
    },
  },
  absentInMin: ['GAME_INFO_OVERLAY'],
};
