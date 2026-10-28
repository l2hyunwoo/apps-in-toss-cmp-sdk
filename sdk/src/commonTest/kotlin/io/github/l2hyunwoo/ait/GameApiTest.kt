package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class GameApiTest {
    @Test fun infoOverlayHiddenEmitsAndCleansUp() = runTest {
        val t = FakeTransport().apply {
            streamStarted = {
                onEvent("{}")
                onEvent("{}")
            }
        }
        assertEquals(listOf(Unit, Unit), AppsInTossClient(t).game.subscribeInfoOverlayHidden().take(2).toList())
        assertEquals("gameInfoOverlayHidden" to "{}", t.streams.single())
        assertEquals(1, t.cleaned)
    }

    @Test fun cancellationRunsCleanup() = runTest {
        val t = FakeTransport()
        val job = async { AppsInTossClient(t).game.subscribeInfoOverlayHidden().toList() }
        runCurrent()
        job.cancelAndJoin()
        assertEquals(1, t.cleaned)
    }

    @Test fun bridgeFailureEndsFlow() = runTest {
        val t = FakeTransport().apply { streamStarted = { streamFailure("""{"code":"UNSUPPORTED"}""") } }
        assertEquals(ErrorCode.UNSUPPORTED, assertFailsWith<AppsInTossException> { AppsInTossClient(t).game.subscribeInfoOverlayHidden().toList() }.code)
        assertEquals(1, t.cleaned)
    }
}
