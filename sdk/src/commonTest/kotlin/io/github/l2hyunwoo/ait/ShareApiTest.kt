package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class ShareApiTest {
    @Test fun `공유 링크를 만들고 잘못된 입력과 응답을 거부한다`() = runTest {
        val t = FakeTransport().apply { reply = "\"https://toss.im/l\"" }
        val c = AppsInTossClient(t)
        assertEquals("https://toss.im/l", c.share.createLink("intoss://app"))
        assertEquals("shareLink" to """{"path":"intoss://app"}""", t.calls.last())
        c.share.createLink("intoss://app/about", ogImageUrl = "https://img")
        assertEquals("""{"path":"intoss://app/about","ogImageUrl":"https://img"}""", t.calls.last().second)
        t.reply = "null"
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.share.createLink("intoss://app") }.code)
        val sent = t.calls.size
        assertFailsWith<IllegalArgumentException> { c.share.createLink("https://app") }
        assertFailsWith<IllegalArgumentException> { c.share.createLink("intoss://app", ogImageUrl = " ") }
        assertEquals(sent, t.calls.size)
    }
}
