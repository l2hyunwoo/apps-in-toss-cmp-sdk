package io.github.l2hyunwoo.ait

sealed interface GameProfile {
    data class Found(val nickname: String, val profileImageUri: String) : GameProfile
    data object NotFound : GameProfile
}
enum class LeaderboardSubmitResult { SUCCESS, LEADERBOARD_NOT_FOUND, PROFILE_NOT_FOUND, UNPARSABLE_SCORE }
