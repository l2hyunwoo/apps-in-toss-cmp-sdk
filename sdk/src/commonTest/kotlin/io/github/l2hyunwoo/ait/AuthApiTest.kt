package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class AuthApiTest {
    @Test fun isIntegratedDecodesBoolean() = runTest {
        val t = FakeTransport().apply { reply = "true" }
        val c = AppsInTossClient(t)
        assertTrue(c.auth.isIntegrated())
        assertEquals("isIntegrated" to "{}", t.calls.last())
        t.reply = "null"
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.auth.isIntegrated() }.code)
    }

    @Test fun signSendsTxIdAndRejectsBlank() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        assertFailsWith<IllegalArgumentException> { c.auth.sign(" ") }
        assertTrue(t.calls.isEmpty())
        c.auth.sign("tx-1")
        assertEquals("sign" to """{"txId":"tx-1"}""", t.calls.last())
    }
}
