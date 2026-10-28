package io.github.l2hyunwoo.ait

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*

class GameApi internal constructor(private val client: AppsInTossClient) {
    /** [Capability.GAME_PROFILE]가 필요하다. */
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

    /** [Capability.LEADERBOARD]가 필요하다. */
    suspend fun setLeaderboardScore(score: Double): LeaderboardSubmitResult {
        require(score.isFinite()) { "Score must be finite" }
        val result = client.call("leaderboardSubmit", buildJsonObject { put("score", score.toString()) })
        return decode("leaderboard") {
            LeaderboardSubmitResult.valueOf(Json.parseToJsonElement(result).jsonObject.string("statusCode"))
        }
    }

    /** [Capability.LEADERBOARD]가 필요하다. */
    suspend fun openLeaderboard() {
        client.call("leaderboardOpen")
    }

    /**
     * 게임 정보 overlay가 완전히 사라질 때마다 방출한다. 사라지기 전에 수집해야 하며, 놓친 이벤트는 다시 전달하지 않는다.
     * 스스로 완료되지 않는다. [Capability.GAME_INFO_OVERLAY]가 필요하다.
     */
    fun subscribeInfoOverlayHidden(): Flow<Unit> = client.stream("gameInfoOverlayHidden").map { }
}

sealed interface GameProfile {
    data class Found(val nickname: String, val profileImageUri: String) : GameProfile
    data object NotFound : GameProfile
}
enum class LeaderboardSubmitResult { SUCCESS, LEADERBOARD_NOT_FOUND, PROFILE_NOT_FOUND, UNPARSABLE_SCORE }
