package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.*

/** Fake bridge: records calls and lets tests complete them or push stream events. */
internal class FakeTransport(override val host: RuntimeHost = RuntimeHost.SANDBOX) : AppsInTossTransport {
    val calls = mutableListOf<Pair<String, String>>()
    var reply: String? = "null"
    var success: (String) -> Unit = {}
    var failure: (String) -> Unit = {}
    var cleaned = 0
    var onEvent: (String) -> Unit = {}
    var streamFailure: (String) -> Unit = {}
    var streamStarted: (String) -> Unit = {}
    val streams = mutableListOf<Pair<String, String>>()

    override fun call(method: String, payload: String, success: (String) -> Unit, failure: (String) -> Unit): () -> Unit {
        calls += method to payload
        this.success = success
        this.failure = failure
        reply?.let(success)
        return { cleaned++ }
    }

    override fun stream(method: String, payload: String, onEvent: (String) -> Unit, failure: (String) -> Unit): () -> Unit {
        streams += method to payload
        this.onEvent = onEvent
        streamFailure = failure
        streamStarted(method)
        return { cleaned++ }
    }
}

class ClientTest {
    @Test fun unsupported() = runTest {
        val client = AppsInTossClient(FakeTransport(RuntimeHost.BROWSER))
        assertFalse(client.supports(Capability.TOSS_LOGIN))
        assertEquals(ErrorCode.UNSUPPORTED, assertFailsWith<AppsInTossException> { client.auth.login() }.code)
        assertEquals(ErrorCode.UNSUPPORTED, assertFailsWith<AppsInTossException> { client.game.openLeaderboard() }.code)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun loginValidationConcurrencyAndCleanup() = runTest {
        val transport = FakeTransport().apply { reply = null }
        val client = AppsInTossClient(transport)
        val first = async { client.auth.login() }
        runCurrent()
        assertEquals(ErrorCode.IN_PROGRESS, assertFailsWith<AppsInTossException> { client.auth.login() }.code)
        transport.success("""{"authorizationCode":"code","referrer":"SANDBOX"}""")
        assertEquals("code", first.await().authorizationCode)
        assertEquals(1, transport.cleaned)
        val invalid = async { runCatching { client.auth.login() }.exceptionOrNull() }
        runCurrent()
        transport.success("""{"authorizationCode":null,"referrer":"SANDBOX"}""")
        assertEquals(ErrorCode.INVALID_RESPONSE, (invalid.await() as AppsInTossException).code)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cancellationTimeoutCloseAndLateResults() = runTest {
        val t = FakeTransport().apply { reply = null }
        val c = AppsInTossClient(t, 10)
        val cancelled = async { c.auth.login() }
        runCurrent()
        cancelled.cancelAndJoin()
        t.success("ignored")
        assertEquals(1, t.cleaned)
        val timed = async { runCatching { c.auth.login() }.exceptionOrNull() }
        runCurrent()
        advanceTimeBy(11)
        runCurrent()
        assertEquals(ErrorCode.TIMEOUT, (timed.await() as AppsInTossException).code)
        val closed = async { runCatching { c.auth.login() }.exceptionOrNull() }
        runCurrent()
        c.close()
        c.close()
        assertEquals(ErrorCode.HOST_UNAVAILABLE, (closed.await() as AppsInTossException).code)
        assertFalse(c.supports(Capability.SHARE))
        assertEquals(3, t.cleaned)
    }

    @Test fun userKeyIsStrictlyValidated() = runTest {
        val t = FakeTransport()
        val client = AppsInTossClient(t)
        t.reply = """{"type":"HASH","hash":"user-key"}"""
        assertEquals("user-key", client.user.getAnonymousKey())
        t.reply = """{"type":"HASH","hash":123}"""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { client.user.getAnonymousKey() }.code)
    }

    @Test fun enclosingTimeoutRemainsCancellation() = runTest {
        val c = AppsInTossClient(FakeTransport().apply { reply = null }, 10_000)
        assertNull(withTimeoutOrNull(1) { c.auth.login() })
    }

    @Test fun eventsDecodeAndRemoveListener() = runTest {
        val t = FakeTransport().apply { streamStarted = { onEvent("""{"type":"back"}""") } }
        assertEquals(HostEvent.Back, AppsInTossClient(t).events.first())
        assertEquals("hostEvents", t.streams.single().first)
        assertEquals(1, t.cleaned)
    }

    @Test fun invalidEventFailsStream() = runTest {
        val t = FakeTransport().apply { streamStarted = { onEvent("""{"type":"safeArea","top":"1"}""") } }
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { AppsInTossClient(t).events.first() }.code)
    }

    @Test fun migratedApisSendPayloadsAndDecodeResponses() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        c.share.sendMessage("hi")
        assertEquals("share" to """{"message":"hi"}""", t.calls.last())
        c.screen.close()
        assertEquals("closeScreen", t.calls.last().first)
        c.device.triggerHaptic(HapticType.SUCCESS)
        assertEquals("haptic" to """{"type":"success"}""", t.calls.last())

        assertNull(c.storage.getItem("record"))
        t.reply = """"{\"wins\":1}""""
        assertEquals("""{"wins":1}""", c.storage.getItem("record"))
        c.storage.setItem("record", "v")
        assertEquals("storageSet" to """{"key":"record","value":"v"}""", t.calls.last())
        c.storage.removeItem("record")
        assertEquals("storageRemove", t.calls.last().first)

        t.reply = """{"statusCode":"SUCCESS","nickname":"nunu","profileImageUri":"https://img"}"""
        assertEquals(GameProfile.Found("nunu", "https://img"), c.game.getUserProfile())
        t.reply = """{"statusCode":"PROFILE_NOT_FOUND"}"""
        assertEquals(GameProfile.NotFound, c.game.getUserProfile())

        t.reply = """{"statusCode":"LEADERBOARD_NOT_FOUND"}"""
        assertEquals(LeaderboardSubmitResult.LEADERBOARD_NOT_FOUND, c.game.setLeaderboardScore(3.0))
        assertEquals("leaderboardSubmit", t.calls.last().first)
        c.game.openLeaderboard()
        assertEquals("leaderboardOpen", t.calls.last().first)
    }

    @Test fun migratedApisRejectInvalidInputAndResponses() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        assertFailsWith<IllegalArgumentException> { c.storage.getItem(" ") }
        assertFailsWith<IllegalArgumentException> { c.game.setLeaderboardScore(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { c.share.sendMessage(" ") }
        assertTrue(t.calls.isEmpty())
        t.reply = "1"
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.storage.getItem("k") }.code)
        t.reply = """{"statusCode":"UNKNOWN"}"""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.game.setLeaderboardScore(1.0) }.code)
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.game.getUserProfile() }.code)
    }

    @Test fun bridgeFailureCodesAreWhitelisted() = runTest {
        val t = FakeTransport().apply { reply = null }
        val c = AppsInTossClient(t)
        val result = async { runCatching { c.screen.close() }.exceptionOrNull() }
        testScheduler.runCurrent()
        t.failure("""{"code":"PERMISSION_DENIED"}""")
        assertEquals(ErrorCode.PERMISSION_DENIED, (result.await() as AppsInTossException).code)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun streamRepliesToHostQuestions() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        val answers = ArrayDeque(listOf<() -> JsonElement>({ JsonPrimitive(true) }, { error("grant failed") }))
        val events = async {
            c.stream("probe", answer = { answers.removeFirst()() }).first()
        }
        runCurrent()
        t.onEvent("""{"type":"grant","orderId":"o1","replyId":"r1"}""")
        t.onEvent("""{"type":"grant","orderId":"o2","replyId":"r2"}""")
        runCurrent()
        assertEquals(listOf("""{"replyId":"r1","value":true}""", """{"replyId":"r2","value":null}"""), t.calls.filter { it.first == "reply" }.map { it.second })
        t.onEvent("""{"type":"success"}""")
        assertEquals("success", events.await().string("type"))
        assertEquals(1 + 2, t.cleaned)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun closeEndsOpenStreams() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        val collected = async { runCatching { c.stream("probe").toList() }.exceptionOrNull() }
        runCurrent()
        c.close()
        assertEquals(ErrorCode.HOST_UNAVAILABLE, (collected.await() as AppsInTossException).code)
        assertEquals(1, t.cleaned)
    }
}
