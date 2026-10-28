package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class NotificationApiTest {
    @Test fun `동의 요청은 결과를 디코딩하고 정리한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        val result = async { c.notification.requestAgreement("template") }
        runCurrent()
        assertEquals("notificationAgreement" to """{"templateCode":"template"}""", t.streams.single())
        t.onEvent("""{"type":"agreementRejected"}""")
        assertEquals(NotificationAgreementResult.AGREEMENT_REJECTED, result.await())
        assertEquals(1, t.cleaned)
    }

    @Test fun `동의 요청은 잘못된 입력과 이벤트를 거부한다`() = runTest {
        val t = FakeTransport().apply { streamStarted = { onEvent("""{"type":"maybe"}""") } }
        val c = AppsInTossClient(t)
        assertFailsWith<IllegalArgumentException> { c.notification.requestAgreement(" ") }
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.notification.requestAgreement("t") }.code)
        t.streamStarted = { t.streamFailure("""{"code":"UNSUPPORTED"}""") }
        assertEquals(ErrorCode.UNSUPPORTED, assertFailsWith<AppsInTossException> { c.notification.requestAgreement("t") }.code)
        assertEquals(2, t.cleaned)
    }
}
