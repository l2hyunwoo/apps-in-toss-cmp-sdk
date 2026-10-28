package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class FileApiTest {
    @Test fun `PDF 뷰어는 닫힐 때까지 기다리고 정리한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        val viewer = async { c.file.openPDFViewer("JVBERi0=", "doc.pdf") }
        runCurrent()
        assertEquals("pdfViewer" to """{"data":"JVBERi0=","filename":"doc.pdf"}""", t.streams.single())
        assertFalse(viewer.isCompleted)
        t.onEvent("""{"type":"closed"}""")
        viewer.await()
        assertEquals(1, t.cleaned)

        val cancelled = async { c.file.openPDFViewer("JVBERi0=") }
        runCurrent()
        assertEquals("""{"data":"JVBERi0="}""", t.streams.last().second)
        cancelled.cancelAndJoin()
        assertEquals(2, t.cleaned)
    }

    @Test fun `PDF 뷰어는 잘못된 입력과 이벤트를 거부한다`() = runTest {
        val t = FakeTransport().apply { streamStarted = { onEvent("""{"type":"open"}""") } }
        val c = AppsInTossClient(t)
        assertFailsWith<IllegalArgumentException> { c.file.openPDFViewer(" ") }
        assertFailsWith<IllegalArgumentException> { c.file.openPDFViewer("JVBERi0=", "") }
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.file.openPDFViewer("JVBERi0=") }.code)
    }

    @Test fun `base64 저장은 데이터를 그대로 보낸다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        c.file.saveBase64("iVBORw0KGgo=", "photo.png", "image/png")
        assertEquals("fileSaveBase64" to """{"data":"iVBORw0KGgo=","fileName":"photo.png","mimeType":"image/png"}""", t.calls.single())
        assertFailsWith<IllegalArgumentException> { c.file.saveBase64("iVBORw0KGgo=", "photo.png", " ") }
        assertEquals(1, t.calls.size)
    }
}
