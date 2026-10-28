package io.github.l2hyunwoo.ait

enum class HapticType(internal val wire: String) {
    TICK_WEAK("tickWeak"),
    TAP("tap"),
    TICK_MEDIUM("tickMedium"),
    SOFT_MEDIUM("softMedium"),
    BASIC_WEAK("basicWeak"),
    BASIC_MEDIUM("basicMedium"),
    SUCCESS("success"),
    ERROR("error"),
    WIGGLE("wiggle"),
    CONFETTI("confetti"),
}
