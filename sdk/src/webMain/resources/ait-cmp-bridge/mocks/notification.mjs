export default {
  Notification: {
    requestAgreement: ({onEvent}) => {
      console.info('[CMP] Mock notification agreement accepted.');
      const timer = setTimeout(() => onEvent({type: 'newAgreement'}), 0);
      return () => clearTimeout(timer);
    },
  },
};
