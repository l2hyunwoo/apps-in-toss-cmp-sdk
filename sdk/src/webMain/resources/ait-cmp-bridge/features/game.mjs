function requireResult(result) {
  if (result == null) throw {code:'UNSUPPORTED'};
  return result;
}

export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    GAME_PROFILE: () => available(sdk.Game?.getUserProfile),
    LEADERBOARD: () => [sdk.Game?.setLeaderboardScore, sdk.Game?.openLeaderboard].every(available),
    GAME_INFO_OVERLAY: () => available(sdk.Game?.subscribeInfoOverlayHidden),
  },
  calls: {
    gameProfile: () => () => { requireCapability('GAME_PROFILE'); return Promise.resolve(sdk.Game.getUserProfile()).then(requireResult); },
    leaderboardSubmit: ({score}) => {
      if (typeof score !== 'string' || !Number.isFinite(Number(score))) throw new TypeError('Score must be a finite number string');
      return () => { requireCapability('LEADERBOARD'); return Promise.resolve(sdk.Game.setLeaderboardScore({score})).then(requireResult); };
    },
    leaderboardOpen: () => () => { requireCapability('LEADERBOARD'); return sdk.Game.openLeaderboard(); },
  },
  streams: {
    gameInfoOverlayHidden: () => ({emit, fail}) => {
      requireCapability('GAME_INFO_OVERLAY');
      return sdk.Game.subscribeInfoOverlayHidden({onEvent: () => emit({}), onError: fail});
    },
  },
});
