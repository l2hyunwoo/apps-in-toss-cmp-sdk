package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.*

class TossPayApiTest {
    @Test fun authorizeSendsTokensAndDecodesResults() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        t.reply = """{"success":true}"""
        assertEquals(TossPayAuthorization(true, null), c.tossPay.authorize("pay"))
        assertEquals("tossPayAuthorize" to """{"payToken":"pay"}""", t.calls.last())
        t.reply = """{"success":false,"reason":"declined"}"""
        assertEquals(TossPayAuthorization(false, "declined"), c.tossPay.authorizeSubscription("wrapped"))
        assertEquals("tossPayAuthorizeSubscription" to """{"wrappedToken":"wrapped"}""", t.calls.last())
    }

    @Test fun authorizeRejectsInvalidInputAndResponses() = runTest {
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
