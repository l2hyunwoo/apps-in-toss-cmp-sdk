const BANNER_OPTIONS = {theme: ['auto', 'light', 'dark'], tone: ['blackAndWhite', 'grey'], variant: ['card', 'expanded']};

function requireAdGroupId(adGroupId) {
  if (typeof adGroupId !== 'string' || !adGroupId.trim()) throw new TypeError('adGroupId is required');
}

function validBounds(bounds) {
  return bounds != null && ['left', 'top', 'width', 'height'].every(key => Number.isFinite(bounds[key])) && bounds.width >= 0 && bounds.height >= 0;
}

function place(element, {left, top, width, height}) {
  Object.assign(element.style, {left: `${left}px`, top: `${top}px`, width: `${width}px`, height: `${height}px`});
}

function slotEvent(type, payload) {
  const event = {type, slotId: payload?.slotId, adGroupId: payload?.adGroupId};
  if (payload?.adMetadata?.creativeId !== undefined) Object.assign(event, {creativeId: payload.adMetadata.creativeId, requestId: payload.adMetadata.requestId});
  // Only the numeric code crosses the bridge; SDK messages can contain sensitive data.
  if (type === 'failedToRender') event.code = payload?.error?.code;
  return event;
}

export default ({sdk, available, requireCapability}) => {
  const banners = new Set();
  const adStream = (capability, official, options) => ({emit, fail}) => {
    requireCapability(capability);
    return official()({options, onEvent: emit, onError: fail});
  };
  return {
    capabilities: {
      FULL_SCREEN_AD: () => [sdk.loadFullScreenAd, sdk.showFullScreenAd].every(available),
      ADMOB: () => ['loadAppsInTossAdMob', 'showAppsInTossAdMob', 'isAppsInTossAdMobLoaded'].every(name => available(sdk.GoogleAdMob?.[name])),
      BANNER_AD: () => ['initialize', 'attachBanner', 'destroy', 'destroyAll'].every(name => available(sdk.TossAds?.[name])),
    },
    calls: {
      admobLoaded: ({adGroupId}) => {
        requireAdGroupId(adGroupId);
        return () => { requireCapability('ADMOB'); return sdk.GoogleAdMob.isAppsInTossAdMobLoaded({adGroupId}); };
      },
      bannerInitialize: () => () => {
        requireCapability('BANNER_AD');
        return new Promise((resolve, reject) => sdk.TossAds.initialize({callbacks: {onInitialized: () => resolve(null), onInitializationFailed: reject}}));
      },
      bannerDestroy: ({slotId}) => {
        if (typeof slotId !== 'string' || !slotId) throw new TypeError('slotId is required');
        return () => {
          requireCapability('BANNER_AD');
          sdk.TossAds.destroy(slotId);
          for (const banner of banners) if (banner.slotId === slotId) banner.end();
          return null;
        };
      },
      bannerDestroyAll: () => () => {
        requireCapability('BANNER_AD');
        sdk.TossAds.destroyAll();
        for (const banner of [...banners]) banner.end();
        return null;
      },
    },
    streams: {
      loadFullScreenAd: ({adGroupId}) => { requireAdGroupId(adGroupId); return adStream('FULL_SCREEN_AD', () => sdk.loadFullScreenAd, {adGroupId}); },
      showFullScreenAd: ({adGroupId}) => { requireAdGroupId(adGroupId); return adStream('FULL_SCREEN_AD', () => sdk.showFullScreenAd, {adGroupId}); },
      admobLoad: ({adGroupId, referrer}) => {
        requireAdGroupId(adGroupId);
        if (referrer != null && typeof referrer !== 'string') throw new TypeError('referrer must be a string');
        return adStream('ADMOB', () => sdk.GoogleAdMob.loadAppsInTossAdMob, referrer == null ? {adGroupId} : {adGroupId, referrer});
      },
      admobShow: ({adGroupId}) => { requireAdGroupId(adGroupId); return adStream('ADMOB', () => sdk.GoogleAdMob.showAppsInTossAdMob, {adGroupId}); },
      // The Compose canvas cannot host DOM, so the banner gets its own element over the canvas.
      // Kotlin answers each `bounds` question when the bounds change; the stop fallback (null) ends the loop.
      bannerAttach: ({adGroupId, bounds, ...options}) => {
        requireAdGroupId(adGroupId);
        if (!validBounds(bounds)) throw new TypeError('bounds must be finite CSS px with non-negative size');
        for (const [key, value] of Object.entries(options)) {
          if (!BANNER_OPTIONS[key]?.includes(value)) throw new TypeError(`Invalid banner option: ${key}`);
        }
        return ({emit, ask}) => {
          requireCapability('BANNER_AD');
          const document = globalThis.document;
          if (typeof document?.createElement !== 'function') throw {code: 'UNSUPPORTED'};
          const element = document.createElement('div');
          element.setAttribute('data-ait-banner', '');
          element.style.position = 'fixed';
          element.style.zIndex = '1';
          place(element, bounds);
          document.body.appendChild(element);
          const banner = {slotId: null, end: () => { element.remove(); emit({type: 'destroyed'}); }};
          const forward = type => payload => {
            if (typeof payload?.slotId === 'string') banner.slotId = payload.slotId;
            emit(slotEvent(type, payload));
          };
          let result;
          try {
            result = sdk.TossAds.attachBanner(adGroupId, element, {...options, callbacks: {
              onAdRendered: forward('rendered'), onAdViewable: forward('viewable'), onAdClicked: forward('clicked'),
              onAdImpression: forward('impression'), onAdFailedToRender: forward('failedToRender'), onNoFill: forward('noFill'),
            }});
          } catch (error) { element.remove(); throw error; }
          banners.add(banner);
          let open = true;
          const follow = () => ask({type: 'bounds'}, null).then(next => {
            if (!open || next == null) return;
            if (validBounds(next)) place(element, next);
            follow();
          });
          follow();
          return () => {
            open = false;
            banners.delete(banner);
            element.remove();
            result?.destroy?.();
          };
        };
      },
    },
  };
};
