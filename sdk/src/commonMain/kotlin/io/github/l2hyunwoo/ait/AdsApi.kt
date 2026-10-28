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

    /** Preloads a full-screen ad and returns after the `loaded` event. Requires [Capability.FULL_SCREEN_AD]. */
    suspend fun loadFullScreenAd(adGroupId: String) {
        client.stream("loadFullScreenAd", adGroupPayload(adGroupId)).first { decode("full-screen ad load") { it.string("type") } == "loaded" }
    }

    /**
     * Shows a preloaded full-screen ad. Completes after [FullScreenAdEvent.Dismissed] or [FullScreenAdEvent.FailedToShow];
     * cancelling collection releases the official callbacks. Grant rewards only on [FullScreenAdEvent.UserEarnedReward].
     * Requires [Capability.FULL_SCREEN_AD].
     */
    fun showFullScreenAd(adGroupId: String): Flow<FullScreenAdEvent> = client.adEvents("showFullScreenAd", adGroupPayload(adGroupId))
}

/** Official v2 compatibility alias for AdMob. Prefer [AdsApi.loadFullScreenAd] and [AdsApi.showFullScreenAd]. */
class AdMobApi internal constructor(private val client: AppsInTossClient) {
    /** Preloads an AdMob ad and returns after the `loaded` event. Requires [Capability.ADMOB]. */
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

    /** Shows a preloaded AdMob ad; same completion rules as [AdsApi.showFullScreenAd]. Requires [Capability.ADMOB]. */
    fun showAppsInTossAdMob(adGroupId: String): Flow<FullScreenAdEvent> = client.adEvents("admobShow", adGroupPayload(adGroupId))

    /** Requires [Capability.ADMOB]. */
    suspend fun isAppsInTossAdMobLoaded(adGroupId: String): Boolean {
        val result = client.call("admobLoaded", adGroupPayload(adGroupId))
        return decode("AdMob loaded") { Json.parseToJsonElement(result).jsonPrimitive.also { require(!it.isString) }.boolean }
    }
}

class TossAdsApi internal constructor(private val client: AppsInTossClient) {
    /** Initializes the banner SDK once before [attachBanner]. Requires [Capability.BANNER_AD]. */
    suspend fun initialize() {
        client.call("bannerInitialize")
    }

    /**
     * Attaches a banner. The bridge places its own HTML element over the Compose canvas at [bounds]; only that
     * element takes pointer input. The banner lives until [BannerHandle.destroy], [destroy] of its slot,
     * [destroyAll], or cancellation of [scope]. [onEvent] and [onError] run in [scope]; bridge failures end the banner.
     * Requires [Capability.BANNER_AD].
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
                // The bridge asks for bounds again after each answer, so this waits for the next change.
                client.stream("bannerAttach", payload) { handle.bounds.first { it != sent }.also { sent = it }.toJson() }
                    .takeWhile { decode("banner event") { it.string("type") } != "destroyed" }
                    .collect { event -> decode("banner event") { event.toBannerEvent() }?.let(onEvent) }
            } catch (e: AppsInTossException) {
                onError(e)
            }
        }
        return handle
    }

    /** Destroys the banner slot with [slotId] from a [BannerEvent]. Requires [Capability.BANNER_AD]. */
    suspend fun destroy(slotId: String) {
        require(slotId.isNotEmpty()) { "slotId is required" }
        client.call("bannerDestroy", buildJsonObject { put("slotId", slotId) })
    }

    /** Destroys every banner slot. Requires [Capability.BANNER_AD]. */
    suspend fun destroyAll() {
        client.call("bannerDestroyAll")
    }
}

/** A banner attached by [TossAdsApi.attachBanner]. Call from the host UI thread. */
class BannerHandle internal constructor(initial: BannerBounds) {
    internal val bounds = MutableStateFlow(initial)
    internal var job: Job? = null

    /** Moves the banner element. Requires [Capability.BANNER_AD]. */
    fun updateBounds(bounds: BannerBounds) {
        this.bounds.value = bounds
    }

    /** Destroys the banner and removes its element from the DOM. Requires [Capability.BANNER_AD]. */
    fun destroy() {
        job?.cancel()
    }
}

/** Banner position in CSS px from the viewport's top-left corner. On web, Compose dp equal CSS px. */
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

// Unknown event types from newer SDKs are skipped so existing ad flows keep working.
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
