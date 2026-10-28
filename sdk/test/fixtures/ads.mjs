import assert from 'node:assert/strict';
import mockAds from '../../src/webMain/resources/ait-cmp-bridge/mocks/ads.mjs';

const supported = fn => Object.assign(fn, {isSupported: () => true});
const admob = settle => ({GoogleAdMob: {
  loadAppsInTossAdMob: supported(() => () => {}),
  showAppsInTossAdMob: supported(() => () => {}),
  isAppsInTossAdMobLoaded: supported(async () => settle(true)),
}});
const tossAds = (settle, log = []) => ({TossAds: {
  initialize: supported(({callbacks}) => { Promise.resolve(settle(null)).then(() => callbacks.onInitialized()); }),
  attachBanner: supported(() => ({destroy: () => log.push('destroy')})),
  destroy: supported(slotId => { log.push(`destroy:${slotId}`); }),
  destroyAll: supported(() => { log.push('destroyAll'); }),
}});

/** Fake official callback API: records the latest params and counts cleanup calls. */
function official() {
  const state = {cleaned: 0};
  state.fn = supported(params => { state.params = params; return () => { state.cleaned++; }; });
  return state;
}

function streamRun(method, payload, invalid, capability, place) {
  return async ({createBridge, base}) => {
    for (const bad of invalid) assert.throws(() => createBridge(base()).stream(method, JSON.stringify(bad), () => {}, () => {}), TypeError);
    let error;
    createBridge(base()).stream(method, JSON.stringify(payload), () => assert.fail(), e => { error = JSON.parse(e); });
    assert.equal(error.code, 'UNSUPPORTED');

    const ok = official();
    const b = createBridge({...base(), ...place(ok.fn)});
    assert.equal(b.supports(capability), true);
    const events = [];
    const stop = b.stream(method, JSON.stringify(payload), e => events.push(JSON.parse(e)), () => assert.fail());
    assert.equal(ok.params.options.adGroupId, payload.adGroupId);
    ok.params.onEvent({type: 'loaded'});
    stop(); stop();
    ok.params.onEvent({type: 'late'});
    assert.deepEqual(events, [{type: 'loaded'}]);
    assert.equal(ok.cleaned, 1);

    const failing = official();
    createBridge({...base(), ...place(failing.fn)}).stream(method, JSON.stringify(payload), () => assert.fail(), e => { error = JSON.parse(e); });
    failing.params.onError(Object.assign(Error('private message'), {code: 'private'}));
    assert.deepEqual(error, {code: 'SDK_FAILURE'});
    assert.equal(failing.cleaned, 1);
  };
}

const fullScreen = slot => fn => {
  const other = supported(() => () => {});
  return slot === 'load' ? {loadFullScreenAd: fn, showFullScreenAd: other} : {loadFullScreenAd: other, showFullScreenAd: fn};
};
const admobWith = name => fn => {
  const {GoogleAdMob} = admob(v => v);
  return {GoogleAdMob: {...GoogleAdMob, [name]: fn}};
};

function fakeDocument() {
  const body = {children: [], appendChild(child) { this.children.push(child); }};
  const createElement = tag => ({
    tag, style: {}, attributes: {}, children: [], ownerDocument: document,
    setAttribute(name, value) { this.attributes[name] = value; },
    appendChild(child) { this.children.push(child); },
    replaceChildren() { this.children = []; },
    remove() { const index = body.children.indexOf(this); if (index >= 0) body.children.splice(index, 1); },
  });
  const document = {body, createElement};
  return document;
}

async function withDocument(run) {
  const previous = globalThis.document;
  globalThis.document = fakeDocument();
  try { await run(globalThis.document); } finally { if (previous === undefined) delete globalThis.document; else globalThis.document = previous; }
}

const BOUNDS = {left: 10, top: 20, width: 300, height: 96};

export default {
  calls: {
    admobLoaded: {payload: {adGroupId: 'ad'}, invalid: [{}, {adGroupId: ' '}], capability: 'ADMOB', sdk: admob, expect: true},
    bannerInitialize: {payload: {}, capability: 'BANNER_AD', sdk: tossAds, expect: null},
    bannerDestroy: {payload: {slotId: 'slot'}, invalid: [{}, {slotId: 1}], capability: 'BANNER_AD', sdk: tossAds, expect: null},
    bannerDestroyAll: {payload: {}, capability: 'BANNER_AD', sdk: tossAds, expect: null},
  },
  streams: {
    loadFullScreenAd: {payload: {adGroupId: 'ad'}, run: streamRun('loadFullScreenAd', {adGroupId: 'ad'}, [{}, {adGroupId: ''}], 'FULL_SCREEN_AD', fullScreen('load'))},
    showFullScreenAd: {
      payload: {adGroupId: 'ad'},
      run: async ctx => {
        await streamRun('showFullScreenAd', {adGroupId: 'ad'}, [{adGroupId: 3}], 'FULL_SCREEN_AD', fullScreen('show'))(ctx);
        const info = console.info; console.info = () => {};
        try {
          const events = [];
          const stop = ctx.createBridge({...ctx.base(), ...mockAds}).stream('showFullScreenAd', '{"adGroupId":"ad"}', e => events.push(JSON.parse(e)), () => assert.fail());
          for (let i = 0; i < 6; i++) await ctx.tick();
          stop();
          assert.deepEqual(events.map(e => e.type), ['requested', 'show', 'impression', 'userEarnedReward', 'dismissed']);
          assert.deepEqual(events[3].data, {unitType: 'mock-coin', unitAmount: 1});
        } finally { console.info = info; }
      },
    },
    admobLoad: {payload: {adGroupId: 'ad', referrer: null}, run: streamRun('admobLoad', {adGroupId: 'ad', referrer: 'home'}, [{adGroupId: 'ad', referrer: 1}], 'ADMOB', admobWith('loadAppsInTossAdMob'))},
    admobShow: {payload: {adGroupId: 'ad'}, run: streamRun('admobShow', {adGroupId: 'ad'}, [{}], 'ADMOB', admobWith('showAppsInTossAdMob'))},
    bannerAttach: {
      payload: {adGroupId: 'ad', bounds: BOUNDS},
      run: async ({createBridge, base, callBridge, tick}) => {
        const payload = JSON.stringify({adGroupId: 'ad', bounds: BOUNDS, variant: 'card'});
        for (const bad of [{adGroupId: 'ad'}, {adGroupId: 'ad', bounds: {...BOUNDS, width: -1}}, {adGroupId: 'ad', bounds: BOUNDS, variant: 'wide'}, {adGroupId: 'ad', bounds: BOUNDS, extra: 1}]) {
          assert.throws(() => createBridge(base()).stream('bannerAttach', JSON.stringify(bad), () => {}, () => {}), TypeError);
        }
        let error;
        createBridge({...base(), ...tossAds(v => v)}).stream('bannerAttach', payload, () => assert.fail(), e => { error = JSON.parse(e); });
        assert.equal(error.code, 'UNSUPPORTED', 'no document');

        await withDocument(async document => {
          const log = [];
          const sdk = {...base(), ...tossAds(v => v, log)};
          let attached;
          sdk.TossAds.attachBanner = supported((adGroupId, target, options) => { attached = {adGroupId, target, options}; return {destroy: () => log.push('destroy')}; });
          const b = createBridge(sdk);
          const events = [];
          const stop = b.stream('bannerAttach', payload, e => events.push(JSON.parse(e)), () => assert.fail());
          const [element] = document.body.children;
          assert.equal(attached.target, element);
          assert.equal(attached.options.variant, 'card');
          assert.equal(element.attributes['data-ait-banner'], '');
          assert.deepEqual([element.style.position, element.style.left, element.style.top, element.style.width, element.style.height], ['fixed', '10px', '20px', '300px', '96px']);

          const meta = {slotId: 's1', adGroupId: 'ad', adMetadata: {creativeId: 'c', requestId: 'r'}};
          attached.options.callbacks.onAdRendered(meta);
          attached.options.callbacks.onAdFailedToRender({slotId: 's1', adGroupId: 'ad', adMetadata: {}, error: {code: 7, message: 'private'}});
          assert.deepEqual(events[1], {type: 'rendered', slotId: 's1', adGroupId: 'ad', creativeId: 'c', requestId: 'r'});
          assert.deepEqual(events[2], {type: 'failedToRender', slotId: 's1', adGroupId: 'ad', code: 7});

          assert.equal(events[0].type, 'bounds');
          await callBridge(b, 'reply', {replyId: events[0].replyId, value: {left: 1, top: 2, width: 3, height: 4}});
          await tick();
          assert.equal(element.style.left, '1px');
          const next = events.filter(e => e.type === 'bounds');
          assert.equal(next.length, 2);

          await callBridge(b, 'bannerDestroy', {slotId: 's1'});
          assert.deepEqual(document.body.children, []);
          assert.equal(events.at(-1).type, 'destroyed');
          stop(); stop();
          assert.deepEqual(log, ['destroy:s1', 'destroy']);

          const second = createBridge(sdk);
          const stopSecond = second.stream('bannerAttach', payload, () => {}, () => assert.fail());
          assert.equal(document.body.children.length, 1);
          await callBridge(second, 'bannerDestroyAll', {});
          assert.equal(document.body.children.length, 0);
          stopSecond();

          sdk.TossAds.attachBanner = supported(() => { throw Error('private'); });
          createBridge(sdk).stream('bannerAttach', payload, () => assert.fail(), e => { error = JSON.parse(e); });
          assert.deepEqual(error, {code: 'SDK_FAILURE'});
          assert.equal(document.body.children.length, 0);
        });
      },
    },
  },
};
