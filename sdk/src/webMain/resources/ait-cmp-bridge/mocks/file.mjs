export default {
  File: {
    saveBase64: async ({fileName}) => { console.info(`[CMP] Mock file saved: ${fileName}.`); },
    openPDFViewer: async () => { console.info('[CMP] Mock PDF viewer opened and closed.'); return 'CLOSE'; },
  },
};
