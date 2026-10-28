const page = {result: [{name: 'n', phoneNumber: '010'}], nextOffset: null, done: true};
const contacts = settle => ({Device: {getContacts: Object.assign(async () => settle(page), {getPermission: async () => 'allowed', openPermissionDialog: async () => 'allowed'})}});

export default {
  calls: {
    contactsGet: {payload: {size: 10, offset: 0, contains: '김'}, invalid: [{size: 0, offset: 0}, {size: 10, offset: -1}, {size: 10, offset: 0, contains: 1}], capability: 'CONTACTS', sdk: contacts, expect: {permission: 'allowed', value: page}},
  },
  streams: {},
};
