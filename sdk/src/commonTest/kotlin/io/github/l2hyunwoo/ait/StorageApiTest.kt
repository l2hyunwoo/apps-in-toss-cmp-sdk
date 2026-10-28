package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class StorageApiTest {
    private val transport = FakeTransport()
    private val storage = AppsInTossClient(transport).storage

    @Test fun `일괄 호출은 payload를 보내고 디코딩한다`() = runTest {
        transport.reply = """{"a":"1","b":null}"""
        assertEquals(mapOf("a" to "1", "b" to null), storage.getItems(listOf("a", "b")))
        transport.reply = "null"
        storage.setItems(mapOf("a" to "1"))
        storage.clearItems()
        assertEquals(
            listOf("storageGetItems" to """{"keys":["a","b"]}""", "storageSetItems" to """{"items":{"a":"1"}}""", "storageClear" to "{}"),
            transport.calls,
        )
    }

    @Test fun `형식이 잘못된 항목은 INVALID_RESPONSE다`() = runTest {
        for (reply in listOf("""{"a":1}""", """{}""", "null")) {
            transport.reply = reply
            assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { storage.getItems(listOf("a")) }.code, reply)
        }
    }

    @Test fun `빈 키는 호출 전에 거부한다`() = runTest {
        assertFailsWith<IllegalArgumentException> { storage.getItems(listOf("a", " ")) }
        assertFailsWith<IllegalArgumentException> { storage.setItems(mapOf("" to "v")) }
        assertTrue(transport.calls.isEmpty())
    }
}
