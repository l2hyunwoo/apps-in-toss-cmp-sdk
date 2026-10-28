package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

private const val HERE = """{"accessLocation":"FINE","timestamp":1700000000000,"coords":{"latitude":37.5,"longitude":127.0,"altitude":0,"accuracy":5,"altitudeAccuracy":0,"heading":-1}}"""
private val here = Location(AccessLocation.FINE, 1.7e12, LocationCoords(37.5, 127.0, 0.0, 5.0, 0.0, -1.0))

class LocationApiTest {
    @Test fun `위치 조회는 정확도를 보내고 디코딩한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        t.reply = """{"permission":"allowed","value":$HERE}"""
        assertEquals(PermissionResult.Granted(here), c.location.getLocation(Accuracy.BALANCED))
        assertEquals("locationGet" to """{"accuracy":3}""", t.calls.last())
        t.reply = """{"permission":"allowed","value":{"timestamp":1,"coords":{"latitude":1,"longitude":2,"altitude":3,"accuracy":4,"altitudeAccuracy":5,"heading":6}}}"""
        assertNull((c.location.getLocation(Accuracy.BEST_FOR_NAVIGATION) as PermissionResult.Granted).value.accessLocation)
        assertEquals("""{"accuracy":6}""", t.calls.last().second)
        t.reply = """{"permission":"denied"}"""
        assertEquals(PermissionResult.Denied, c.location.getLocation(Accuracy.LOWEST))
        t.reply = """{"permission":"allowed","value":{"timestamp":1,"coords":{"latitude":"1"}}}"""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.location.getLocation(Accuracy.HIGH) }.code)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `위치 구독은 업데이트를 방출하고 취소하면 정리한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        val events = mutableListOf<PermissionResult<Location>>()
        val job = launch { c.location.subscribeLocation(Accuracy.HIGH, 1000, 10.0).collect { events += it } }
        runCurrent()
        assertEquals("locationSubscribe" to """{"accuracy":4,"timeInterval":1000,"distanceInterval":10.0}""", t.streams.single())
        t.onEvent("""{"permission":"allowed","value":$HERE}""")
        t.onEvent("""{"permission":"allowed","value":$HERE}""")
        runCurrent()
        assertEquals(listOf<PermissionResult<Location>>(PermissionResult.Granted(here), PermissionResult.Granted(here)), events)
        job.cancelAndJoin()
        assertEquals(1, t.cleaned)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `위치 구독은 권한이 거부되면 완료된다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        val events = async { c.location.subscribeLocation(Accuracy.BALANCED, 0, 0.0).toList() }
        runCurrent()
        t.onEvent("""{"permission":"allowed","value":$HERE}""")
        t.onEvent("""{"permission":"denied"}""")
        assertEquals(listOf(PermissionResult.Granted(here), PermissionResult.Denied), events.await())
        assertEquals(1, t.cleaned)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `위치 구독은 잘못된 입력과 이벤트를 거부한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        assertFailsWith<IllegalArgumentException> { c.location.subscribeLocation(Accuracy.HIGH, -1, 0.0) }
        assertFailsWith<IllegalArgumentException> { c.location.subscribeLocation(Accuracy.HIGH, 0, Double.NaN) }
        val failed = async { runCatching { c.location.subscribeLocation(Accuracy.HIGH, 0, 0.0).toList() }.exceptionOrNull() }
        runCurrent()
        t.onEvent("""{"permission":"allowed","value":{"coords":{}}}""")
        assertEquals(ErrorCode.INVALID_RESPONSE, (failed.await() as AppsInTossException).code)
        assertEquals(1, t.cleaned)
    }
}
