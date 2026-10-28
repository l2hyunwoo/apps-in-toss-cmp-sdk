import assert from 'node:assert/strict';

const file = settle => ({File: {saveBase64: Object.assign(async () => settle(undefined), {isSupported: () => true})}});

export default {
  calls: {
    fileSaveBase64: {
      payload: {data: 'AA==', fileName: 'a.png', mimeType: 'image/png'},
      invalid: [{data: '', fileName: 'a.png', mimeType: 'image/png'}, {data: 'AA==', fileName: ' ', mimeType: 'image/png'}, {data: 'AA==', fileName: 'a.png'}],
      capability: 'SAVE_FILE',
      sdk: file,
      expect: null,
    },
  },
  streams: {
    pdfViewer: {
      payload: {data: 'AA=='},
      run: async ({createBridge, base, tick}) => {
        for (const payload of [{data: ''}, {data: 'AA==', filename: ''}, {data: 'AA==', filename: 1}]) {
          assert.throws(() => createBridge(base()).stream('pdfViewer', JSON.stringify(payload), () => {}, () => {}), TypeError);
        }
        let error;
        createBridge(base()).stream('pdfViewer', '{"data":"AA=="}', () => assert.fail(), e => { error = JSON.parse(e); });
        assert.equal(error.code, 'UNSUPPORTED');

        const opened = []; const pending = [];
        const sdk = {...base(), File: {openPDFViewer: Object.assign(params => { opened.push(params); return new Promise((resolve, reject) => pending.push({resolve, reject})); }, {isSupported: () => true})}};
        const events = [];
        createBridge(sdk).stream('pdfViewer', '{"data":"AA==","filename":"a.pdf"}', e => events.push(JSON.parse(e)), () => assert.fail());
        assert.deepEqual(opened, [{data: 'AA==', filename: 'a.pdf'}]);
        pending[0].resolve('CLOSE');
        await tick();
        assert.deepEqual(events, [{type: 'closed'}]);

        const stop = createBridge(sdk).stream('pdfViewer', '{"data":"AA=="}', () => assert.fail('late event'), () => assert.fail('late failure'));
        assert.deepEqual(opened[1], {data: 'AA=='});
        stop();
        pending[1].resolve('CLOSE');
        await tick();

        error = undefined;
        createBridge(sdk).stream('pdfViewer', '{"data":"AA=="}', () => assert.fail(), e => { error = JSON.parse(e); });
        pending[2].reject(Object.assign(Error('private'), {code: 'INVALID_DATA'}));
        await tick();
        assert.deepEqual(error, {code: 'SDK_FAILURE'});
      },
    },
  },
};
