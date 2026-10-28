package io.github.l2hyunwoo.ait

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
}

sealed interface GameProfile {
    data class Found(val nickname: String, val profileImageUri: String) : GameProfile
    data object NotFound : GameProfile
}
enum class LeaderboardSubmitResult { SUCCESS, LEADERBOARD_NOT_FOUND, PROFILE_NOT_FOUND, UNPARSABLE_SCORE }
