package io.github.l2hyunwoo.ait

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

class AdsApi internal constructor(private val client: AppsInTossClient) {
    val admob = AdMobApi(client)
    val tossAds = TossAdsApi(client)

    /** 전면 광고를 미리 불러오고 `loaded` 이벤트 뒤에 반환한다. [Capability.FULL_SCREEN_AD]가 필요하다. */
    suspend fun loadFullScreenAd(adGroupId: String) {
        client.stream("loadFullScreenAd", adGroupPayload(adGroupId)).first { decode("full-screen ad load") { it.string("type") } == "loaded" }
    }

    /**
     * 미리 불러온 전면 광고를 보여 준다. [FullScreenAdEvent.Dismissed]나 [FullScreenAdEvent.FailedToShow] 뒤에 완료되고,
     * 수집을 취소하면 공식 콜백을 해제한다. 보상은 [FullScreenAdEvent.UserEarnedReward]에서만 지급한다.
     * [Capability.FULL_SCREEN_AD]가 필요하다.
     */
    fun showFullScreenAd(adGroupId: String): Flow<FullScreenAdEvent> = client.adEvents("showFullScreenAd", adGroupPayload(adGroupId))
}

/** AdMob용 공식 v2 호환 별칭이다. [AdsApi.loadFullScreenAd]와 [AdsApi.showFullScreenAd]를 우선 쓴다. */
class AdMobApi internal constructor(private val client: AppsInTossClient) {
    /** AdMob 광고를 미리 불러오고 `loaded` 이벤트 뒤에 반환한다. [Capability.ADMOB]가 필요하다. */
    suspend fun loadAppsInTossAdMob(adGroupId: String, referrer: String? = null): AdMobLoadResult {
        val payload = JsonObject(adGroupPayload(adGroupId) + ("referrer" to JsonPrimitive(referrer)))
        val event = client.stream("admobLoad", payload).first { decode("AdMob load") { it.string("type") } == "loaded" }
        return decode("AdMob load") {
            val data = event.getValue("data").jsonObject
            val info = data.getValue("responseInfo").jsonObject
            AdMobLoadResult(
                adGroupId = data.string("adGroupId"),
                adUnitId = data.string("adUnitId"),
                responseInfo = AdMobResponseInfo(
                    adNetworkInfoArray = info.getValue("adNetworkInfoArray").jsonArray.map { it.jsonObject.toAdNetworkInfo() },
                    loadedAdNetworkInfo = info.getValue("loadedAdNetworkInfo").let { if (it is JsonNull) null else it.jsonObject.toAdNetworkInfo() },
                    responseId = info.optionalString("responseId"),
                ),
            )
        }
    }

    /** 미리 불러온 AdMob 광고를 보여 준다. 완료 규칙은 [AdsApi.showFullScreenAd]와 같다. [Capability.ADMOB]가 필요하다. */
    fun showAppsInTossAdMob(adGroupId: String): Flow<FullScreenAdEvent> = client.adEvents("admobShow", adGroupPayload(adGroupId))

    /** [Capability.ADMOB]가 필요하다. */
    suspend fun isAppsInTossAdMobLoaded(adGroupId: String): Boolean {
        val result = client.call("admobLoaded", adGroupPayload(adGroupId))
        return decode("AdMob loaded") { Json.parseToJsonElement(result).jsonPrimitive.also { require(!it.isString) }.boolean }
    }
}

class TossAdsApi internal constructor(private val client: AppsInTossClient) {
    /** [attachBanner] 전에 배너 SDK를 한 번 초기화한다. [Capability.BANNER_AD]가 필요하다. */
    suspend fun initialize() {
        client.call("bannerInitialize")
    }

    /**
     * 배너를 붙인다. bridge는 자체 HTML 요소를 [bounds] 위치의 Compose canvas 위에 올리며, 그 요소만
     * 포인터 입력을 받는다. 배너는 [BannerHandle.destroy], 해당 slot의 [destroy], [destroyAll] 또는 [scope]
     * 취소 때까지 유지된다. [onEvent]와 [onError]는 [scope]에서 실행되고, bridge가 실패하면 배너가 끝난다.
     * [Capability.BANNER_AD]가 필요하다.
     */
    fun attachBanner(
        scope: CoroutineScope,
        adGroupId: String,
        bounds: BannerBounds,
        theme: BannerTheme? = null,
        tone: BannerTone? = null,
        variant: BannerVariant? = null,
        onError: (AppsInTossException) -> Unit = {},
        onEvent: (BannerEvent) -> Unit = {},
    ): BannerHandle {
        requireAdGroupId(adGroupId)
        val payload = buildJsonObject {
            put("adGroupId", adGroupId)
            put("bounds", bounds.toJson())
            theme?.let { put("theme", it.value) }
            tone?.let { put("tone", it.value) }
            variant?.let { put("variant", it.value) }
        }
        val handle = BannerHandle(bounds)
        var sent = bounds
        handle.job = scope.launch {
            try {
                // bridge는 응답할 때마다 bounds를 다시 물으므로 여기서는 다음 변경을 기다린다.
                client.stream("bannerAttach", payload) { handle.bounds.first { it != sent }.also { sent = it }.toJson() }
                    .takeWhile { decode("banner event") { it.string("type") } != "destroyed" }
                    .collect { event -> decode("banner event") { event.toBannerEvent() }?.let(onEvent) }
            } catch (e: AppsInTossException) {
                onError(e)
            }
        }
        return handle
    }

    /** [BannerEvent]의 [slotId]에 해당하는 배너 slot을 제거한다. [Capability.BANNER_AD]가 필요하다. */
    suspend fun destroy(slotId: String) {
        require(slotId.isNotEmpty()) { "slotId is required" }
        client.call("bannerDestroy", buildJsonObject { put("slotId", slotId) })
    }

    /** 모든 배너 slot을 제거한다. [Capability.BANNER_AD]가 필요하다. */
    suspend fun destroyAll() {
        client.call("bannerDestroyAll")
    }
}

/** [TossAdsApi.attachBanner]로 붙인 배너다. 호스트 UI 스레드에서 호출한다. */
class BannerHandle internal constructor(initial: BannerBounds) {
    internal val bounds = MutableStateFlow(initial)
    internal var job: Job? = null

    /** 배너 요소를 옮긴다. [Capability.BANNER_AD]가 필요하다. */
    fun updateBounds(bounds: BannerBounds) {
        this.bounds.value = bounds
    }

    /** 배너를 제거하고 그 요소를 DOM에서 뺀다. [Capability.BANNER_AD]가 필요하다. */
    fun destroy() {
        job?.cancel()
    }
}

/** viewport 왼쪽 위 모서리 기준의 배너 위치(CSS px)다. 웹에서는 Compose dp가 CSS px와 같다. */
data class BannerBounds(val left: Double, val top: Double, val width: Double, val height: Double) {
    init {
        require(listOf(left, top, width, height).all { it.isFinite() } && width >= 0 && height >= 0) { "Bounds must be finite with non-negative size" }
    }
}

enum class BannerTheme(internal val value: String) { AUTO("auto"), LIGHT("light"), DARK("dark") }
enum class BannerTone(internal val value: String) { BLACK_AND_WHITE("blackAndWhite"), GREY("grey") }
enum class BannerVariant(internal val value: String) { CARD("card"), EXPANDED("expanded") }

sealed interface BannerEvent {
    val slotId: String
    val adGroupId: String

    data class Rendered(override val slotId: String, override val adGroupId: String, val creativeId: String, val requestId: String) : BannerEvent
    data class Viewable(override val slotId: String, override val adGroupId: String, val creativeId: String, val requestId: String) : BannerEvent
    data class Clicked(override val slotId: String, override val adGroupId: String, val creativeId: String, val requestId: String) : BannerEvent
    data class Impression(override val slotId: String, override val adGroupId: String, val creativeId: String, val requestId: String) : BannerEvent
    data class FailedToRender(override val slotId: String, override val adGroupId: String, val errorCode: Int) : BannerEvent
    data class NoFill(override val slotId: String, override val adGroupId: String) : BannerEvent
}

sealed interface FullScreenAdEvent {
    data object Requested : FullScreenAdEvent
    data object Show : FullScreenAdEvent
    data object Impression : FullScreenAdEvent
    data object Clicked : FullScreenAdEvent
    data object Dismissed : FullScreenAdEvent
    data object FailedToShow : FullScreenAdEvent
    data class UserEarnedReward(val unitType: String, val unitAmount: Double) : FullScreenAdEvent
}

data class AdMobLoadResult(val adGroupId: String, val adUnitId: String, val responseInfo: AdMobResponseInfo)
data class AdMobResponseInfo(val adNetworkInfoArray: List<AdNetworkInfo>, val loadedAdNetworkInfo: AdNetworkInfo?, val responseId: String?)
data class AdNetworkInfo(
    val adSourceId: String,
    val adSourceName: String,
    val adSourceInstanceId: String,
    val adSourceInstanceName: String,
    val adNetworkClassName: String?,
)

private fun requireAdGroupId(adGroupId: String) = require(adGroupId.isNotBlank()) { "adGroupId is required" }

private fun adGroupPayload(adGroupId: String): JsonObject {
    requireAdGroupId(adGroupId)
    return buildJsonObject { put("adGroupId", adGroupId) }
}

private fun AppsInTossClient.adEvents(method: String, payload: JsonObject): Flow<FullScreenAdEvent> = stream(method, payload)
    .mapNotNull { decode("ad event") { it.toAdEvent() } }
    .transformWhile {
        emit(it)
        it != FullScreenAdEvent.Dismissed && it != FullScreenAdEvent.FailedToShow
    }

// 기존 광고 흐름이 계속 동작하도록 새 SDK의 알 수 없는 이벤트 타입은 건너뛴다.
private fun JsonObject.toAdEvent(): FullScreenAdEvent? = when (string("type")) {
    "requested" -> FullScreenAdEvent.Requested

    "show" -> FullScreenAdEvent.Show

    "impression" -> FullScreenAdEvent.Impression

    "clicked" -> FullScreenAdEvent.Clicked

    "dismissed" -> FullScreenAdEvent.Dismissed

    "failedToShow" -> FullScreenAdEvent.FailedToShow

    "userEarnedReward" -> getValue("data").jsonObject.let {
        FullScreenAdEvent.UserEarnedReward(it.string("unitType"), it.getValue("unitAmount").jsonPrimitive.also { amount -> require(!amount.isString) }.double)
    }

    else -> null
}

private fun JsonObject.toBannerEvent(): BannerEvent? {
    val slotId = string("slotId")
    val adGroupId = string("adGroupId")
    return when (string("type")) {
        "rendered" -> BannerEvent.Rendered(slotId, adGroupId, string("creativeId"), string("requestId"))
        "viewable" -> BannerEvent.Viewable(slotId, adGroupId, string("creativeId"), string("requestId"))
        "clicked" -> BannerEvent.Clicked(slotId, adGroupId, string("creativeId"), string("requestId"))
        "impression" -> BannerEvent.Impression(slotId, adGroupId, string("creativeId"), string("requestId"))
        "failedToRender" -> BannerEvent.FailedToRender(slotId, adGroupId, getValue("code").jsonPrimitive.also { require(!it.isString) }.int)
        "noFill" -> BannerEvent.NoFill(slotId, adGroupId)
        else -> null
    }
}

private fun JsonObject.toAdNetworkInfo() = AdNetworkInfo(
    adSourceId = string("adSourceId"),
    adSourceName = string("adSourceName"),
    adSourceInstanceId = string("adSourceInstanceId"),
    adSourceInstanceName = string("adSourceInstanceName"),
    adNetworkClassName = optionalString("adNetworkClassName"),
)

private fun JsonObject.optionalString(key: String): String? = if (getValue(key) is JsonNull) null else string(key)

private fun BannerBounds.toJson() = buildJsonObject {
    put("left", left)
    put("top", top)
    put("width", width)
    put("height", height)
}
