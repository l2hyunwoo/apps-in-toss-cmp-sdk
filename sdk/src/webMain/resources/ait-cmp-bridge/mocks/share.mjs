export default {
  Share: {
    sendMessage: async () => {},
    createLink: async ({path}) => `https://mock.invalid/share?deep_link_value=${encodeURIComponent(path)}`,
  },
};
