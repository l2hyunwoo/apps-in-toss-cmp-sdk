let text = '';

export default {
  Clipboard: {
    getText: async () => text,
    setText: async value => { text = value; },
  },
};
