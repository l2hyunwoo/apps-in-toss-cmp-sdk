const CONTACTS = [{name: '김모의', phoneNumber: '010-0000-0001'}, {name: '이모의', phoneNumber: '010-0000-0002'}, {name: '박모의', phoneNumber: '010-0000-0003'}];

export default {
  Device: {
    getContacts: Object.assign(async ({size, offset, query}) => {
      const matched = CONTACTS.filter(c => !query?.contains || c.name.includes(query.contains));
      const result = matched.slice(offset, offset + size);
      const done = offset + size >= matched.length;
      return {result, nextOffset: done ? null : offset + size, done};
    }, {getPermission: async () => 'allowed', openPermissionDialog: async () => 'allowed'}),
  },
};
