package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class MediaApi internal constructor(private val client: AppsInTossClient) {
    /** Reads photos from the album; with [base64] `dataUri` holds base64 data. Requires [Capability.PHOTOS]. */
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

    /** Lets the user pick album items; a cancelled picker returns an empty list. Requires [Capability.ALBUM]. */
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

    /** Opens the camera and returns the captured image. Requires [Capability.CAMERA]. */
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
