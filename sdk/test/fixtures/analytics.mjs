const sent = [];
const analytics = settle => ({Analytics: Object.fromEntries(['log', 'screen', 'impression', 'click'].map(name => [name, async params => { sent.push([name, params]); return settle(undefined); }]))});
const badParams = [{logName: 'n', params: {a: {}}}, {logName: 'n', params: []}, {logName: ' ', params: {}}];
const logger = method => ({
  payload: {logName: 'n', params: {a: 1, b: 'x', c: true, d: null}},
  invalid: [...badParams, {params: {log_name: 'n'}}, {params: null}],
  capability: 'ANALYTICS',
  sdk: analytics,
  expect: () => {
    const [name, params] = sent.at(-1);
    if (name !== method || params.log_name !== 'n' || params.a !== 1) throw Error(`unexpected ${method} params`);
  },
});

export default {
  calls: {
    analyticsLog: {
      payload: {logName: 'n', logType: 'event', params: {a: 1}},
      invalid: [...badParams.map(p => ({...p, logType: 'event'})), {logName: 'n', logType: 'other', params: {}}],
      capability: 'ANALYTICS',
      sdk: analytics,
      expect: () => {
        const [name, params] = sent.at(-1);
        if (name !== 'log' || JSON.stringify(params) !== '{"log_name":"n","log_type":"event","params":{"a":1}}') throw Error('unexpected log params');
      },
    },
    analyticsScreen: logger('screen'),
    analyticsImpression: logger('impression'),
    analyticsClick: logger('click'),
  },
  streams: {},
};
