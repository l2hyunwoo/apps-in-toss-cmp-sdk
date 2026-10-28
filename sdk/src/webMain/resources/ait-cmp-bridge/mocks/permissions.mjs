class PermissionError extends Error {}
// 보호된 mock 함수가 항상 실행되도록 모든 mock 권한을 허용한다.
const status = async () => 'allowed';

export default {
  PermissionError,
  getPermission: status,
  requestPermission: status,
  openPermissionDialog: status,
};
