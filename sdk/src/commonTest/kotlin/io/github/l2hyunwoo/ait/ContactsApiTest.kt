package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class ContactsApiTest {
    @Test fun sendsPageAndDecodesContacts() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        t.reply = """{"permission":"allowed","value":{"result":[{"name":"김","phoneNumber":"010"}],"nextOffset":10,"done":false}}"""
        assertEquals(PermissionResult.Granted(ContactResult(listOf(ContactEntity("김", "010")), 10, false)), c.contacts.getContacts(10, 0, contains = "김"))
        assertEquals("contactsGet" to """{"size":10,"offset":0,"contains":"김"}""", t.calls.last())
        t.reply = """{"permission":"allowed","value":{"result":[],"nextOffset":null,"done":true}}"""
        assertEquals(PermissionResult.Granted(ContactResult(emptyList(), null, true)), c.contacts.getContacts(10, 10))
        assertEquals("""{"size":10,"offset":10}""", t.calls.last().second)
        t.reply = """{"permission":"denied"}"""
        assertEquals(PermissionResult.Denied, c.contacts.getContacts(10, 0))
    }

    @Test fun rejectsInvalidInputAndResponses() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        assertFailsWith<IllegalArgumentException> { c.contacts.getContacts(0, 0) }
        assertFailsWith<IllegalArgumentException> { c.contacts.getContacts(10, -1) }
        assertTrue(t.calls.isEmpty())
        t.reply = """{"permission":"allowed","value":{"result":[],"nextOffset":"10","done":false}}"""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.contacts.getContacts(10, 0) }.code)
    }
}
