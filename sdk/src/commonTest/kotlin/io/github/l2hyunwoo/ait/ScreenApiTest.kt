package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class ScreenApiTest {
    @Test fun settersSendOfficialKeys() = runTest {
        val t = FakeTransport().apply { reply = """{"enabled":true}""" }
        val c = AppsInTossClient(t)
        assertTrue(c.screen.setAwakeMode(true))
        assertEquals("screenAwakeMode" to """{"enabled":true}""", t.calls.last())
        t.reply = """{"enabled":false}"""
        assertFalse(c.screen.setSecure(false))
        assertEquals("screenSecure" to """{"enabled":false}""", t.calls.last())
        t.reply = "null"
        c.screen.setIosSwipeBack(false)
        assertEquals("screenIosSwipeBack" to """{"isEnabled":false}""", t.calls.last())
        c.screen.setOrientation(DeviceOrientation.LANDSCAPE)
        assertEquals("screenOrientation" to """{"type":"landscape"}""", t.calls.last())
    }

    @Test fun malformedStateIsInvalid() = runTest {
        val c = AppsInTossClient(FakeTransport().apply { reply = "{}" })
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.screen.setAwakeMode(true) }.code)
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.screen.setSecure(true) }.code)
    }
}
