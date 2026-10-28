const log = type => async params => { console.info(`[CMP] Mock analytics ${type}:`, params); };

export default {
  Analytics: {log: log('log'), screen: log('screen'), impression: log('impression'), click: log('click')},
};
