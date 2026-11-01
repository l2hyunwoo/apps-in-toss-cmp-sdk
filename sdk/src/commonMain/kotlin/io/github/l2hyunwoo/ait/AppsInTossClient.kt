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

/** UI 호스트마다 client를 하나씩 둔다. 메서드와 close()는 호스트 UI 스레드에서 호출한다. */
public class AppsInTossClient(private val transport: AppsInTossTransport, private val timeoutMillis: Long = 60_000) {
    init {
        require(timeoutMillis > 0)
    }
    public val host: RuntimeHost get() = transport.host
    private var closed = false
    private val pending = mutableSetOf<() -> Unit>()
    public fun supports(capability: Capability): Boolean = !closed && transport.supports(capability)

    public val auth: AuthApi = AuthApi(this)
    public val user: UserApi = UserApi(this)
    public val share: ShareApi = ShareApi(this)
    public val screen: ScreenApi = ScreenApi(this)
    public val navigationBar: NavigationBarApi = NavigationBarApi(this)
    public val storage: StorageApi = StorageApi(this)
    public val device: DeviceApi = DeviceApi(this)
    public val media: MediaApi = MediaApi(this)
    public val contacts: ContactsApi = ContactsApi(this)
    public val location: LocationApi = LocationApi(this)
    public val permissions: PermissionsApi = PermissionsApi(this)
    public val clipboard: ClipboardApi = ClipboardApi(this)
    public val environment: EnvironmentApi = EnvironmentApi(this)
    public val game: GameApi = GameApi(this)
    public val iap: IapApi = IapApi(this)
    public val tossPay: TossPayApi = TossPayApi(this)
    public val ads: AdsApi = AdsApi(this)
    public val promotion: PromotionApi = PromotionApi(this)
    public val analytics: AnalyticsApi = AnalyticsApi(this)
    public val review: ReviewApi = ReviewApi(this)
    public val notification: NotificationApi = NotificationApi(this)
    public val file: FileApi = FileApi(this)
    public val migration: MigrationApi = MigrationApi(this)

    /** 뒤로 가기, 홈, safe-area 이벤트다. [Capability.HOST_EVENTS]가 필요하다. */
    public val events: Flow<HostEvent> = stream("hostEvents").map { event ->
        decode("host event") { decodeEvent(event) }
    }

    public fun close() {
        if (closed) return
        closed = true
        pending.toList().forEach { it() }
        pending.clear()
    }

    internal fun checkAvailable() {
        if (closed) throw AppsInTossException(ErrorCode.HOST_UNAVAILABLE, "Client closed")
        if (host == RuntimeHost.BROWSER) throw AppsInTossException(ErrorCode.UNSUPPORTED, "Apps in Toss host is unavailable")
    }

    /** 일회성 bridge 요청을 보내고 원본 JSON 결과를 반환한다. */
    internal suspend fun call(method: String, payload: JsonObject = EMPTY_PAYLOAD): String = awaitResponse { success, failure -> transport.call(method, payload.toString(), success, failure) }

    /**
     * bridge stream을 연다. 호스트 질문(`replyId`가 있는 이벤트)은 [answer]로 전달하고, 응답에 실패하면
     * `null`로 회신한다. bridge는 이 값을 공식 기본값으로 처리한다.
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

/** 모든 디코딩 실패를 [ErrorCode.INVALID_RESPONSE]로 바꾼다. */
internal inline fun <T> decode(name: String, block: () -> T): T = try {
    block()
} catch (_: Exception) {
    throw AppsInTossException(ErrorCode.INVALID_RESPONSE, "Invalid $name response")
}

internal fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.also { require(it.isString) }.content
