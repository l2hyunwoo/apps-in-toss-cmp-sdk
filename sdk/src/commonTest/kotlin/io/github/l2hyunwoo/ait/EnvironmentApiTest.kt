package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class EnvironmentApiTest {
    private val transport = FakeTransport()
    private val environment = AppsInTossClient(transport).environment

    @Test fun `환경 값은 이름을 보내고 디코딩한다`() = runTest {
        transport.reply = "\"value\""
        assertEquals("value", environment.deviceId())
        assertEquals("value", environment.groupId())
        assertEquals("value", environment.tossAppVersion())
        assertEquals("value", environment.deploymentId())
        assertEquals("value", environment.initialURL())
        transport.reply = "\"sandbox\""
        assertEquals(OperationalEnvironment.SANDBOX, environment.environment())
        assertEquals(
            listOf("deviceId", "groupId", "tossAppVersion", "deploymentId", "initialURL", "environment").map { "environmentValue" to """{"name":"$it"}""" },
            transport.calls,
        )
    }

    @Test fun `비동기 호출 결과를 디코딩한다`() = runTest {
        transport.reply = "\"5G\""
        assertEquals(NetworkStatus.CELLULAR_5G, environment.getNetworkStatus())
        transport.reply = "\"SATELLITE\""
        assertEquals(NetworkStatus.UNKNOWN, environment.getNetworkStatus())
        transport.reply = "1767225600000"
        assertEquals(1767225600000L, environment.getServerTime())
        transport.reply = "null"
        assertNull(environment.getServerTime())
        transport.reply = "true"
        assertTrue(environment.isMinVersionSupported("5.245.0", "always"))
        assertEquals("isMinVersionSupported" to """{"android":"5.245.0","ios":"always"}""", transport.calls.last())
    }

    @Test fun `형식이 잘못된 응답은 INVALID_RESPONSE다`() = runTest {
        transport.reply = "\"web\""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { environment.environment() }.code)
        transport.reply = "null"
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { environment.deviceId() }.code)
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { environment.getNetworkStatus() }.code)
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { environment.isMinVersionSupported("1.0.0", "never") }.code)
        transport.reply = "\"1767225600000\""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { environment.getServerTime() }.code)
    }

    @Test fun `잘못된 버전을 거부한다`() = runTest {
        assertFailsWith<IllegalArgumentException> { environment.isMinVersionSupported("5.0", "always") }
        assertFailsWith<IllegalArgumentException> { environment.isMinVersionSupported("5.0.0", "sometimes") }
        assertTrue(transport.calls.isEmpty())
    }
}
