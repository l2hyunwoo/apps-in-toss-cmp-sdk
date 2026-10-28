export default {
  User: {getAnonymousKey: Object.assign(async () => ({type:'HASH',hash:'mock-local-user'}), {isSupported:()=>true})},
};
