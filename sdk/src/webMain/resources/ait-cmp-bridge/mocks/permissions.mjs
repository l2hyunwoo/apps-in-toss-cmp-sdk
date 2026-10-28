class PermissionError extends Error {}
// Every mock permission is allowed so the protected mock functions always run.
const status = async () => 'allowed';

export default {
  PermissionError,
  getPermission: status,
  requestPermission: status,
  openPermissionDialog: status,
};
