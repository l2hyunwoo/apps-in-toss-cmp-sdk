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
    @Test fun `정보 overlay가 사라지면 방출하고 정리한다`() = runTest {
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

    @Test fun `취소하면 cleanup을 실행한다`() = runTest {
        val t = FakeTransport()
        val job = async { AppsInTossClient(t).game.subscribeInfoOverlayHidden().toList() }
        runCurrent()
        job.cancelAndJoin()
        assertEquals(1, t.cleaned)
    }

    @Test fun `bridge 실패는 Flow를 끝낸다`() = runTest {
        val t = FakeTransport().apply { streamStarted = { streamFailure("""{"code":"UNSUPPORTED"}""") } }
        assertEquals(ErrorCode.UNSUPPORTED, assertFailsWith<AppsInTossException> { AppsInTossClient(t).game.subscribeInfoOverlayHidden().toList() }.code)
        assertEquals(1, t.cleaned)
    }
}
