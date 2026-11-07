package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.*

class NavigationBarApiTest {
    @Test fun `넘긴 옵션만 보낸다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        c.navigationBar.setOptions(theme = NavigationBarTheme.DARK)
        assertEquals("navigationBarOptions" to """{"theme":"dark"}""", t.calls.last())
        c.navigationBar.setOptions(withBackButton = false, withHomeButton = true, withTitle = true, transparentBackground = false, backgroundColor = NavigationBarBackground.Color("#3182F6"))
        assertEquals("""{"withBackButton":false,"withHomeButton":true,"withTitle":true,"transparentBackground":false,"backgroundColor":"#3182F6"}""", t.calls.last().second)
        c.navigationBar.setOptions(backgroundColor = NavigationBarBackground.ThemeDefault)
        assertEquals("""{"backgroundColor":null}""", t.calls.last().second)
    }

    @Test fun `잘못된 색을 거부한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        for (hex in listOf("red", "#FFF", "3182F6", "#3182F6A")) {
            assertFailsWith<IllegalArgumentException>(hex) { c.navigationBar.setOptions(backgroundColor = NavigationBarBackground.Color(hex)) }
        }
        assertTrue(t.calls.isEmpty())
    }

    @Test fun `액세서리 버튼을 추가하고 제거한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        c.navigationBar.addAccessoryButton("찜하기", "icon-heart-mono")
        c.navigationBar.removeAccessoryButton()
        assertEquals(
            listOf(
                "navigationBarAccessoryAdd" to """{"accessibilityLabel":"찜하기","icon":{"name":"icon-heart-mono"}}""",
                "navigationBarAccessoryRemove" to "{}",
            ),
            t.calls,
        )
    }

    @Test fun `액세서리 버튼의 빈 라벨과 빈 아이콘 이름을 거부한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        assertFailsWith<IllegalArgumentException> { c.navigationBar.addAccessoryButton(" ", "icon-heart-mono") }
        assertFailsWith<IllegalArgumentException> { c.navigationBar.addAccessoryButton("찜하기", "") }
        assertTrue(t.calls.isEmpty())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `액세서리 버튼 클릭을 받고 오류로 끝나며 구독을 한 번 해제한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        val clicks = mutableListOf<Unit>()
        val result = async { runCatching { c.navigationBar.accessoryButtonClicks().collect { clicks += it } }.exceptionOrNull() }
        runCurrent()
        assertEquals("navigationBarAccessoryClicks" to "{}", t.streams.single())
        t.onEvent("{}")
        t.onEvent("{}")
        runCurrent()
        t.streamFailure("""{"code":"SDK_FAILURE"}""")
        val error = result.await()
        assertIs<AppsInTossException>(error)
        assertEquals(ErrorCode.SDK_FAILURE, error.code)
        assertEquals(2, clicks.size)
        assertEquals(1, t.cleaned)
    }

    @Test fun `액세서리 버튼 클릭을 하나만 받으면 구독을 한 번 해제한다`() = runTest {
        val t = FakeTransport().apply { streamStarted = { onEvent("{}") } }
        AppsInTossClient(t).navigationBar.accessoryButtonClicks().first()
        assertEquals(1, t.cleaned)
    }

    @Test fun `액세서리 버튼을 지원하지 않으면 클릭 구독이 UNSUPPORTED로 끝난다`() = runTest {
        val t = FakeTransport().apply { streamStarted = { streamFailure("""{"code":"UNSUPPORTED"}""") } }
        val error = assertFailsWith<AppsInTossException> { AppsInTossClient(t).navigationBar.accessoryButtonClicks().toList() }
        assertEquals(ErrorCode.UNSUPPORTED, error.code)
        assertEquals(1, t.cleaned)
    }
}
