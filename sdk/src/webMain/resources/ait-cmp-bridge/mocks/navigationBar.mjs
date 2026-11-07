export default {
  NavigationBar: {
    setOptions: async () => { console.info('[CMP] Mock navigation bar options are not applied.'); },
    addAccessoryButton: async ({accessibilityLabel}) => { console.info(`[CMP] Mock accessory button added: ${accessibilityLabel}`); },
    removeAccessoryButton: async () => { console.info('[CMP] Mock accessory button removed.'); },
    // mock 호스트에는 내비게이션 바가 없어서 클릭 이벤트가 생기지 않는다.
    subscribeAccessoryButton: () => () => {},
  },
};
