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
    // A stream, not a call: the promise settles only when the user closes the viewer, which can outlast the call timeout.
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
