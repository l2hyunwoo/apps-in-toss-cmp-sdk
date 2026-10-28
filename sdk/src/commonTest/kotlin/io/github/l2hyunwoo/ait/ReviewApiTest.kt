package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class ReviewApiTest {
    @Test fun `리뷰 요청은 bridge를 호출한다`() = runTest {
        val t = FakeTransport()
        AppsInTossClient(t).review.request()
        assertEquals("reviewRequest" to "{}", t.calls.single())
    }
}
