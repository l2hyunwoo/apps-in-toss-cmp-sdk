package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class ReviewApiTest {
    @Test fun requestCallsBridge() = runTest {
        val t = FakeTransport()
        AppsInTossClient(t).review.request()
        assertEquals("reviewRequest" to "{}", t.calls.single())
    }
}
