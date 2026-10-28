import assert from 'node:assert/strict';

const supported = fn => Object.assign(fn, {isSupported: () => true});
const iap = settle => ({IAP: {
  createOneTimePurchaseOrder: supported(() => () => {}),
  createSubscriptionPurchaseOrder: supported(() => () => {}),
  getProductItemList: supported(async () => settle({products: []})),
  getPendingOrders: supported(async () => settle({orders: [{orderId: 'o1', sku: 's', paymentCompletedDate: 'd'}]})),
  getCompletedOrRefundedOrders: supported(async params => settle({hasNext: false, orders: [], params: params ?? null})),
  completeProductGrant: supported(async ({params}) => settle(params.orderId === 'o1')),
  getSubscriptionInfo: supported(async ({params}) => settle({subscription: {orderId: params.orderId}})),
}});

// Fake official order: records the grant promise so tests can see what processProductGrant resolved to.
function orderSdk(base, method) {
  const state = {cleaned: 0, grants: []};
  const fn = supported(({options, onEvent, onError}) => {
    state.options = options; state.onEvent = onEvent; state.onError = onError;
    state.purchase = params => { const result = Promise.resolve(options.processProductGrant(params)); state.grants.push(result); return result; };
    return () => { state.cleaned++; };
  });
  return {state, sdk: {...base(), IAP: {...iap(v => v).IAP, [method]: fn}}};
}

function orderRun(method, payload, officialMethod, grantParams, expectedQuestion) {
  return async ({createBridge, base, callBridge, tick}) => {
    const open = () => {
      const {state, sdk} = orderSdk(base, officialMethod);
      const bridge = createBridge(sdk);
      const events = []; const failures = [];
      const stop = bridge.stream(method, JSON.stringify(payload), e => events.push(JSON.parse(e)), e => failures.push(JSON.parse(e)));
      return {state, bridge, events, failures, stop};
    };

    // Kotlin grants: strict true reaches the official callback, then success ends the order.
    let o = open();
    o.state.purchase(grantParams);
    const {replyId, ...question} = o.events[0];
    assert.deepEqual(question, expectedQuestion);
    await callBridge(o.bridge, 'reply', {replyId, value: true});
    assert.equal(await o.state.grants[0], true);
    o.state.onEvent({type: 'success', data: {orderId: 'o1'}});
    assert.deepEqual(o.events[1], {type: 'success', data: {orderId: 'o1'}});
    o.stop(); o.stop();
    assert.equal(o.state.cleaned, 1);

    // Kotlin grant threw (reply null) or replied a non-boolean: the official callback gets false.
    for (const value of [null, 'true']) {
      o = open();
      o.state.purchase(grantParams);
      await callBridge(o.bridge, 'reply', {replyId: o.events[0].replyId, value});
      assert.equal(await o.state.grants[0], false);
      o.state.onError({code: 'PRODUCT_NOT_GRANTED_BY_PARTNER', message: 'private'});
      assert.deepEqual(o.events[1], {type: 'error', code: 'PRODUCT_NOT_GRANTED_BY_PARTNER'});
      o.stop();
    }

    // Collection cancelled or client closed (both stop the stream): the pending grant settles false.
    o = open();
    o.state.purchase(grantParams);
    const pending = o.events[0].replyId;
    o.stop();
    assert.equal(await o.state.grants[0], false);
    assert.equal(o.state.cleaned, 1);
    await callBridge(o.bridge, 'reply', {replyId: pending, value: true});
    // A grant requested after stop never waits for Kotlin.
    assert.equal(await o.state.purchase(grantParams), false);
    assert.equal(o.events.length, 1);
    await tick();

    // Undocumented and version errors do not leak SDK details.
    o = open();
    o.state.onError({code: 'SOMETHING', message: 'private'});
    assert.deepEqual(o.failures, [{code: 'SDK_FAILURE'}]);
    assert.equal(o.state.cleaned, 1);
    o = open();
    o.state.onError({code: 'UNSUPPORTED_APP_VERSION'});
    assert.deepEqual(o.failures, [{code: 'UNSUPPORTED'}]);

    let error;
    const thrower = {...base(), IAP: {...iap(v => v).IAP, [officialMethod]: supported(() => { throw {code: 'UNSUPPORTED_APP_VERSION'}; })}};
    createBridge(thrower).stream(method, JSON.stringify(payload), () => assert.fail(), e => { error = JSON.parse(e); });
    assert.deepEqual(error, {code: 'UNSUPPORTED'});
    createBridge(base()).stream(method, JSON.stringify(payload), () => assert.fail(), e => { error = JSON.parse(e); });
    assert.deepEqual(error, {code: 'UNSUPPORTED'});
    assert.throws(() => createBridge(base()).stream(method, JSON.stringify({...payload, sku: ' '}), () => {}, () => {}), TypeError);
  };
}

export default {
  calls: {
    iapProducts: {payload: {}, capability: 'IAP', sdk: iap, expect: {products: []}},
    iapPendingOrders: {payload: {}, capability: 'IAP', sdk: iap, expect: {orders: [{orderId: 'o1', sku: 's', paymentCompletedDate: 'd'}]}},
    iapCompletedOrders: {
      payload: {key: 'k'}, invalid: [{key: ' '}, {key: 1}], capability: 'IAP', sdk: iap,
      expect: result => assert.deepEqual(result.params, {key: 'k'}),
    },
    iapCompleteGrant: {payload: {orderId: 'o1'}, invalid: [{}, {orderId: ''}], capability: 'IAP', sdk: iap, expect: true},
    iapSubscriptionInfo: {payload: {orderId: 'o1'}, invalid: [{}, {orderId: ' '}], capability: 'IAP_SUBSCRIPTION', sdk: iap, expect: {subscription: {orderId: 'o1'}}},
  },
  streams: {
    iapOneTimeOrder: {
      payload: {sku: 'coin'},
      run: orderRun('iapOneTimeOrder', {sku: 'coin'}, 'createOneTimePurchaseOrder', {orderId: 'o1'}, {type: 'grant', orderId: 'o1'}),
    },
    iapSubscriptionOrder: {
      payload: {sku: 'premium', offerId: 'trial'},
      run: orderRun('iapSubscriptionOrder', {sku: 'premium', offerId: 'trial'}, 'createSubscriptionPurchaseOrder', {orderId: 'o1', subscriptionId: 'sub1'}, {type: 'grant', orderId: 'o1', subscriptionId: 'sub1'}),
    },
  },
};
