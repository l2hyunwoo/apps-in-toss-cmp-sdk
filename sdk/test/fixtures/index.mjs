import host from './host.mjs';
import auth from './auth.mjs';
import user from './user.mjs';
import share from './share.mjs';
import screen from './screen.mjs';
import navigationBar from './navigationBar.mjs';
import storage from './storage.mjs';
import device from './device.mjs';
import media from './media.mjs';
import contacts from './contacts.mjs';
import location from './location.mjs';
import permissions from './permissions.mjs';
import clipboard from './clipboard.mjs';
import environment from './environment.mjs';
import game from './game.mjs';
import iap from './iap.mjs';
import tossPay from './tossPay.mjs';
import ads from './ads.mjs';
import promotion from './promotion.mjs';
import analytics from './analytics.mjs';
import review from './review.mjs';
import notification from './notification.mjs';
import file from './file.mjs';
import migration from './migration.mjs';

// bridge-methods.test.mjs용 feature fixture다. 형태:
//   calls[method] = {payload, invalid?, capability, sdk: settle => namespaces, expect?, run?}
//   streams[method] = {payload, run: async ctx => {}}
//   absentInMin = 최소 SDK(3.0.1)에 없는 [capability, ...]
// `settle(value)`는 성공 확인에서는 값을, 늦은 결과 확인에서는 끝나지 않는 promise를 반환한다.
export const FIXTURES = [host, auth, user, share, screen, navigationBar, storage, device, media, contacts, location, permissions, clipboard, environment, game, iap, tossPay, ads, promotion, analytics, review, notification, file, migration];
