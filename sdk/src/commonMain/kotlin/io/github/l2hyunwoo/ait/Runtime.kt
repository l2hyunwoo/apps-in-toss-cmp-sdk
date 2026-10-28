package io.github.l2hyunwoo.ait

enum class RuntimeHost { APPS_IN_TOSS, SANDBOX, MOCK, BROWSER }

/** 현재 호스트가 실행할 수 있는 기능 묶음이다. 각 API는 필요한 capability를 문서에 적는다. */
enum class Capability {
    HOST_EVENTS,

    // auth
    TOSS_LOGIN,
    TOSS_LOGIN_INTEGRATION,
    TOSS_SIGN,

    // user
    USER_ID,
    USER_KEY_AUTH_CODE,
    CONSENTED_DATA,
    DECLARED_AGE_RANGE,

    // share
    SHARE,
    SHARE_LINK,

    // screen, navigationBar
    CLOSE,
    SCREEN_AWAKE,
    SECURE_SCREEN,
    IOS_SWIPE_BACK,
    ORIENTATION,
    NAVIGATION_BAR,

    // storage
    STORAGE,
    STORAGE_BATCH,

    // device
    HAPTIC,
    DEVICE_INFO,
    OPEN_URL,

    // media, contacts, location, permissions
    PHOTOS,
    ALBUM,
    CAMERA,
    CONTACTS,
    LOCATION,
    LOCATION_UPDATES,
    PERMISSIONS,

    // clipboard
    CLIPBOARD,

    // environment
    ENVIRONMENT_INFO,
    NETWORK_STATUS,
    SERVER_TIME,

    // game
    GAME_PROFILE,
    LEADERBOARD,
    GAME_INFO_OVERLAY,

    // ads
    FULL_SCREEN_AD,
    ADMOB,
    BANNER_AD,

    // iap, tossPay
    IAP,
    IAP_SUBSCRIPTION,
    TOSS_PAY,
    TOSS_PAY_SUBSCRIPTION,

    // promotion, analytics, review, notification, file, migration
    PROMOTION_REWARD,
    ANALYTICS,
    REVIEW,
    NOTIFICATION_AGREEMENT,
    PDF_VIEWER,
    SAVE_FILE,
    ORIGIN_STORAGE_MIGRATION,
}
