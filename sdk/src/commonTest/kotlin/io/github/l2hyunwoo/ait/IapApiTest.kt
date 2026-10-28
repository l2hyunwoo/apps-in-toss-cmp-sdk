package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class IapApiTest {
    private val order = """{"orderId":"o1","displayName":"Coin","displayAmount":"1,000원","amount":1000,"currency":"KRW","fraction":0,"miniAppIconUrl":null}"""

    // 모든 지급 사례를 두 주문 종류에서 실행하도록 한 형태로 묶는다.
    private val kinds: List<Pair<String, suspend (AppsInTossClient, suspend (String) -> Boolean) -> PurchaseResult>> = listOf(
        "iapOneTimeOrder" to { c, grant -> c.iap.createOneTimePurchaseOrder("coin") { grant(it) } },
        "iapSubscriptionOrder" to { c, grant -> c.iap.createSubscriptionPurchaseOrder("premium", "trial") { id, _ -> grant(id) } },
    )

    private fun replies(t: FakeTransport) = t.calls.filter { it.first == "reply" }.map { it.second }

    @Test fun `지급에 성공하면 true를 보내고 주문을 디코딩한다`() = runTest {
        for ((method, purchase) in kinds) {
            val t = FakeTransport()
            val granted = mutableListOf<String>()
            val result = async {
                purchase(AppsInTossClient(t)) {
                    granted += it
                    true
                }
            }
            runCurrent()
            t.onEvent("""{"type":"grant","orderId":"o1","subscriptionId":"s1","replyId":"r1"}""")
            runCurrent()
            assertEquals(listOf("o1"), granted)
            assertEquals(listOf("""{"replyId":"r1","value":true}"""), replies(t))
            t.onEvent("""{"type":"success","data":$order}""")
            assertEquals(PurchaseResult.Success(PurchaseOrder("o1", "Coin", "1,000원", 1000.0, "KRW", 0, null)), result.await())
            assertEquals(method, t.streams.single().first)
            assertEquals(2, t.cleaned)
        }
    }

    @Test fun `지급이 예외를 던지면 null로 답해 bridge가 false를 쓴다`() = runTest {
        for ((_, purchase) in kinds) {
            val t = FakeTransport()
            val result = async { purchase(AppsInTossClient(t)) { error("server down") } }
            runCurrent()
            t.onEvent("""{"type":"grant","orderId":"o1","replyId":"r1"}""")
            runCurrent()
            assertEquals(listOf("""{"replyId":"r1","value":null}"""), replies(t))
            t.onEvent("""{"type":"error","code":"PRODUCT_NOT_GRANTED_BY_PARTNER"}""")
            assertEquals(PurchaseResult.Failure(PurchaseError.PRODUCT_NOT_GRANTED_BY_PARTNER), result.await())
        }
    }

    @Test fun `지급 중 취소하면 답하지 않고 stream을 멈춘다`() = runTest {
        for ((_, purchase) in kinds) {
            val t = FakeTransport()
            val gate = CompletableDeferred<Boolean>()
            val result = async { purchase(AppsInTossClient(t)) { gate.await() } }
            runCurrent()
            t.onEvent("""{"type":"grant","orderId":"o1","replyId":"r1"}""")
            runCurrent()
            result.cancelAndJoin()
            assertEquals(1, t.cleaned)
            assertTrue(replies(t).isEmpty())
        }
    }

    @Test fun `지급 중 close하면 답하지 않고 stream을 멈춘다`() = runTest {
        for ((_, purchase) in kinds) {
            val t = FakeTransport()
            val c = AppsInTossClient(t)
            val gate = CompletableDeferred<Boolean>()
            val result = async { runCatching { purchase(c) { gate.await() } }.exceptionOrNull() }
            runCurrent()
            t.onEvent("""{"type":"grant","orderId":"o1","replyId":"r1"}""")
            runCurrent()
            c.close()
            assertEquals(ErrorCode.HOST_UNAVAILABLE, (result.await() as AppsInTossException).code)
            assertEquals(1, t.cleaned)
            assertTrue(replies(t).isEmpty())
        }
    }

    @Test fun `주문 payload와 결과와 검증을 확인한다`() = runTest {
        val t = FakeTransport().apply { streamStarted = { onEvent("""{"type":"error","code":"USER_CANCELED"}""") } }
        val c = AppsInTossClient(t)
        assertEquals(PurchaseResult.Failure(PurchaseError.USER_CANCELED), c.iap.createOneTimePurchaseOrder("coin") { true })
        assertEquals("""{"sku":"coin"}""", t.streams.last().second)
        c.iap.createSubscriptionPurchaseOrder("premium") { _, _ -> true }
        assertEquals("""{"sku":"premium","offerId":null}""", t.streams.last().second)

        t.streamStarted = { t.onEvent("""{"type":"success","data":{"orderId":"o1"}}""") }
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.iap.createOneTimePurchaseOrder("coin") { true } }.code)
        t.streamStarted = { t.onEvent("""{"type":"error","code":"SOMETHING"}""") }
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.iap.createOneTimePurchaseOrder("coin") { true } }.code)

        val streams = t.streams.size
        assertFailsWith<IllegalArgumentException> { c.iap.createOneTimePurchaseOrder(" ") { true } }
        assertFailsWith<IllegalArgumentException> { c.iap.createSubscriptionPurchaseOrder("premium", " ") { _, _ -> true } }
        assertEquals(streams, t.streams.size)
    }

    @Test fun `구독 지급은 subscriptionId를 받는다`() = runTest {
        val t = FakeTransport()
        var received: Pair<String, String?>? = null
        val result = async {
            AppsInTossClient(t).iap.createSubscriptionPurchaseOrder("premium") { id, sub ->
                received = id to sub
                true
            }
        }
        runCurrent()
        t.onEvent("""{"type":"grant","orderId":"o1","subscriptionId":null,"replyId":"r1"}""")
        runCurrent()
        assertEquals("o1" to null, received)
        t.onEvent("""{"type":"success","data":$order}""")
        assertIs<PurchaseResult.Success>(result.await())
    }

    @Test fun `조회는 payload를 보내고 응답을 디코딩한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        t.reply = """{"products":[
            {"sku":"coin","type":"CONSUMABLE","displayName":"Coin","displayAmount":"1,000원","iconUrl":"i","description":"d"},
            {"sku":"premium","type":"SUBSCRIPTION","displayName":"Premium","displayAmount":"4,900원","iconUrl":"i","description":"d","renewalCycle":"MONTHLY",
             "offers":[{"type":"FREE_TRIAL","offerId":"trial","period":"P7D"},{"type":"RETURNING","offerId":"back","period":"P1M","displayAmount":"2,900원"}]}]}"""
        assertEquals(
            listOf(
                IapProduct("coin", IapProductType.CONSUMABLE, "Coin", "1,000원", "i", "d", null, emptyList()),
                IapProduct(
                    "premium",
                    IapProductType.SUBSCRIPTION,
                    "Premium",
                    "4,900원",
                    "i",
                    "d",
                    RenewalCycle.MONTHLY,
                    listOf(IapOffer(IapOfferType.FREE_TRIAL, "trial", "P7D", null), IapOffer(IapOfferType.RETURNING, "back", "P1M", "2,900원")),
                ),
            ),
            c.iap.getProductItemList(),
        )
        assertEquals("iapProducts", t.calls.last().first)

        t.reply = """{"orders":[{"orderId":"o1","sku":"coin","paymentCompletedDate":"2026-01-01"}]}"""
        assertEquals(listOf(PendingOrder("o1", "coin", "2026-01-01")), c.iap.getPendingOrders())

        t.reply = """{"hasNext":true,"nextKey":"n2","orders":[{"orderId":"o1","sku":"coin","status":"REFUNDED","date":"2025-09-22T00:00:00"}]}"""
        assertEquals(
            CompletedOrRefundedOrders(true, "n2", listOf(CompletedOrRefundedOrder("o1", "coin", OrderStatus.REFUNDED, "2025-09-22T00:00:00"))),
            c.iap.getCompletedOrRefundedOrders("n1"),
        )
        assertEquals("iapCompletedOrders" to """{"key":"n1"}""", t.calls.last())
        t.reply = """{"hasNext":false,"orders":[]}"""
        assertEquals(CompletedOrRefundedOrders(false, null, emptyList()), c.iap.getCompletedOrRefundedOrders())
        assertEquals("""{"key":null}""", t.calls.last().second)

        t.reply = "true"
        assertTrue(c.iap.completeProductGrant("o1"))
        assertEquals("iapCompleteGrant" to """{"orderId":"o1"}""", t.calls.last())

        t.reply = """{"subscription":{"catalogId":7,"status":"SOMETHING_NEW","expiresAt":"2026-02-01","isAutoRenew":true,"gracePeriodExpiresAt":null,"isAccessible":true}}"""
        assertEquals(SubscriptionInfo(7, "SOMETHING_NEW", "2026-02-01", true, null, true), c.iap.getSubscriptionInfo("o1"))
        assertEquals("iapSubscriptionInfo" to """{"orderId":"o1"}""", t.calls.last())
    }

    @Test fun `조회는 잘못된 입력과 응답을 거부한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        assertFailsWith<IllegalArgumentException> { c.iap.completeProductGrant(" ") }
        assertFailsWith<IllegalArgumentException> { c.iap.getSubscriptionInfo("") }
        assertFailsWith<IllegalArgumentException> { c.iap.getCompletedOrRefundedOrders(" ") }
        assertTrue(t.calls.isEmpty())
        t.reply = """{"products":[{"sku":"coin","type":"OTHER","displayName":"","displayAmount":"","iconUrl":"","description":""}]}"""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.iap.getProductItemList() }.code)
        t.reply = """{"orders":[{"orderId":"o1"}]}"""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.iap.getPendingOrders() }.code)
        t.reply = """{"orders":[]}"""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.iap.getCompletedOrRefundedOrders() }.code)
        t.reply = "null"
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.iap.completeProductGrant("o1") }.code)
        t.reply = """{"subscription":{"catalogId":7}}"""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.iap.getSubscriptionInfo("o1") }.code)
    }
}
