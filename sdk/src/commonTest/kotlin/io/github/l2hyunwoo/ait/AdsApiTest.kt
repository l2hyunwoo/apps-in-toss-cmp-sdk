package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AdsApiTest {
    @Test fun loadFullScreenAdWaitsForLoaded() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        val load = async { c.ads.loadFullScreenAd("ad") }
        runCurrent()
        assertEquals("loadFullScreenAd" to """{"adGroupId":"ad"}""", t.streams.single())
        assertFalse(load.isCompleted)
        t.onEvent("""{"type":"loaded"}""")
        load.await()
        assertEquals(1, t.cleaned)
        assertFailsWith<IllegalArgumentException> { c.ads.loadFullScreenAd(" ") }
    }

    @Test fun showFullScreenAdCompletesAfterDismissed() = runTest {
        val t = FakeTransport().apply {
            streamStarted = {
                listOf("""{"type":"requested"}""", """{"type":"show"}""", """{"type":"impression"}""", """{"type":"future"}""", """{"type":"userEarnedReward","data":{"unitType":"coin","unitAmount":10}}""", """{"type":"dismissed"}""", """{"type":"clicked"}""").forEach(onEvent)
            }
        }
        val events = AppsInTossClient(t).ads.showFullScreenAd("ad").toList()
        assertEquals(
            listOf(FullScreenAdEvent.Requested, FullScreenAdEvent.Show, FullScreenAdEvent.Impression, FullScreenAdEvent.UserEarnedReward("coin", 10.0), FullScreenAdEvent.Dismissed),
            events,
        )
        assertEquals("showFullScreenAd" to """{"adGroupId":"ad"}""", t.streams.single())
        assertEquals(1, t.cleaned)
    }

    @Test fun cancellingShowRunsCleanupImmediately() = runTest {
        val t = FakeTransport()
        val events = mutableListOf<FullScreenAdEvent>()
        val job = launch { AppsInTossClient(t).ads.showFullScreenAd("ad").collect { events += it } }
        runCurrent()
        t.onEvent("""{"type":"requested"}""")
        runCurrent()
        job.cancelAndJoin()
        assertEquals(listOf<FullScreenAdEvent>(FullScreenAdEvent.Requested), events)
        assertEquals(1, t.cleaned)
    }

    @Test fun malformedAdEventsFail() = runTest {
        val t = FakeTransport().apply { streamStarted = { onEvent("""{"type":"userEarnedReward","data":{"unitType":"coin","unitAmount":"10"}}""") } }
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { AppsInTossClient(t).ads.admob.showAppsInTossAdMob("ad").toList() }.code)
        assertEquals("admobShow", t.streams.single().first)
    }

    @Test fun admobLoadDecodesResultAndLoadedState() = runTest {
        val t = FakeTransport().apply {
            streamStarted = {
                onEvent(
                    """{"type":"loaded","data":{"adGroupId":"ad","adUnitId":"unit","responseInfo":{"adNetworkInfoArray":[{"adSourceId":"1","adSourceName":"AdMob","adSourceInstanceId":"2","adSourceInstanceName":"default","adNetworkClassName":null}],"loadedAdNetworkInfo":null,"responseId":"res"}}}""",
                )
            }
        }
        val c = AppsInTossClient(t)
        val result = c.ads.admob.loadAppsInTossAdMob("ad", referrer = "home")
        assertEquals("admobLoad" to """{"adGroupId":"ad","referrer":"home"}""", t.streams.single())
        assertEquals(AdMobLoadResult("ad", "unit", AdMobResponseInfo(listOf(AdNetworkInfo("1", "AdMob", "2", "default", null)), null, "res")), result)

        t.streamStarted = { t.onEvent("""{"type":"loaded","data":{"adGroupId":"ad"}}""") }
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.ads.admob.loadAppsInTossAdMob("ad") }.code)
        assertEquals("""{"adGroupId":"ad","referrer":null}""", t.streams.last().second)

        t.reply = "true"
        assertTrue(c.ads.admob.isAppsInTossAdMobLoaded("ad"))
        assertEquals("admobLoaded" to """{"adGroupId":"ad"}""", t.calls.last())
        t.reply = "\"true\""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.ads.admob.isAppsInTossAdMobLoaded("ad") }.code)
    }

    @Test fun tossAdsCallsSendPayloads() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        c.ads.tossAds.initialize()
        c.ads.tossAds.destroy("slot")
        c.ads.tossAds.destroyAll()
        assertEquals(listOf("bannerInitialize" to "{}", "bannerDestroy" to """{"slotId":"slot"}""", "bannerDestroyAll" to "{}"), t.calls)
        assertFailsWith<IllegalArgumentException> { c.ads.tossAds.destroy("") }
        assertFailsWith<IllegalArgumentException> { BannerBounds(0.0, 0.0, -1.0, 10.0) }
    }

    @Test fun bannerHandleUpdatesBoundsAndDestroys() = runTest {
        val t = FakeTransport().apply { reply = "null" }
        val c = AppsInTossClient(t)
        val events = mutableListOf<BannerEvent>()
        val errors = mutableListOf<AppsInTossException>()
        val handle = c.ads.tossAds.attachBanner(this, "ad", BannerBounds(1.0, 2.0, 300.0, 96.0), variant = BannerVariant.CARD, onError = { errors += it }) { events += it }
        runCurrent()
        assertEquals(
            "bannerAttach" to """{"adGroupId":"ad","bounds":{"left":1.0,"top":2.0,"width":300.0,"height":96.0},"variant":"card"}""",
            t.streams.single(),
        )
        t.onEvent("""{"type":"rendered","slotId":"s","adGroupId":"ad","creativeId":"c","requestId":"r"}""")
        t.onEvent("""{"type":"failedToRender","slotId":"s","adGroupId":"ad","code":7}""")
        t.onEvent("""{"type":"bounds","replyId":"r1"}""")
        runCurrent()
        assertEquals(listOf(BannerEvent.Rendered("s", "ad", "c", "r"), BannerEvent.FailedToRender("s", "ad", 7)), events)
        assertTrue(t.calls.isEmpty(), "bounds question waits for a change")

        handle.updateBounds(BannerBounds(5.0, 6.0, 300.0, 96.0))
        runCurrent()
        assertEquals("reply" to """{"replyId":"r1","value":{"left":5.0,"top":6.0,"width":300.0,"height":96.0}}""", t.calls.single())

        handle.destroy()
        runCurrent()
        assertEquals(2, t.cleaned, "reply call and banner stream")
        assertTrue(errors.isEmpty())
    }

    @Test fun bannerEndsOnDestroyedAndReportsFailures() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        val errors = mutableListOf<ErrorCode>()
        c.ads.tossAds.attachBanner(this, "ad", BannerBounds(0.0, 0.0, 1.0, 1.0), onError = { errors += it.code })
        runCurrent()
        t.onEvent("""{"type":"destroyed"}""")
        runCurrent()
        assertEquals(1, t.cleaned)

        c.ads.tossAds.attachBanner(this, "ad", BannerBounds(0.0, 0.0, 1.0, 1.0), onError = { errors += it.code })
        runCurrent()
        t.onEvent("""{"type":"rendered","slotId":"s"}""")
        runCurrent()
        c.ads.tossAds.attachBanner(this, "ad", BannerBounds(0.0, 0.0, 1.0, 1.0), onError = { errors += it.code })
        runCurrent()
        t.streamFailure("""{"code":"UNSUPPORTED"}""")
        runCurrent()
        assertEquals(listOf(ErrorCode.INVALID_RESPONSE, ErrorCode.UNSUPPORTED), errors)
    }
}
