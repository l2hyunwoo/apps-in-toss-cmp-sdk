package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class ClipboardApiTest {
    private val transport = FakeTransport()
    private val clipboard = AppsInTossClient(transport).clipboard

    @Test fun textAndWritesDecode() = runTest {
        transport.reply = """{"text":"copied"}"""
        assertEquals(ClipboardText.Text("copied"), clipboard.getText())
        transport.reply = "{}"
        assertEquals(ClipboardWriteResult.SUCCESS, clipboard.setText("t"))
        assertEquals(listOf("clipboardGetText" to "{}", "clipboardSetText" to """{"text":"t"}"""), transport.calls)
    }

    @Test fun permissionDenialIsAResult() = runTest {
        transport.reply = """{"permission":"denied"}"""
        assertEquals(ClipboardText.PermissionDenied, clipboard.getText())
        assertEquals(ClipboardWriteResult.PERMISSION_DENIED, clipboard.setText("t"))
    }

    @Test fun malformedResponsesAreInvalid() = runTest {
        for (reply in listOf("{}", """{"text":1}""", """{"permission":"granted"}""", "null")) {
            transport.reply = reply
            assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { clipboard.getText() }.code, reply)
        }
        transport.reply = """{"permission":"granted"}"""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { clipboard.setText("t") }.code)
    }
}
