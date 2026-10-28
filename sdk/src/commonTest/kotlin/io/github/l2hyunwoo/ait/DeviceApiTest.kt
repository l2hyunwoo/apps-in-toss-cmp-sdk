package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class DeviceApiTest {
    private val transport = FakeTransport()
    private val device = AppsInTossClient(transport).device

    @Test fun `기기 값과 openURL은 payload를 보내고 디코딩한다`() = runTest {
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

    @Test fun `형식이 잘못된 값은 INVALID_RESPONSE다`() = runTest {
        transport.reply = "\"windows\""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { device.os() }.code)
        transport.reply = "1"
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { device.locale() }.code)
    }

    @Test fun `빈 URL을 거부한다`() = runTest {
        assertFailsWith<IllegalArgumentException> { device.openURL(" ") }
        assertTrue(transport.calls.isEmpty())
    }
}
