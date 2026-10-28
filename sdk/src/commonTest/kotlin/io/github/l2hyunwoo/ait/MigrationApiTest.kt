package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class MigrationApiTest {
    private fun dump(origin: String) = """
        {"origin":"$origin","localStorage":{"a":"1","b":null},
         "indexedDB":[{"name":"db","version":2,"objectStores":[{"name":"s","keyPath":["a","b"],"autoIncrement":false,
           "indexes":[{"name":"i","keyPath":"x","multiEntry":false,"unique":true}],
           "records":[{"key":1,"primaryKey":1,"value":{"n":[1,"x",null]}}]},
           {"name":"t","keyPath":null,"autoIncrement":true,"indexes":[],"records":[]}]}],
         "opfs":{"directories":["d"],"files":[{"path":"d/f.txt","type":"text/plain","lastModified":1700000000000,"data":"aGk="}]},
         "errors":[{"storage":"opfs","message":"m"}]}
    """.trimIndent()

    @Test fun getOriginStorageDecodesDumps() = runTest {
        val t = FakeTransport().apply { reply = """{"previous":${dump("https://web.tossmini.com")},"current":${dump("https://apps.tossmini.com")}}""" }
        val storage = AppsInTossClient(t).migration.getOriginStorage()
        assertEquals("migrationOriginStorage" to "{}", t.calls.single())
        assertEquals("https://web.tossmini.com", storage.previous.origin)
        val current = storage.current
        assertEquals(mapOf("a" to "1", "b" to null), current.localStorage)
        val db = current.indexedDB.single()
        assertEquals(2L, db.version)
        val (store, outOfLine) = db.objectStores
        assertEquals("""["a","b"]""", store.keyPathJson)
        assertNull(outOfLine.keyPathJson)
        assertTrue(outOfLine.autoIncrement)
        assertEquals(IndexedDbIndex("i", "\"x\"", multiEntry = false, unique = true), store.indexes.single())
        assertEquals(IndexedDbRecord("1", "1", """{"n":[1,"x",null]}"""), store.records.single())
        assertEquals(OpfsSnapshot(listOf("d"), listOf(OpfsFile("d/f.txt", "text/plain", 1700000000000, "aGk="))), current.opfs)
        assertEquals(listOf(OriginStorageError("opfs", "m")), current.errors)
    }

    @Test fun getOriginStorageRejectsMalformedResponses() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        val valid = dump("o")
        for (reply in listOf("null", """{"previous":$valid}""", """{"previous":$valid,"current":${valid.replace("\"version\":2", "\"version\":\"2\"")}}""")) {
            t.reply = reply
            assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.migration.getOriginStorage() }.code, reply)
        }
    }
}
