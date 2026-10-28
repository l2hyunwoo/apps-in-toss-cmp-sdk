package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class PermissionsApiTest {
    @Test fun `권한을 보내고 상태를 디코딩한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        t.reply = "\"notDetermined\""
        assertEquals(PermissionStatus.NOT_DETERMINED, c.permissions.getPermission(Permission.GEOLOCATION))
        assertEquals("permissionGet" to """{"name":"geolocation","access":"access"}""", t.calls.last())
        t.reply = "\"denied\""
        assertEquals(PermissionDialogResult.DENIED, c.permissions.requestPermission(Permission.PHOTOS_READ))
        assertEquals("permissionRequest" to """{"name":"photos","access":"read"}""", t.calls.last())
        t.reply = "\"allowed\""
        assertEquals(PermissionDialogResult.ALLOWED, c.permissions.openPermissionDialog(Permission.CLIPBOARD_WRITE))
        assertEquals("permissionDialog" to """{"name":"clipboard","access":"write"}""", t.calls.last())
    }

    @Test fun `형식이 잘못된 상태를 거부한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        for (reply in listOf("\"notDetermined\"", "\"ALLOWED\"", "1")) {
            t.reply = reply
            assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.permissions.requestPermission(Permission.CAMERA) }.code)
        }
        t.reply = "\"granted\""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.permissions.getPermission(Permission.CAMERA) }.code)
    }
}
