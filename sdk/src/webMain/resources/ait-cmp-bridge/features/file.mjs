function requireText(value, name) {
  if (typeof value !== 'string' || !value.trim()) throw new TypeError(`${name} is required`);
}

export default ({sdk, available, requireCapability}) => ({
  capabilities: {
    PDF_VIEWER: () => available(sdk.File?.openPDFViewer),
    SAVE_FILE: () => available(sdk.File?.saveBase64),
  },
  calls: {
    fileSaveBase64: ({data, fileName, mimeType}) => {
      requireText(data, 'data');
      requireText(fileName, 'fileName');
      requireText(mimeType, 'mimeType');
      return () => { requireCapability('SAVE_FILE'); return sdk.File.saveBase64({data, fileName, mimeType}); };
    },
  },
  streams: {
    // call이 아니라 stream이다. promise는 사용자가 뷰어를 닫을 때에만 끝나며, 이는 call timeout보다 길 수 있다.
    pdfViewer: ({data, filename}) => {
      requireText(data, 'data');
      if (filename !== undefined) requireText(filename, 'filename');
      return ({emit, fail}) => {
        requireCapability('PDF_VIEWER');
        Promise.resolve(sdk.File.openPDFViewer(filename === undefined ? {data} : {data, filename})).then(() => emit({type: 'closed'}), fail);
      };
    },
  },
});
