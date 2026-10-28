package io.github.l2hyunwoo.ait.sample

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlinx.coroutines.test.runTest

class NotesApiClientTest {
    @Test fun `UI나 토스 없이 노트를 디코딩한다`() = runTest {
        val api = NotesApiClient { """[{"id":"1","title":"한글","body":"내용"}]""" }
        assertEquals("한글", api.list().single().title)
    }

    @Test fun `잘못된 서버 데이터를 거부한다`() = runTest {
        assertFails { NotesApiClient { """[{"id":null}]""" }.list() }
    }
}
