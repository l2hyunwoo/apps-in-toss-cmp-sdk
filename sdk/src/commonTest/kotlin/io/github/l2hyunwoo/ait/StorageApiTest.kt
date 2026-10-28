package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class StorageApiTest {
    private val transport = FakeTransport()
    private val storage = AppsInTossClient(transport).storage

    @Test fun batchCallsSendPayloadsAndDecode() = runTest {
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

    @Test fun malformedItemsAreInvalid() = runTest {
        for (reply in listOf("""{"a":1}""", """{}""", "null")) {
            transport.reply = reply
            assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { storage.getItems(listOf("a")) }.code, reply)
        }
    }

    @Test fun blankKeysAreRejectedBeforeCalling() = runTest {
        assertFailsWith<IllegalArgumentException> { storage.getItems(listOf("a", " ")) }
        assertFailsWith<IllegalArgumentException> { storage.setItems(mapOf("" to "v")) }
        assertTrue(transport.calls.isEmpty())
    }
}
