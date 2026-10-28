// 문서화된 주문 결과다. 다른 코드는 세부 정보 없이 실패로 넘긴다.
const ORDER_ERRORS = new Set(['INVALID_PRODUCT_ID', 'PAYMENT_PENDING', 'NETWORK_ERROR', 'INVALID_USER_ENVIRONMENT', 'ITEM_ALREADY_OWNED', 'APP_MARKET_VERIFICATION_FAILED', 'TOSS_SERVER_VERIFICATION_FAILED', 'INTERNAL_ERROR', 'KOREAN_ACCOUNT_ONLY', 'USER_CANCELED', 'PRODUCT_NOT_GRANTED_BY_PARTNER']);

function requireString(value, name) {
  if (typeof value !== 'string' || !value.trim()) throw new TypeError(`${name} is required`);
}

function versionError(error) {
  return error?.code === 'UNSUPPORTED_APP_VERSION' ? {code:'UNSUPPORTED'} : error;
}

function order(start) {
  return ({emit, fail, ask}) => {
    try {
      return start({
        // Kotlin이 엄격한 `true`를 줄 때에만 지급한다. 답이 없거나 실패하면 `false`다.
        grant: event => ask({type:'grant', ...event}, false).then(value => value === true),
        onEvent: event => emit({type:'success', data: event.data}),
        onError: error => { if (ORDER_ERRORS.has(error?.code)) emit({type:'error', code: error.code}); else fail(versionError(error)); },
      });
    } catch (error) { throw versionError(error); }
  };
}

export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    IAP: () => ['createOneTimePurchaseOrder', 'getProductItemList', 'getPendingOrders', 'getCompletedOrRefundedOrders', 'completeProductGrant'].every(name => available(sdk.IAP?.[name])),
    IAP_SUBSCRIPTION: () => [sdk.IAP?.createSubscriptionPurchaseOrder, sdk.IAP?.getSubscriptionInfo].every(available),
  },
  calls: {
    iapProducts: () => () => { requireCapability('IAP'); return sdk.IAP.getProductItemList(); },
    iapPendingOrders: () => () => { requireCapability('IAP'); return sdk.IAP.getPendingOrders(); },
    iapCompletedOrders: ({key}) => {
      if (key != null) requireString(key, 'Page key');
      return () => { requireCapability('IAP'); return sdk.IAP.getCompletedOrRefundedOrders(key == null ? undefined : {key}); };
    },
    iapCompleteGrant: ({orderId}) => {
      requireString(orderId, 'Order id');
      return () => { requireCapability('IAP'); return sdk.IAP.completeProductGrant({params: {orderId}}); };
    },
    iapSubscriptionInfo: ({orderId}) => {
      requireString(orderId, 'Order id');
      return () => { requireCapability('IAP_SUBSCRIPTION'); return sdk.IAP.getSubscriptionInfo({params: {orderId}}); };
    },
  },
  streams: {
    iapOneTimeOrder: ({sku}) => {
      requireString(sku, 'SKU');
      return order(({grant, onEvent, onError}) => {
        requireCapability('IAP');
        return sdk.IAP.createOneTimePurchaseOrder({options: {sku, processProductGrant: ({orderId}) => grant({orderId})}, onEvent, onError});
      });
    },
    iapSubscriptionOrder: ({sku, offerId}) => {
      requireString(sku, 'SKU');
      if (offerId != null) requireString(offerId, 'Offer id');
      return order(({grant, onEvent, onError}) => {
        requireCapability('IAP_SUBSCRIPTION');
        return sdk.IAP.createSubscriptionPurchaseOrder({
          options: {sku, offerId: offerId ?? null, processProductGrant: ({orderId, subscriptionId}) => grant({orderId, subscriptionId: subscriptionId ?? null})},
          onEvent, onError,
        });
      });
    },
  },
});
