import { guarded } from './permissions.mjs';

export default ({sdk, requireCapability}) => ({
  capabilities: {
    CONTACTS: () => typeof sdk.Device?.getContacts === 'function' && typeof sdk.Device.getContacts.getPermission === 'function',
  },
  calls: {
    contactsGet: ({size, offset, contains}) => {
      if (!Number.isInteger(size) || size <= 0) throw new TypeError('size must be a positive integer');
      if (!Number.isInteger(offset) || offset < 0) throw new TypeError('offset must be a non-negative integer');
      if (contains !== undefined && typeof contains !== 'string') throw new TypeError('contains must be a string');
      const options = contains === undefined ? {size, offset} : {size, offset, query: {contains}};
      return () => { requireCapability('CONTACTS'); return guarded(sdk, sdk.Device.getContacts, () => sdk.Device.getContacts(options)); };
    },
  },
});
