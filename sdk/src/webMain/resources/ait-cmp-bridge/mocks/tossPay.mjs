const authorize = async () => { console.info('[CMP] Mock Toss Pay authorization succeeded. Approve the payment on your server.'); return {success: true}; };

export default {
  TossPay: {
    authorize,
    authorizeSubscription: Object.assign(async () => authorize(), {isSupported: () => true}),
  },
};
