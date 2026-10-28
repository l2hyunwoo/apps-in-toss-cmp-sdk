package io.github.l2hyunwoo.ait

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*

enum class RuntimeHost { APPS_IN_TOSS, SANDBOX, MOCK, BROWSER }
enum class Capability { TOSS_LOGIN, USER_ID, SHARE, CLOSE, HAPTIC, STORAGE, GAME_PROFILE, LEADERBOARD }
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
sealed interface GameProfile {
    data class Found(val nickname: String, val profileImageUri: String) : GameProfile
    data object NotFound : GameProfile
}
enum class LeaderboardSubmitResult { SUCCESS, LEADERBOARD_NOT_FOUND, PROFILE_NOT_FOUND, UNPARSABLE_SCORE }
data class TossLoginAuthorization(val authorizationCode: String, val referrer: String)
data class SafeAreaInsets(val top: Double, val left: Double, val right: Double, val bottom: Double)
sealed interface HostEvent {
    data object Back : HostEvent
    data object Home : HostEvent
    data class SafeArea(val insets: SafeAreaInsets) : HostEvent
}
enum class ErrorCode { UNSUPPORTED, PERMISSION_DENIED, USER_CANCELLED, TIMEOUT, HOST_UNAVAILABLE, INVALID_RESPONSE, SDK_FAILURE, IN_PROGRESS }
class AppsInTossException(val code: ErrorCode, message: String) : Exception(message)

interface AppsInTossTransport {
    val host: RuntimeHost
    fun supports(capability: Capability): Boolean = host != RuntimeHost.BROWSER
    fun anonymousKey(success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
    fun login(success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
    fun share(text: String, success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
    fun closeScreen(success: (String) -> Unit, failure: (String) -> Unit): () -> Unit
    fun subscribe(listener: (String) -> Unit, failure: (String) -> Unit): () -> Unit

    /** Generic path for bridge methods added after the original transport contract. */
    fun call(method: String, payload: String, success: (String) -> Unit, failure: (String) -> Unit): () -> Unit {
        failure("""{"code":"UNSUPPORTED"}""")
        return {}
    }
}

/** Own one client per UI host. Call methods and close() on the host UI thread. */
class AppsInTossClient(private val transport: AppsInTossTransport, private val timeoutMillis: Long = 60_000) {
    init {
        require(timeoutMillis > 0)
    }
    val host: RuntimeHost get() = transport.host
    private var closed = false
    private val loginMutex = Mutex()
    private val pending = mutableSetOf<() -> Unit>()
    fun supports(capability: Capability): Boolean = !closed && transport.supports(capability)

    suspend fun requestTossLogin(): TossLoginAuthorization {
        checkAvailable()
        if (!loginMutex.tryLock()) throw AppsInTossException(ErrorCode.IN_PROGRESS, "A login is already in progress")
        try {
            val result = awaitResponse(transport::login)
            return try {
                val obj = Json.parseToJsonElement(result).jsonObject
                val codeValue = obj.getValue("authorizationCode").jsonPrimitive
                val referrerValue = obj.getValue("referrer").jsonPrimitive
                require(codeValue.isString && referrerValue.isString)
                val code = codeValue.content
                val referrer = referrerValue.content
                require(code.isNotBlank() && referrer in setOf("DEFAULT", "SANDBOX"))
                TossLoginAuthorization(code, referrer)
            } catch (_: Exception) {
                throw AppsInTossException(ErrorCode.INVALID_RESPONSE, "Invalid login response")
            }
        } finally {
            loginMutex.unlock()
        }
    }

    suspend fun getAnonymousKey(): String {
        val result = awaitResponse(transport::anonymousKey)
        return try {
            val obj = Json.parseToJsonElement(result).jsonObject
            val type = obj.getValue("type").jsonPrimitive
            val hash = obj.getValue("hash").jsonPrimitive
            require(type.isString && type.content == "HASH" && hash.isString && hash.content.isNotBlank())
            hash.content
        } catch (_: Exception) {
            throw AppsInTossException(ErrorCode.INVALID_RESPONSE, "Invalid user key response")
        }
    }

    suspend fun shareText(text: String) {
        require(text.isNotBlank()) { "Share text cannot be blank" }
        awaitResponse { success, failure -> transport.share(text, success, failure) }
    }
    suspend fun closeScreen() {
        awaitResponse(transport::closeScreen)
    }

    suspend fun generateHaptic(type: HapticType) {
        call("haptic", buildJsonObject { put("type", type.wire) })
    }

    /** Reads the host's persistent storage; returns null when the key is absent. */
    suspend fun getStorageItem(key: String): String? {
        val result = call("storageGet", storagePayload(key))
        return decode("storage") {
            val value = Json.parseToJsonElement(result)
            if (value is JsonNull) null else value.jsonPrimitive.also { require(it.isString) }.content
        }
    }

    suspend fun setStorageItem(key: String, value: String) {
        call("storageSet", storagePayload(key) { put("value", value) })
    }

    suspend fun removeStorageItem(key: String) {
        call("storageRemove", storagePayload(key))
    }

    suspend fun getGameProfile(): GameProfile {
        val result = call("gameProfile", JsonObject(emptyMap()))
        return decode("game profile") {
            val obj = Json.parseToJsonElement(result).jsonObject
            when (obj.getValue("statusCode").jsonPrimitive.content) {
                "SUCCESS" -> GameProfile.Found(obj.string("nickname"), obj.string("profileImageUri"))
                "PROFILE_NOT_FOUND" -> GameProfile.NotFound
                else -> error("Unknown profile status")
            }
        }
    }

    suspend fun submitLeaderboardScore(score: Double): LeaderboardSubmitResult {
        require(score.isFinite()) { "Score must be finite" }
        val result = call("leaderboardSubmit", buildJsonObject { put("score", score.toString()) })
        return decode("leaderboard") {
            val status = Json.parseToJsonElement(result).jsonObject.getValue("statusCode").jsonPrimitive.content
            LeaderboardSubmitResult.valueOf(status)
        }
    }

    suspend fun openLeaderboard() {
        call("leaderboardOpen", JsonObject(emptyMap()))
    }

    private suspend fun call(method: String, payload: JsonObject): String = awaitResponse { success, failure -> transport.call(method, payload.toString(), success, failure) }

    private fun storagePayload(key: String, extra: JsonObjectBuilder.() -> Unit = {}): JsonObject {
        require(key.isNotBlank()) { "Storage key cannot be blank" }
        return buildJsonObject {
            put("key", key)
            extra()
        }
    }

    private inline fun <T> decode(name: String, block: () -> T): T = try {
        block()
    } catch (_: Exception) {
        throw AppsInTossException(ErrorCode.INVALID_RESPONSE, "Invalid $name response")
    }

    val events: Flow<HostEvent> = callbackFlow {
        checkAvailable()
        val cleanup = transport.subscribe({ raw ->
            try {
                trySend(decodeEvent(raw))
            } catch (_: Exception) {
                close(AppsInTossException(ErrorCode.INVALID_RESPONSE, "Invalid host event"))
            }
        }, { close(decodeError(it)) })
        val stop: () -> Unit = { close(AppsInTossException(ErrorCode.HOST_UNAVAILABLE, "Client closed")) }
        pending += stop
        awaitClose {
            pending -= stop
            cleanup()
        }
    }

    fun close() {
        if (closed) return
        closed = true
        pending.toList().forEach { it() }
        pending.clear()
    }

    private fun checkAvailable() {
        if (closed) throw AppsInTossException(ErrorCode.HOST_UNAVAILABLE, "Client closed")
        if (host == RuntimeHost.BROWSER) throw AppsInTossException(ErrorCode.UNSUPPORTED, "Apps in Toss host is unavailable")
    }

    private suspend fun awaitResponse(start: ((String) -> Unit, (String) -> Unit) -> (() -> Unit)): String {
        checkAvailable()
        try {
            return withTimeoutOrNull(timeoutMillis) {
                suspendCancellableCoroutine { continuation ->
                    var cleanup: (() -> Unit)? = null
                    var completed = false
                    lateinit var stop: () -> Unit
                    val finish: () -> Unit = {
                        pending -= stop
                        completed = true
                        val release = cleanup
                        cleanup = null
                        release?.invoke()
                    }
                    stop = {
                        if (continuation.isActive) {
                            finish()
                            continuation.resumeWithException(AppsInTossException(ErrorCode.HOST_UNAVAILABLE, "Client closed"))
                        }
                    }
                    pending += stop
                    continuation.invokeOnCancellation { finish() }
                    try {
                        cleanup = start({ value ->
                            if (continuation.isActive) {
                                finish()
                                continuation.resume(value)
                            }
                        }, { error ->
                            if (continuation.isActive) {
                                finish()
                                continuation.resumeWithException(decodeError(error))
                            }
                        })
                        if (completed || !continuation.isActive) finish()
                    } catch (e: Exception) {
                        finish()
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }
            } ?: throw AppsInTossException(ErrorCode.TIMEOUT, "Host request timed out; host UI may still be open")
        } catch (e: CancellationException) {
            throw e
        }
    }
}

private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.also { require(it.isString) }.content

private fun decodeError(raw: String): AppsInTossException {
    val code = runCatching { Json.parseToJsonElement(raw).jsonObject["code"]?.jsonPrimitive?.content }.getOrNull()
    val known = ErrorCode.entries.firstOrNull { it.name == code } ?: ErrorCode.SDK_FAILURE
    return AppsInTossException(known, "Apps in Toss request failed ($known)")
}

private fun decodeEvent(raw: String): HostEvent {
    val obj = Json.parseToJsonElement(raw).jsonObject
    return when (obj.getValue("type").jsonPrimitive.content) {
        "back" -> HostEvent.Back

        "home" -> HostEvent.Home

        "safeArea" -> {
            fun inset(key: String): Double {
                val value = obj.getValue(key).jsonPrimitive
                require(!value.isString)
                return value.double.also { require(it.isFinite() && it >= 0) }
            }
            HostEvent.SafeArea(SafeAreaInsets(inset("top"), inset("left"), inset("right"), inset("bottom")))
        }

        else -> error("Unknown host event")
    }
}
