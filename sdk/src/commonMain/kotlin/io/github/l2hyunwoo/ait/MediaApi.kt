package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class MediaApi internal constructor(private val client: AppsInTossClient) {
    /** 앨범에서 사진을 읽는다. [base64]를 켜면 `dataUri`에 base64 데이터가 담긴다. [Capability.PHOTOS]가 필요하다. */
    suspend fun getPhotos(maxCount: Int = 10, maxWidth: Int = 1024, base64: Boolean = false): PermissionResult<List<ImageResponse>> {
        require(maxCount > 0 && maxWidth > 0)
        val result = client.call(
            "photosGet",
            buildJsonObject {
                put("maxCount", maxCount)
                put("maxWidth", maxWidth)
                put("base64", base64)
            },
        )
        return decode("photos") { Json.parseToJsonElement(result).jsonObject.permissionResult { it.jsonArray.map { image -> image.jsonObject.image() } } }
    }

    /** 사용자가 앨범 항목을 고르게 한다. 선택을 취소하면 빈 목록을 반환한다. [Capability.ALBUM]가 필요하다. */
    suspend fun getAlbumItems(
        types: Set<AlbumItemType> = setOf(AlbumItemType.PHOTO),
        maxCount: Int = 10,
        maxWidth: Int = 1024,
        base64: Boolean = false,
    ): PermissionResult<List<AlbumItemResponse>> {
        require(types.isNotEmpty() && maxCount > 0 && maxWidth > 0)
        val result = client.call(
            "albumItemsGet",
            buildJsonObject {
                putJsonArray("types") { types.forEach { add(it.name) } }
                put("maxCount", maxCount)
                put("maxWidth", maxWidth)
                put("base64", base64)
            },
        )
        return decode("album items") {
            Json.parseToJsonElement(result).jsonObject.permissionResult { value ->
                value.jsonArray.map {
                    val obj = it.jsonObject
                    AlbumItemResponse(obj.string("id"), obj.string("dataUri"), AlbumItemType.valueOf(obj.string("type")))
                }
            }
        }
    }

    /** 카메라를 열고 촬영한 이미지를 반환한다. [Capability.CAMERA]가 필요하다. */
    suspend fun openCamera(base64: Boolean = false, maxWidth: Int = 1024): PermissionResult<ImageResponse> {
        require(maxWidth > 0)
        val result = client.call(
            "cameraOpen",
            buildJsonObject {
                put("maxWidth", maxWidth)
                put("base64", base64)
            },
        )
        return decode("camera") { Json.parseToJsonElement(result).jsonObject.permissionResult { it.jsonObject.image() } }
    }

    private fun JsonObject.image() = ImageResponse(string("id"), string("dataUri"))
}

data class ImageResponse(val id: String, val dataUri: String)
data class AlbumItemResponse(val id: String, val dataUri: String, val type: AlbumItemType)
enum class AlbumItemType { PHOTO, VIDEO }
