package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class DeviceApiTest {
    private val transport = FakeTransport()
    private val device = AppsInTossClient(transport).device

    @Test fun valuesAndOpenURLSendPayloadsAndDecode() = runTest {
        transport.reply = "\"ko-KR\""
        assertEquals("ko-KR", device.locale())
        transport.reply = "\"ios\""
        assertEquals(DeviceOs.IOS, device.os())
        transport.reply = "null"
        device.openURL("https://toss.im")
        assertEquals(
            listOf("deviceValue" to """{"name":"locale"}""", "deviceValue" to """{"name":"os"}""", "openURL" to """{"url":"https://toss.im"}"""),
            transport.calls,
        )
    }

    @Test fun malformedValuesAreInvalid() = runTest {
        transport.reply = "\"windows\""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { device.os() }.code)
        transport.reply = "1"
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { device.locale() }.code)
    }

    @Test fun blankURLIsRejected() = runTest {
        assertFailsWith<IllegalArgumentException> { device.openURL(" ") }
        assertTrue(transport.calls.isEmpty())
    }
}
