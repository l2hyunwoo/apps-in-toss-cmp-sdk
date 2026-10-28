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

// 각 mock은 공식 SDK namespace 객체를 export하고, mock.mjs가 이를 병합한다.
export const MOCKS = [host, auth, user, share, screen, navigationBar, storage, device, media, contacts, location, permissions, clipboard, environment, game, iap, tossPay, ads, promotion, analytics, review, notification, file, migration];
