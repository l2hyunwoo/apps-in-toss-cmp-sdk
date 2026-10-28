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

// 가짜 공식 주문이다. 테스트가 processProductGrant의 resolve 값을 볼 수 있도록 grant promise를 기록한다.
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

    // Kotlin이 지급한다. 엄격한 true가 공식 callback에 전달되고, 이어서 success가 주문을 끝낸다.
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

    // Kotlin grant가 throw했거나(reply null) boolean이 아닌 값을 답하면 공식 callback은 false를 받는다.
    for (const value of [null, 'true']) {
      o = open();
      o.state.purchase(grantParams);
      await callBridge(o.bridge, 'reply', {replyId: o.events[0].replyId, value});
      assert.equal(await o.state.grants[0], false);
      o.state.onError({code: 'PRODUCT_NOT_GRANTED_BY_PARTNER', message: 'private'});
      assert.deepEqual(o.events[1], {type: 'error', code: 'PRODUCT_NOT_GRANTED_BY_PARTNER'});
      o.stop();
    }

    // 수집이 취소되거나 client가 닫히면(둘 다 stream을 멈춘다) 대기 중인 grant는 false로 끝난다.
    o = open();
    o.state.purchase(grantParams);
    const pending = o.events[0].replyId;
    o.stop();
    assert.equal(await o.state.grants[0], false);
    assert.equal(o.state.cleaned, 1);
    await callBridge(o.bridge, 'reply', {replyId: pending, value: true});
    // stop 후에 요청한 grant는 Kotlin을 기다리지 않는다.
    assert.equal(await o.state.purchase(grantParams), false);
    assert.equal(o.events.length, 1);
    await tick();

    // 문서화되지 않은 오류와 버전 오류는 SDK 세부 정보를 노출하지 않는다.
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
