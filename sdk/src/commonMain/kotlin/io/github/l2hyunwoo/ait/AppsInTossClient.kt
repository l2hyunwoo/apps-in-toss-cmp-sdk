package io.github.l2hyunwoo.ait

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.*

/** Own one client per UI host. Call methods and close() on the host UI thread. */
class AppsInTossClient(private val transport: AppsInTossTransport, private val timeoutMillis: Long = 60_000) {
    init {
        require(timeoutMillis > 0)
    }
    val host: RuntimeHost get() = transport.host
    private var closed = false
    private val pending = mutableSetOf<() -> Unit>()
    fun supports(capability: Capability): Boolean = !closed && transport.supports(capability)

    val auth = AuthApi(this)
    val user = UserApi(this)
    val share = ShareApi(this)
    val screen = ScreenApi(this)
    val navigationBar = NavigationBarApi(this)
    val storage = StorageApi(this)
    val device = DeviceApi(this)
    val media = MediaApi(this)
    val contacts = ContactsApi(this)
    val location = LocationApi(this)
    val permissions = PermissionsApi(this)
    val clipboard = ClipboardApi(this)
    val environment = EnvironmentApi(this)
    val game = GameApi(this)
    val iap = IapApi(this)
    val tossPay = TossPayApi(this)
    val ads = AdsApi(this)
    val promotion = PromotionApi(this)
    val analytics = AnalyticsApi(this)
    val review = ReviewApi(this)
    val notification = NotificationApi(this)
    val file = FileApi(this)
    val migration = MigrationApi(this)

    /** Back, home and safe-area events. Requires [Capability.HOST_EVENTS]. */
    val events: Flow<HostEvent> = stream("hostEvents").map { event ->
        decode("host event") { decodeEvent(event) }
    }

    fun close() {
        if (closed) return
        closed = true
        pending.toList().forEach { it() }
        pending.clear()
    }

    internal fun checkAvailable() {
        if (closed) throw AppsInTossException(ErrorCode.HOST_UNAVAILABLE, "Client closed")
        if (host == RuntimeHost.BROWSER) throw AppsInTossException(ErrorCode.UNSUPPORTED, "Apps in Toss host is unavailable")
    }

    /** Sends a one-shot bridge request and returns the raw JSON result. */
    internal suspend fun call(method: String, payload: JsonObject = EMPTY_PAYLOAD): String = awaitResponse { success, failure -> transport.call(method, payload.toString(), success, failure) }

    /**
     * Opens a bridge stream. Host questions (events with `replyId`) go to [answer]; a failed answer
     * replies `null`, which the bridge treats as the official fallback value.
     */
    internal fun stream(method: String, payload: JsonObject = EMPTY_PAYLOAD, answer: suspend (JsonObject) -> JsonElement = { JsonNull }): Flow<JsonObject> = callbackFlow {
        checkAvailable()
        val cleanup = transport.stream(method, payload.toString(), { raw ->
            val event = try {
                Json.parseToJsonElement(raw).jsonObject
            } catch (_: Exception) {
                close(AppsInTossException(ErrorCode.INVALID_RESPONSE, "Invalid $method event"))
                return@stream
            }
            val replyId = (event["replyId"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            if (replyId == null) {
                trySend(event)
            } else {
                launch {
                    val value = try {
                        answer(event)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        JsonNull
                    }
                    runCatching {
                        call(
                            "reply",
                            buildJsonObject {
                                put("replyId", replyId)
                                put("value", value)
                            },
                        )
                    }
                }
            }
        }, { close(decodeError(it)) })
        val stop: () -> Unit = { close(AppsInTossException(ErrorCode.HOST_UNAVAILABLE, "Client closed")) }
        pending += stop
        awaitClose {
            pending -= stop
            cleanup()
        }
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

internal val EMPTY_PAYLOAD = JsonObject(emptyMap())

/** Maps any decoding failure to [ErrorCode.INVALID_RESPONSE]. */
internal inline fun <T> decode(name: String, block: () -> T): T = try {
    block()
} catch (_: Exception) {
    throw AppsInTossException(ErrorCode.INVALID_RESPONSE, "Invalid $name response")
}

internal fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.also { require(it.isString) }.content
