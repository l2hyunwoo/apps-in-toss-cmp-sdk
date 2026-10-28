package io.github.l2hyunwoo.ait

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*

class GameApi internal constructor(private val client: AppsInTossClient) {
    /** Requires [Capability.GAME_PROFILE]. */
    suspend fun getUserProfile(): GameProfile {
        val result = client.call("gameProfile")
        return decode("game profile") {
            val obj = Json.parseToJsonElement(result).jsonObject
            when (obj.string("statusCode")) {
                "SUCCESS" -> GameProfile.Found(obj.string("nickname"), obj.string("profileImageUri"))
                "PROFILE_NOT_FOUND" -> GameProfile.NotFound
                else -> error("Unknown profile status")
            }
        }
    }

    /** Requires [Capability.LEADERBOARD]. */
    suspend fun setLeaderboardScore(score: Double): LeaderboardSubmitResult {
        require(score.isFinite()) { "Score must be finite" }
        val result = client.call("leaderboardSubmit", buildJsonObject { put("score", score.toString()) })
        return decode("leaderboard") {
            LeaderboardSubmitResult.valueOf(Json.parseToJsonElement(result).jsonObject.string("statusCode"))
        }
    }

    /** Requires [Capability.LEADERBOARD]. */
    suspend fun openLeaderboard() {
        client.call("leaderboardOpen")
    }

    /**
     * Emits each time the game info overlay fully disappears. Collect before it hides; a missed event is not redelivered.
     * Never completes on its own. Requires [Capability.GAME_INFO_OVERLAY].
     */
    fun subscribeInfoOverlayHidden(): Flow<Unit> = client.stream("gameInfoOverlayHidden").map { }
}

sealed interface GameProfile {
    data class Found(val nickname: String, val profileImageUri: String) : GameProfile
    data object NotFound : GameProfile
}
enum class LeaderboardSubmitResult { SUCCESS, LEADERBOARD_NOT_FOUND, PROFILE_NOT_FOUND, UNPARSABLE_SCORE }
