const supported = fn => Object.assign(fn, {isSupported: () => true});

function order(processProductGrant, grantParams) {
  return ({onEvent, onError}) => {
    let active = true;
    const data = {orderId: 'mock-order-1', displayName: '모의 상품', displayAmount: '1,000원', amount: 1000, currency: 'KRW', fraction: 0, miniAppIconUrl: null};
    console.info('[CMP] Mock purchase order. No payment is made.');
    Promise.resolve(processProductGrant({orderId: data.orderId, ...grantParams})).then(granted => {
      if (!active) return;
      if (granted) onEvent({type: 'success', data}); else onError({code: 'PRODUCT_NOT_GRANTED_BY_PARTNER'});
    }, onError);
    return () => { active = false; };
  };
}

export default {
  IAP: {
    createOneTimePurchaseOrder: supported(({options, ...handlers}) => order(options.processProductGrant, {})(handlers)),
    createSubscriptionPurchaseOrder: supported(({options, ...handlers}) => order(options.processProductGrant, {subscriptionId: 'mock-subscription-1'})(handlers)),
    getProductItemList: supported(async () => ({products: [
      {sku: 'mock.coin', type: 'CONSUMABLE', displayName: '모의 코인', displayAmount: '1,000원', iconUrl: '', description: '모의 상품'},
      {sku: 'mock.premium', type: 'SUBSCRIPTION', displayName: '모의 구독', displayAmount: '4,900원', iconUrl: '', description: '모의 구독', renewalCycle: 'MONTHLY', offers: [{type: 'FREE_TRIAL', offerId: 'mock-trial', period: 'P7D'}]},
    ]})),
    getPendingOrders: supported(async () => ({orders: []})),
    getCompletedOrRefundedOrders: supported(async () => ({hasNext: false, nextKey: null, orders: [{orderId: 'mock-order-1', sku: 'mock.coin', status: 'COMPLETED', date: '2026-01-01T00:00:00'}]})),
    completeProductGrant: supported(async () => true),
    getSubscriptionInfo: supported(async () => ({subscription: {catalogId: 1, status: 'ACTIVE', expiresAt: null, isAutoRenew: true, gracePeriodExpiresAt: null, isAccessible: true}})),
  },
};
