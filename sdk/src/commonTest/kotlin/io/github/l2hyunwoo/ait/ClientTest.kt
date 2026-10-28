package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*

class ClientTest {
    private class Transport(override val host: RuntimeHost = RuntimeHost.SANDBOX) : AppsInTossTransport {
        lateinit var success: (String) -> Unit
        var cleaned = 0
        override fun login(success: (String) -> Unit, failure: (String) -> Unit): () -> Unit {
            this.success = success
            return { cleaned++ }
        }
        override fun anonymousKey(success: (String) -> Unit, failure: (String) -> Unit) = login(success, failure)
        override fun share(text: String, success: (String) -> Unit, failure: (String) -> Unit) = login(success, failure)
        override fun closeScreen(success: (String) -> Unit, failure: (String) -> Unit) = login(success, failure)
        override fun subscribe(listener: (String) -> Unit, failure: (String) -> Unit): () -> Unit {
            listener("""{"type":"back"}""")
            return { cleaned++ }
        }
        val calls = mutableListOf<Pair<String, String>>()
        var reply = "null"
        override fun call(method: String, payload: String, success: (String) -> Unit, failure: (String) -> Unit): () -> Unit {
            calls += method to payload
            success(reply)
            return { cleaned++ }
        }
    }

    private class LegacyTransport : AppsInTossTransport {
        override val host = RuntimeHost.SANDBOX
        override fun login(success: (String) -> Unit, failure: (String) -> Unit): () -> Unit = {}
        override fun anonymousKey(success: (String) -> Unit, failure: (String) -> Unit): () -> Unit = {}
        override fun share(text: String, success: (String) -> Unit, failure: (String) -> Unit): () -> Unit = {}
        override fun closeScreen(success: (String) -> Unit, failure: (String) -> Unit): () -> Unit = {}
        override fun subscribe(listener: (String) -> Unit, failure: (String) -> Unit): () -> Unit = {}
    }

    @Test fun unsupported() = runTest {
        val client = AppsInTossClient(Transport(RuntimeHost.BROWSER))
        assertFalse(client.supports(Capability.TOSS_LOGIN))
        assertEquals(ErrorCode.UNSUPPORTED, assertFailsWith<AppsInTossException> { client.requestTossLogin() }.code)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun loginValidationConcurrencyAndCleanup() = runTest {
        val transport = Transport()
        val client = AppsInTossClient(transport)
        val first = async { client.requestTossLogin() }
        runCurrent()
        assertEquals(ErrorCode.IN_PROGRESS, assertFailsWith<AppsInTossException> { client.requestTossLogin() }.code)
        transport.success("""{"authorizationCode":"code","referrer":"SANDBOX"}""")
        assertEquals("code", first.await().authorizationCode)
        assertEquals(1, transport.cleaned)
        val invalid = async { runCatching { client.requestTossLogin() }.exceptionOrNull() }
        runCurrent()
        transport.success("""{"authorizationCode":null,"referrer":"SANDBOX"}""")
        assertEquals(ErrorCode.INVALID_RESPONSE, (invalid.await() as AppsInTossException).code)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cancellationTimeoutCloseAndLateResults() = runTest {
        val t = Transport()
        val c = AppsInTossClient(t, 10)
        val cancelled = async { c.requestTossLogin() }
        runCurrent()
        cancelled.cancelAndJoin()
        t.success("ignored")
        assertEquals(1, t.cleaned)
        val timed = async { runCatching { c.requestTossLogin() }.exceptionOrNull() }
        runCurrent()
        advanceTimeBy(11)
        runCurrent()
        assertEquals(ErrorCode.TIMEOUT, (timed.await() as AppsInTossException).code)
        val closed = async { runCatching { c.requestTossLogin() }.exceptionOrNull() }
        runCurrent()
        c.close()
        c.close()
        assertEquals(ErrorCode.HOST_UNAVAILABLE, (closed.await() as AppsInTossException).code)
        assertFalse(c.supports(Capability.SHARE))
        assertEquals(3, t.cleaned)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun userKeyIsStrictlyValidated() = runTest {
        val t = Transport()
        val client = AppsInTossClient(t)
        val result = async { client.getAnonymousKey() }
        runCurrent()
        t.success("""{"type":"HASH","hash":"user-key"}""")
        assertEquals("user-key", result.await())
        val invalid = async { runCatching { client.getAnonymousKey() }.exceptionOrNull() }
        runCurrent()
        t.success("""{"type":"HASH","hash":123}""")
        assertEquals(ErrorCode.INVALID_RESPONSE, (invalid.await() as AppsInTossException).code)
    }

    @Test fun enclosingTimeoutRemainsCancellation() = runTest {
        val c = AppsInTossClient(Transport(), 10_000)
        assertNull(withTimeoutOrNull(1) { c.requestTossLogin() })
    }

    @Test fun eventCollectionRemovesListener() = runTest {
        val t = Transport()
        assertEquals(HostEvent.Back, AppsInTossClient(t).events.first())
        assertEquals(1, t.cleaned)
    }

    @Test fun gameApisSendPayloadsAndDecodeResponses() = runTest {
        val t = Transport()
        val c = AppsInTossClient(t)
        c.generateHaptic(HapticType.SUCCESS)
        assertEquals("haptic" to """{"type":"success"}""", t.calls.last())

        assertNull(c.getStorageItem("record"))
        t.reply = """"{\"wins\":1}""""
        assertEquals("""{"wins":1}""", c.getStorageItem("record"))
        c.setStorageItem("record", "v")
        assertEquals("storageSet" to """{"key":"record","value":"v"}""", t.calls.last())
        c.removeStorageItem("record")
        assertEquals("storageRemove", t.calls.last().first)

        t.reply = """{"statusCode":"SUCCESS","nickname":"nunu","profileImageUri":"https://img"}"""
        assertEquals(GameProfile.Found("nunu", "https://img"), c.getGameProfile())
        t.reply = """{"statusCode":"PROFILE_NOT_FOUND"}"""
        assertEquals(GameProfile.NotFound, c.getGameProfile())

        t.reply = """{"statusCode":"LEADERBOARD_NOT_FOUND"}"""
        assertEquals(LeaderboardSubmitResult.LEADERBOARD_NOT_FOUND, c.submitLeaderboardScore(3.0))
        assertEquals("leaderboardSubmit", t.calls.last().first)
        c.openLeaderboard()
        assertEquals("leaderboardOpen", t.calls.last().first)
    }

    @Test fun gameApisRejectInvalidInputAndResponses() = runTest {
        val t = Transport()
        val c = AppsInTossClient(t)
        assertFailsWith<IllegalArgumentException> { c.getStorageItem(" ") }
        assertFailsWith<IllegalArgumentException> { c.submitLeaderboardScore(Double.NaN) }
        assertTrue(t.calls.isEmpty())
        t.reply = "1"
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.getStorageItem("k") }.code)
        t.reply = """{"statusCode":"UNKNOWN"}"""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.submitLeaderboardScore(1.0) }.code)
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.getGameProfile() }.code)
    }

    @Test fun legacyTransportReportsGameApisUnsupported() = runTest {
        val c = AppsInTossClient(LegacyTransport())
        assertEquals(ErrorCode.UNSUPPORTED, assertFailsWith<AppsInTossException> { c.generateHaptic(HapticType.TAP) }.code)
        assertEquals(ErrorCode.UNSUPPORTED, assertFailsWith<AppsInTossException> { AppsInTossClient(Transport(RuntimeHost.BROWSER)).openLeaderboard() }.code)
    }
}
