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
  streams: {},
};
