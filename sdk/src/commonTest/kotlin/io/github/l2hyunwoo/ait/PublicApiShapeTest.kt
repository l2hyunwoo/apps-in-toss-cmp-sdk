package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.test.*

/** 공개 API 전체를 한 번씩 호출해 시그니처와 브리지 method 연결을 고정한다. 응답 디코딩은 기능별 테스트가 맡는다. */
@OptIn(ExperimentalCoroutinesApi::class)
class PublicApiShapeTest {
    private val transport = FakeTransport().apply { reply = null }
    private val client = AppsInTossClient(transport)
    private val reached = mutableSetOf<String>()

    private fun TestScope.expect(bridge: String, block: suspend () -> Unit) {
        transport.calls.clear()
        transport.streams.clear()
        val job = backgroundScope.launch { runCatching { block() } }
        runCurrent()
        val sent = (transport.calls + transport.streams).map { it.first }
        assertEquals(listOf(bridge), sent, "$bridge 요청")
        reached += bridge
        job.cancel()
        runCurrent()
    }

    private fun TestScope.expectFlow(bridge: String, flow: () -> Flow<*>) = expect(bridge) { flow().collect() }

    @Test fun `공개 API가 모두 브리지 method로 연결된다`() = runTest {
        expectFlow("hostEvents") { client.events }
        expect("login") { client.auth.login() }
        expect("isIntegrated") { client.auth.isIntegrated() }
        expect("sign") { client.auth.sign("tx") }

        expect("anonymousKey") { client.user.getAnonymousKey() }
        expect("anonymousKeyAuthCode") { client.user.createAnonymousKeyAuthCode() }
        expect("consentedData") { client.user.getConsentedData("key") }
        expect("declaredAgeRange") { client.user.getDeclaredAgeRange(listOf(19)) }

        expect("share") { client.share.sendMessage("hi") }
        expect("shareLink") { client.share.createLink("intoss://app") }

        expect("closeScreen") { client.screen.close() }
        expect("screenAwakeMode") { client.screen.setAwakeMode(true) }
        expect("screenSecure") { client.screen.setSecure(true) }
        expect("screenIosSwipeBack") { client.screen.setIosSwipeBack(false) }
        expect("screenOrientation") { client.screen.setOrientation(DeviceOrientation.LANDSCAPE) }
        expect("navigationBarOptions") { client.navigationBar.setOptions(withTitle = true, theme = NavigationBarTheme.DARK) }

        expect("storageGet") { client.storage.getItem("k") }
        expect("storageGetItems") { client.storage.getItems(listOf("k")) }
        expect("storageSet") { client.storage.setItem("k", "v") }
        expect("storageSetItems") { client.storage.setItems(mapOf("k" to "v")) }
        expect("storageRemove") { client.storage.removeItem("k") }
        expect("storageClear") { client.storage.clearItems() }

        expect("haptic") { client.device.triggerHaptic(HapticType.TAP) }
        expect("deviceValue") { client.device.locale() }
        expect("deviceValue") { client.device.os() }
        expect("openURL") { client.device.openURL("https://toss.im") }

        expect("photosGet") { client.media.getPhotos() }
        expect("albumItemsGet") { client.media.getAlbumItems(setOf(AlbumItemType.PHOTO, AlbumItemType.VIDEO)) }
        expect("cameraOpen") { client.media.openCamera() }
        expect("contactsGet") { client.contacts.getContacts(size = 10, offset = 0) }
        expect("locationGet") { client.location.getLocation(Accuracy.BALANCED) }
        expectFlow("locationSubscribe") { client.location.subscribeLocation(Accuracy.HIGH, 1_000, 10.0) }

        expect("permissionGet") { client.permissions.getPermission(Permission.CLIPBOARD_READ) }
        expect("permissionRequest") { client.permissions.requestPermission(Permission.CLIPBOARD_READ) }
        expect("permissionDialog") { client.permissions.openPermissionDialog(Permission.CLIPBOARD_READ) }
        expect("clipboardGetText") { client.clipboard.getText() }
        expect("clipboardSetText") { client.clipboard.setText("t") }

        expect("environmentValue") { client.environment.deviceId() }
        expect("environmentValue") { client.environment.groupId() }
        expect("environmentValue") { client.environment.environment() }
        expect("environmentValue") { client.environment.tossAppVersion() }
        expect("environmentValue") { client.environment.deploymentId() }
        expect("environmentValue") { client.environment.initialURL() }
        expect("networkStatus") { client.environment.getNetworkStatus() }
        expect("serverTime") { client.environment.getServerTime() }
        expect("isMinVersionSupported") { client.environment.isMinVersionSupported("5.200.0", "5.200.0") }

        expect("gameProfile") { client.game.getUserProfile() }
        expect("leaderboardSubmit") { client.game.setLeaderboardScore(1.0) }
        expect("leaderboardOpen") { client.game.openLeaderboard() }
        expectFlow("gameInfoOverlayHidden") { client.game.subscribeInfoOverlayHidden() }

        expect("iapOneTimeOrder") { client.iap.createOneTimePurchaseOrder("sku") { true } }
        expect("iapSubscriptionOrder") { client.iap.createSubscriptionPurchaseOrder("sku") { _, _ -> true } }
        expect("iapProducts") { client.iap.getProductItemList() }
        expect("iapPendingOrders") { client.iap.getPendingOrders() }
        expect("iapCompletedOrders") { client.iap.getCompletedOrRefundedOrders() }
        expect("iapCompleteGrant") { client.iap.completeProductGrant("order") }
        expect("iapSubscriptionInfo") { client.iap.getSubscriptionInfo("order") }
        expect("tossPayAuthorize") { client.tossPay.authorize("token") }
        expect("tossPayAuthorizeSubscription") { client.tossPay.authorizeSubscription("token") }

        expect("loadFullScreenAd") { client.ads.loadFullScreenAd("ad") }
        expectFlow("showFullScreenAd") { client.ads.showFullScreenAd("ad") }
        expect("admobLoad") { client.ads.admob.loadAppsInTossAdMob("ad") }
        expectFlow("admobShow") { client.ads.admob.showAppsInTossAdMob("ad") }
        expect("admobLoaded") { client.ads.admob.isAppsInTossAdMobLoaded("ad") }
        expect("bannerInitialize") { client.ads.tossAds.initialize() }
        expect("bannerAttach") {
            val handle = client.ads.tossAds.attachBanner(this@runTest.backgroundScope, "ad", BannerBounds(0.0, 0.0, 320.0, 50.0), theme = BannerTheme.LIGHT)
            handle.updateBounds(BannerBounds(0.0, 10.0, 320.0, 50.0))
            awaitCancellation()
        }
        expect("bannerDestroy") { client.ads.tossAds.destroy("slot") }
        expect("bannerDestroyAll") { client.ads.tossAds.destroyAll() }

        expect("promotionGrantReward") { client.promotion.grantReward("CODE", 100) }
        expect("analyticsLog") { client.analytics.log("event", LogType.EVENT, mapOf("count" to 1)) }
        expect("analyticsScreen") { client.analytics.screen("home") }
        expect("analyticsImpression") { client.analytics.impression("banner") }
        expect("analyticsClick") { client.analytics.click("button") }
        expect("reviewRequest") { client.review.request() }
        expect("notificationAgreement") { client.notification.requestAgreement("TEMPLATE") }
        expect("pdfViewer") { client.file.openPDFViewer("JVBERi0=") }
        expect("fileSaveBase64") { client.file.saveBase64("aGk=", "hi.txt", "text/plain") }
        expect("migrationOriginStorage") { client.migration.getOriginStorage() }

        // 커버리지 매니페스트의 브리지 method 수(reply 제외)와 같아야 한다. deviceValue·environmentValue는 여러 메서드가 공유한다.
        assertEquals(72, reached.size)
    }
}
