# Kotlin SDK

앱인토스 호스트 기능을 Kotlin JS·Wasm에서 호출하는 라이브러리입니다. 공식 웹 SDK 호출은 SDK 내부 [JS 리소스](src/webMain/resources/ait-cmp-bridge)가 담당하며, Kotlin에는 JSON 기반 연결 계약만 노출합니다.

## 제공 API

| API | 역할 |
| --- | --- |
| `createAppsInTossClient()` | 현재 JS 브리지에 연결한 클라이언트 생성 |
| `host`, `supports(capability)` | 실행 환경·기능 지원 여부 확인 |
| `events` | 뒤로 가기·홈·안전 영역 이벤트 Flow(`HOST_EVENTS`) |
| `close()` | 진행 중 요청·이벤트 연결 정리 |

기능은 공식 SDK 도메인별 하위 객체로 나뉩니다. 메서드 이름은 공식 API 이름을 그대로 따르므로 동작·인자·결과의 세부는 [공식 SDK 레퍼런스](https://developers-apps-in-toss.toss.im/documentation/sdk/domains-api.md)에서 같은 이름으로 찾습니다. 호출 전 `supports()`로 표의 `Capability`를 확인합니다. 지원하지 않으면 `ErrorCode.UNSUPPORTED`로 실패합니다.

| 하위 객체 | 메서드 | 필요한 `Capability` |
| --- | --- | --- |
| `client.auth` 토스 로그인·서명 | `client.auth.login()` | `TOSS_LOGIN` |
|  | `client.auth.isIntegrated()` | `TOSS_LOGIN_INTEGRATION` |
|  | `client.auth.sign()` | `TOSS_SIGN` |
| `client.user` 익명 키·동의 데이터·연령대 | `client.user.getAnonymousKey()` | `USER_ID` |
|  | `client.user.createAnonymousKeyAuthCode()` | `USER_KEY_AUTH_CODE` |
|  | `client.user.getConsentedData()` | `CONSENTED_DATA` |
|  | `client.user.getDeclaredAgeRange()` | `DECLARED_AGE_RANGE` |
| `client.share` 공유 메시지·링크 | `client.share.sendMessage()` | `SHARE` |
|  | `client.share.createLink()` | `SHARE_LINK` |
| `client.screen` 화면 닫기·켜짐 유지·캡처 방지·방향 | `client.screen.close()` | `CLOSE` |
|  | `client.screen.setAwakeMode()` | `SCREEN_AWAKE` |
|  | `client.screen.setSecure()` | `SECURE_SCREEN` |
|  | `client.screen.setIosSwipeBack()` | `IOS_SWIPE_BACK` |
|  | `client.screen.setOrientation()` | `ORIENTATION` |
| `client.navigationBar` 내비게이션 바 설정 | `client.navigationBar.setOptions()` | `NAVIGATION_BAR` |
| `client.storage` 토스앱 로컬 저장소 | `client.storage.getItem()` | `STORAGE` |
|  | `client.storage.getItems()` | `STORAGE_BATCH` |
|  | `client.storage.setItem()` | `STORAGE` |
|  | `client.storage.setItems()` | `STORAGE_BATCH` |
|  | `client.storage.removeItem()` | `STORAGE` |
|  | `client.storage.clearItems()` | `STORAGE` |
| `client.device` 햅틱·언어·OS·외부 URL | `client.device.triggerHaptic()` | `HAPTIC` |
|  | `client.device.locale()` | `DEVICE_INFO` |
|  | `client.device.os()` | `DEVICE_INFO` |
|  | `client.device.openURL()` | `OPEN_URL` |
| `client.media` 사진·앨범·카메라 | `client.media.getPhotos()` | `PHOTOS` |
|  | `client.media.getAlbumItems()` | `ALBUM` |
|  | `client.media.openCamera()` | `CAMERA` |
| `client.contacts` 연락처 | `client.contacts.getContacts()` | `CONTACTS` |
| `client.location` 현재 위치·위치 구독 | `client.location.getLocation()` | `LOCATION` |
|  | `client.location.subscribeLocation()` | `LOCATION_UPDATES` |
| `client.permissions` 권한 조회·요청 | `client.permissions.getPermission()` | `PERMISSIONS` |
|  | `client.permissions.requestPermission()` | `PERMISSIONS` |
|  | `client.permissions.openPermissionDialog()` | `PERMISSIONS` |
| `client.clipboard` 클립보드 | `client.clipboard.getText()` | `CLIPBOARD` |
|  | `client.clipboard.setText()` | `CLIPBOARD` |
| `client.environment` 기기·배포·앱 버전·네트워크·서버 시간 | `client.environment.deviceId()` | `ENVIRONMENT_INFO` |
|  | `client.environment.groupId()` | `ENVIRONMENT_INFO` |
|  | `client.environment.environment()` | `ENVIRONMENT_INFO` |
|  | `client.environment.tossAppVersion()` | `ENVIRONMENT_INFO` |
|  | `client.environment.deploymentId()` | `ENVIRONMENT_INFO` |
|  | `client.environment.initialURL()` | `ENVIRONMENT_INFO` |
|  | `client.environment.getNetworkStatus()` | `NETWORK_STATUS` |
|  | `client.environment.getServerTime()` | `SERVER_TIME` |
|  | `client.environment.isMinVersionSupported()` | `ENVIRONMENT_INFO` |
| `client.game` 게임센터 프로필·리더보드 | `client.game.getUserProfile()` | `GAME_PROFILE` |
|  | `client.game.setLeaderboardScore()` | `LEADERBOARD` |
|  | `client.game.openLeaderboard()` | `LEADERBOARD` |
|  | `client.game.subscribeInfoOverlayHidden()` | `GAME_INFO_OVERLAY` |
| `client.iap` 인앱 결제 | `client.iap.createOneTimePurchaseOrder()` | `IAP` |
|  | `client.iap.createSubscriptionPurchaseOrder()` | `IAP_SUBSCRIPTION` |
|  | `client.iap.getProductItemList()` | `IAP` |
|  | `client.iap.getPendingOrders()` | `IAP` |
|  | `client.iap.getCompletedOrRefundedOrders()` | `IAP` |
|  | `client.iap.completeProductGrant()` | `IAP` |
|  | `client.iap.getSubscriptionInfo()` | `IAP_SUBSCRIPTION` |
| `client.tossPay` 토스페이 | `client.tossPay.authorize()` | `TOSS_PAY` |
|  | `client.tossPay.authorizeSubscription()` | `TOSS_PAY_SUBSCRIPTION` |
| `client.ads` 전면 광고·AdMob·배너 | `client.ads.loadFullScreenAd()` | `FULL_SCREEN_AD` |
|  | `client.ads.showFullScreenAd()` | `FULL_SCREEN_AD` |
|  | `client.ads.admob.loadAppsInTossAdMob()` | `ADMOB` |
|  | `client.ads.admob.showAppsInTossAdMob()` | `ADMOB` |
|  | `client.ads.admob.isAppsInTossAdMobLoaded()` | `ADMOB` |
|  | `client.ads.tossAds.initialize()` | `BANNER_AD` |
|  | `client.ads.tossAds.attachBanner()` | `BANNER_AD` |
|  | `client.ads.tossAds.destroy()` | `BANNER_AD` |
|  | `client.ads.tossAds.destroyAll()` | `BANNER_AD` |
| `client.promotion` 프로모션 리워드 | `client.promotion.grantReward()` | `PROMOTION_REWARD` |
| `client.analytics` 로그 기록 | `client.analytics.log()` | `ANALYTICS` |
|  | `client.analytics.screen()` | `ANALYTICS` |
|  | `client.analytics.impression()` | `ANALYTICS` |
|  | `client.analytics.click()` | `ANALYTICS` |
| `client.review` 리뷰 요청 | `client.review.request()` | `REVIEW` |
| `client.notification` 알림 동의 | `client.notification.requestAgreement()` | `NOTIFICATION_AGREEMENT` |
| `client.file` PDF 보기·파일 저장 | `client.file.openPDFViewer()` | `PDF_VIEWER` |
|  | `client.file.saveBase64()` | `SAVE_FILE` |
| `client.migration` origin 저장소 이전 | `client.migration.getOriginStorage()` | `ORIGIN_STORAGE_MIGRATION` |

### 호출 방식

- 공식 SDK의 Promise API는 `suspend fun`입니다. 사용자가 닫을 때까지 끝나지 않는 API(`file.openPDFViewer`, `notification.requestAgreement`, 광고 로드)도 `suspend fun`이며, 코루틴을 취소하면 공식 정리 함수를 호출합니다.
- 이벤트가 여러 번 오는 API(`location.subscribeLocation`, `ads.showFullScreenAd`, `game.subscribeInfoOverlayHidden`)는 cold `Flow`입니다. 종료 이벤트 뒤에 완료하고, 수집을 취소하면 공식 구독을 해제합니다.
- 공식 문서에 정의된 업무 결과는 예외가 아니라 결과 타입입니다. 권한이 필요한 API(`media`, `contacts`, `location`, `clipboard`)는 `PermissionResult.Granted` 또는 `PermissionResult.Denied`를 반환하며, 권한이 거부된 상태면 공식 함수를 호출하지 않습니다.
- 이미지와 파일 데이터는 공식 SDK가 주는 base64 문자열을 그대로 전달합니다.
- 배너 광고(`ads.tossAds.attachBanner`)는 Compose 캔버스 위에 `data-ait-banner` HTML 요소를 CSS px 좌표로 배치합니다. 반환된 `BannerHandle`의 `updateBounds()`로 위치를 갱신하고 `destroy()`로 해제합니다.

### 결제·리워드

- 인앱 결제 주문(`iap.createOneTimePurchaseOrder`, `iap.createSubscriptionPurchaseOrder`)은 `grant` 람다로 지급 여부를 묻습니다. `true`를 반환할 때만 지급이 확정되고, 예외·취소·`close()`이면 `false`로 처리됩니다. 지급 판단은 서비스 서버에서 검증한 뒤 내려야 합니다.
- 지급하지 못한 주문은 `iap.getPendingOrders()`로 찾아 `iap.completeProductGrant()`로 마무리합니다.
- 공식 SDK 3.0.1의 `IAP.getCompletedOrRefundedOrders`는 페이지 키를 무시해 항상 첫 페이지를 반환합니다. `hasNext`가 참인 동안 반복하는 코드는 이 버전에서 끝나지 않으므로 반복 횟수를 제한합니다.
- `promotion.grantReward`와 `tossPay`의 결과도 서비스 서버에서 다시 확인합니다.

### 연결하지 않은 공식 API

공식 SDK의 deprecated API와 호환 별칭은 대체 API로만 제공합니다. 공식 API 레퍼런스에 없는 `partner`, `tdsEvent`, `Viral`, `Promotion.openContactsInvite`, `onVisibilityChangedByTransparentServiceWeb`, `TossPay.checkoutPayment`, `TossPay.requestTossPayPaysBilling`은 제공하지 않습니다. 전체 분류는 [커버리지 매니페스트](test/coverage)에 있습니다.

### 1.0.0-alpha04에서 이전

| 이전 | 현재 |
| --- | --- |
| `requestTossLogin()` | `auth.login()` |
| `getAnonymousKey()` | `user.getAnonymousKey()` |
| `shareText(text)` | `share.sendMessage(text)` |
| `closeScreen()` | `screen.close()` |
| `generateHaptic(type)` | `device.triggerHaptic(type)` |
| `getStorageItem`·`setStorageItem`·`removeStorageItem` | `storage.getItem`·`setItem`·`removeItem` |
| `getGameProfile()` | `game.getUserProfile()` |
| `submitLeaderboardScore(score)`, `openLeaderboard()` | `game.setLeaderboardScore(score)`, `game.openLeaderboard()` |

`AppsInTossTransport`는 `call`과 `stream` 두 메서드로 바뀌었습니다. 직접 구현한 transport가 있으면 두 메서드로 옮겨야 합니다.

`RuntimeHost`, `Capability`, `HostEvent`, `ErrorCode`, `AppsInTossException`으로 상태와 실패를 구분합니다. 토스앱 버전이 최소 지원 버전보다 낮아 공식 SDK가 결과를 돌려주지 않으면 `ErrorCode.UNSUPPORTED`로 실패합니다. 리더보드는 콘솔 설정이 없으면 `LEADERBOARD_NOT_FOUND`를 반환합니다. 익명 키는 인증 증명이 아니며, 로그인 인가 코드의 서버 교환은 서비스가 구현해야 합니다.

## 연결과 수명

Maven Central의 `io.github.l2hyunwoo:apps-in-toss-sdk`를 참조합니다. 생성 CLI는 SDK 의존성과 웹 빌드 연결을 함께 설정합니다.

웹 빌드가 SDK의 `web-resources` JAR를 추출하고 CMP 진입점보다 먼저 JS 브리지를 초기화합니다. 별도 npm 브리지 설치는 필요하지 않습니다. UI 호스트마다 클라이언트 하나를 소유합니다. API와 `close()`는 호스트 UI 스레드에서 호출합니다. 호스트 해제 시 `close()`를 호출해야 합니다. 연결 예제는 [앱인토스 진입점](../sample/appsInTossApp/src/webMain/kotlin/Main.kt)에 있습니다.

`HostEvent.SafeArea`의 `top`은 상태바 여백입니다. 기본 불투명 내비게이션 바를 쓰면 WebView가 이미 내비게이션 바 아래에서 시작하므로 `top`을 적용하지 않습니다. 하단·좌우 값만 적용합니다.

로그인은 한 번에 하나만 처리합니다. 단발 요청의 기본 제한 시간은 60초입니다. 코루틴 취소·제한 시간 초과 시 늦게 도착한 결과를 무시하고 연결을 해제합니다. 호스트의 로그인 창 자체를 닫지는 않습니다. 미지원 기능·호스트 부재·잘못된 응답을 구분하며 원본 오류의 민감한 내용을 노출하지 않습니다.

JS·Wasm이 호스트 연동 대상이며, JVM 타깃은 계약 테스트용입니다. Android·iOS 앱은 [공통 UI](../sample/shared-ui/README.md)와 [서비스 로직](../sample/shared-core/README.md)을 재사용합니다.
