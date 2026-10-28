const review = settle => ({Review: {request: Object.assign(async () => settle(undefined), {isSupported: () => true})}});

export default {
  calls: {
    reviewRequest: {payload: {}, capability: 'REVIEW', sdk: review, expect: null},
  },
  streams: {},
};
