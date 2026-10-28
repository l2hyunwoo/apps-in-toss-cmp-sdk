package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class PermissionsApi internal constructor(private val client: AppsInTossClient) {
    /** 권한 요청 창을 띄우지 않고 현재 상태를 읽는다. [Capability.PERMISSIONS]가 필요하다. */
    suspend fun getPermission(permission: Permission): PermissionStatus {
        val result = client.call("permissionGet", permission.payload())
        return decode("permission") { PermissionStatus.decode(Json.parseToJsonElement(result).jsonPrimitive) }
    }

    /** 상태가 정해지지 않았을 때만 권한을 요청한다. [Capability.PERMISSIONS]가 필요하다. */
    suspend fun requestPermission(permission: Permission): PermissionDialogResult {
        val result = client.call("permissionRequest", permission.payload())
        return decode("permission") { PermissionDialogResult.decode(Json.parseToJsonElement(result).jsonPrimitive) }
    }

    /** 거부된 뒤에 다시 요청한다. [Capability.PERMISSIONS]가 필요하다. */
    suspend fun openPermissionDialog(permission: Permission): PermissionDialogResult {
        val result = client.call("permissionDialog", permission.payload())
        return decode("permission") { PermissionDialogResult.decode(Json.parseToJsonElement(result).jsonPrimitive) }
    }

    private fun Permission.payload() = buildJsonObject {
        put("name", permissionName)
        put("access", access)
    }
}

/** 공식 `{name, access}` 조합이다. */
enum class Permission(internal val permissionName: String, internal val access: String) {
    CLIPBOARD_READ("clipboard", "read"),
    CLIPBOARD_WRITE("clipboard", "write"),
    CONTACTS_READ("contacts", "read"),
    CONTACTS_WRITE("contacts", "write"),
    PHOTOS_READ("photos", "read"),
    PHOTOS_WRITE("photos", "write"),
    GEOLOCATION("geolocation", "access"),
    CAMERA("camera", "access"),
    MICROPHONE("microphone", "access"),
}

enum class PermissionStatus(private val wire: String) {
    NOT_DETERMINED("notDetermined"),
    DENIED("denied"),
    ALLOWED("allowed"),
    ;

    internal companion object {
        fun decode(value: JsonPrimitive) = entries.first { value.isString && it.wire == value.content }
    }
}

enum class PermissionDialogResult {
    ALLOWED,
    DENIED,
    ;

    internal companion object {
        fun decode(value: JsonPrimitive) = entries.first { value.isString && it.name.lowercase() == value.content }
    }
}

/** 기기 권한이 필요한 API의 결과다. 거부는 예외가 아니라 결과다. */
sealed interface PermissionResult<out T> {
    data class Granted<T>(val value: T) : PermissionResult<T>
    data object Denied : PermissionResult<Nothing>
}

/** bridge 형식 `{permission: "allowed", value}` 또는 `{permission: "denied"}`를 디코딩한다. */
internal fun <T> JsonObject.permissionResult(value: (JsonElement) -> T): PermissionResult<T> = when (string("permission")) {
    "allowed" -> PermissionResult.Granted(value(getValue("value")))
    "denied" -> PermissionResult.Denied
    else -> error("Unknown permission result")
}
