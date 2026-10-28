package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class MediaApiTest {
    @Test fun `옵션을 보내고 이미지를 디코딩한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        t.reply = """{"permission":"allowed","value":[{"id":"p1","dataUri":"aGk="}]}"""
        assertEquals(PermissionResult.Granted(listOf(ImageResponse("p1", "aGk="))), c.media.getPhotos(maxCount = 1, base64 = true))
        assertEquals("photosGet" to """{"maxCount":1,"maxWidth":1024,"base64":true}""", t.calls.last())

        t.reply = """{"permission":"allowed","value":[{"id":"v1","dataUri":"file://v","type":"VIDEO"}]}"""
        assertEquals(
            PermissionResult.Granted(listOf(AlbumItemResponse("v1", "file://v", AlbumItemType.VIDEO))),
            c.media.getAlbumItems(types = setOf(AlbumItemType.PHOTO, AlbumItemType.VIDEO)),
        )
        assertEquals("albumItemsGet" to """{"types":["PHOTO","VIDEO"],"maxCount":10,"maxWidth":1024,"base64":false}""", t.calls.last())

        t.reply = """{"permission":"allowed","value":{"id":"c1","dataUri":"file://c"}}"""
        assertEquals(PermissionResult.Granted(ImageResponse("c1", "file://c")), c.media.openCamera(maxWidth = 512))
        assertEquals("cameraOpen" to """{"maxWidth":512,"base64":false}""", t.calls.last())
    }

    @Test fun `거부된 권한은 결과로 돌아온다`() = runTest {
        val t = FakeTransport().apply { reply = """{"permission":"denied"}""" }
        val c = AppsInTossClient(t)
        assertEquals(PermissionResult.Denied, c.media.getPhotos())
        assertEquals(PermissionResult.Denied, c.media.getAlbumItems())
        assertEquals(PermissionResult.Denied, c.media.openCamera())
    }

    @Test fun `잘못된 입력과 응답을 거부한다`() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        assertFailsWith<IllegalArgumentException> { c.media.getPhotos(maxCount = 0) }
        assertFailsWith<IllegalArgumentException> { c.media.getAlbumItems(types = emptySet()) }
        assertFailsWith<IllegalArgumentException> { c.media.openCamera(maxWidth = -1) }
        assertTrue(t.calls.isEmpty())
        for (reply in listOf("""{"permission":"allowed","value":[{"id":"p1"}]}""", """{"permission":"unknown"}""", "null")) {
            t.reply = reply
            assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.media.getPhotos() }.code)
        }
        t.reply = """{"permission":"allowed","value":[{"id":"a","dataUri":"d","type":"AUDIO"}]}"""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.media.getAlbumItems() }.code)
    }
}
