package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class AnalyticsApiTest {
    @Test fun `로그 호출은 payload를 보낸다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        c.analytics.log("purchase", LogType.EVENT, mapOf("id" to 7, "price" to 1.5, "ok" to true, "name" to "x", "none" to null))
        assertEquals("analyticsLog" to """{"logName":"purchase","logType":"event","params":{"id":7,"price":1.5,"ok":true,"name":"x","none":null}}""", t.calls.last())
        c.analytics.screen()
        assertEquals("analyticsScreen" to """{"params":{}}""", t.calls.last())
        c.analytics.impression("banner", mapOf("slot" to "top"))
        assertEquals("analyticsImpression" to """{"logName":"banner","params":{"slot":"top"}}""", t.calls.last())
        c.analytics.click("buy")
        assertEquals("analyticsClick" to """{"logName":"buy","params":{}}""", t.calls.last())
    }

    @Test fun `로그 호출은 잘못된 입력을 거부한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        assertFailsWith<IllegalArgumentException> { c.analytics.log(" ", LogType.INFO) }
        assertFailsWith<IllegalArgumentException> { c.analytics.log("n", LogType.INFO, mapOf("a" to Double.NaN)) }
        assertFailsWith<IllegalArgumentException> { c.analytics.log("n", LogType.INFO, mapOf("a" to listOf(1))) }
        assertFailsWith<IllegalArgumentException> { c.analytics.click(" ") }
        assertFailsWith<IllegalArgumentException> { c.analytics.screen(params = mapOf("log_name" to "x")) }
        assertTrue(t.calls.isEmpty())
    }
}
