const supported = fn => Object.assign(fn, {isSupported: () => true});
const SHOW_EVENTS = [{type: 'requested'}, {type: 'show'}, {type: 'impression'}, {type: 'userEarnedReward', data: {unitType: 'mock-coin', unitAmount: 1}}, {type: 'dismissed'}];

// Emits events on later ticks like the host does, and stops after cleanup.
function play(events, onEvent) {
  let active = true;
  events.reduce((previous, event) => previous.then(() => { if (active) onEvent(event); }), Promise.resolve());
  return () => { active = false; };
}

const load = supported(({options, onEvent}) => {
  console.info(`[CMP] Mock full-screen ad loaded: ${options.adGroupId}`);
  return play([{type: 'loaded'}], onEvent);
});
const show = supported(({options, onEvent}) => {
  console.info(`[CMP] Mock full-screen ad shown with a reward: ${options.adGroupId}`);
  return play(SHOW_EVENTS, onEvent);
});

export default {
  loadFullScreenAd: load,
  showFullScreenAd: show,
  GoogleAdMob: {
    loadAppsInTossAdMob: supported(({options, onEvent}) => {
      console.info(`[CMP] Mock AdMob ad loaded: ${options.adGroupId}`);
      return play([{type: 'loaded', data: {adGroupId: options.adGroupId, adUnitId: 'mock-ad-unit', responseInfo: {adNetworkInfoArray: [], loadedAdNetworkInfo: null, responseId: null}}}], onEvent);
    }),
    showAppsInTossAdMob: show,
    isAppsInTossAdMobLoaded: supported(async () => true),
  },
  TossAds: {
    initialize: supported(({callbacks}) => { Promise.resolve().then(() => callbacks?.onInitialized?.()); }),
    attachBanner: supported((adGroupId, target, {callbacks} = {}) => {
      console.info(`[CMP] Mock banner attached: ${adGroupId}`);
      if (typeof target?.ownerDocument?.createElement === 'function') {
        const placeholder = target.ownerDocument.createElement('div');
        placeholder.textContent = 'Mock banner ad';
        Object.assign(placeholder.style, {width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', background: '#f2f4f6', color: '#4e5968', font: '14px sans-serif'});
        target.appendChild(placeholder);
      }
      const payload = {slotId: `mock-slot-${adGroupId}`, adGroupId, adMetadata: {creativeId: 'mock-creative', requestId: 'mock-request'}};
      Promise.resolve().then(() => { callbacks?.onAdRendered?.(payload); callbacks?.onAdImpression?.(payload); });
      return {destroy: () => target?.replaceChildren?.()};
    }),
    destroy: supported(() => {}),
    destroyAll: supported(() => {}),
  },
};
