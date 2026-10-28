package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class NavigationBarApiTest {
    @Test fun sendsOnlyGivenOptions() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        c.navigationBar.setOptions(theme = NavigationBarTheme.DARK)
        assertEquals("navigationBarOptions" to """{"theme":"dark"}""", t.calls.last())
        c.navigationBar.setOptions(withBackButton = false, withHomeButton = true, withTitle = true, transparentBackground = false, backgroundColor = NavigationBarBackground.Color("#3182F6"))
        assertEquals("""{"withBackButton":false,"withHomeButton":true,"withTitle":true,"transparentBackground":false,"backgroundColor":"#3182F6"}""", t.calls.last().second)
        c.navigationBar.setOptions(backgroundColor = NavigationBarBackground.ThemeDefault)
        assertEquals("""{"backgroundColor":null}""", t.calls.last().second)
    }

    @Test fun rejectsInvalidColor() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        for (hex in listOf("red", "#FFF", "3182F6", "#3182F6A")) {
            assertFailsWith<IllegalArgumentException>(hex) { c.navigationBar.setOptions(backgroundColor = NavigationBarBackground.Color(hex)) }
        }
        assertTrue(t.calls.isEmpty())
    }
}
