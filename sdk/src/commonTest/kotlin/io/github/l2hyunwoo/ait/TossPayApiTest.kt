package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.*

class TossPayApiTest {
    @Test fun `인증은 토큰을 보내고 결과를 디코딩한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        t.reply = """{"success":true}"""
        assertEquals(TossPayAuthorization(true, null), c.tossPay.authorize("pay"))
        assertEquals("tossPayAuthorize" to """{"payToken":"pay"}""", t.calls.last())
        t.reply = """{"success":false,"reason":"declined"}"""
        assertEquals(TossPayAuthorization(false, "declined"), c.tossPay.authorizeSubscription("wrapped"))
        assertEquals("tossPayAuthorizeSubscription" to """{"wrappedToken":"wrapped"}""", t.calls.last())
    }

    @Test fun `인증은 잘못된 입력과 응답을 거부한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        assertFailsWith<IllegalArgumentException> { c.tossPay.authorize(" ") }
        assertFailsWith<IllegalArgumentException> { c.tossPay.authorizeSubscription("") }
        assertTrue(t.calls.isEmpty())
        t.reply = """{"reason":"x"}"""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.tossPay.authorize("pay") }.code)
        t.reply = """{"success":false,"reason":1}"""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.tossPay.authorizeSubscription("w") }.code)
    }
}
