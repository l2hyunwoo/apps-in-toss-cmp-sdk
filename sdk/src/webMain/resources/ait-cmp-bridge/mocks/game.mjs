export default {
  Game: {
    getUserProfile: Object.assign(async () => ({statusCode:'SUCCESS', nickname:'모의 사용자', profileImageUri:''}), {isSupported:()=>true}),
    setLeaderboardScore: Object.assign(async () => ({statusCode:'SUCCESS'}), {isSupported:()=>true}),
    openLeaderboard: Object.assign(async () => { console.info('[CMP] Mock leaderboard opened.'); }, {isSupported:()=>true}),
    subscribeInfoOverlayHidden: ({onEvent}) => {
      let active = true;
      queueMicrotask(() => { if (active) { console.info('[CMP] Mock game info overlay hidden.'); onEvent(); } });
      return () => { active = false; };
    },
  },
};
