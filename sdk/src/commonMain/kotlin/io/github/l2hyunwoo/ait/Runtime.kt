package io.github.l2hyunwoo.ait

enum class RuntimeHost { APPS_IN_TOSS, SANDBOX, MOCK, BROWSER }

/** Feature groups the current host can run. Each API documents the capability it requires. */
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
    TOSS_PAY,

    // promotion, analytics, review, notification, file, migration
    PROMOTION_REWARD,
    ANALYTICS,
    REVIEW,
    NOTIFICATION_AGREEMENT,
    PDF_VIEWER,
    SAVE_FILE,
    ORIGIN_STORAGE_MIGRATION,
}
