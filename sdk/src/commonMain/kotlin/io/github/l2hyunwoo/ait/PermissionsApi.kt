package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class PermissionsApi internal constructor(private val client: AppsInTossClient) {
    /** Reads the current status without showing a prompt. Requires [Capability.PERMISSIONS]. */
    suspend fun getPermission(permission: Permission): PermissionStatus {
        val result = client.call("permissionGet", permission.payload())
        return decode("permission") { PermissionStatus.decode(Json.parseToJsonElement(result).jsonPrimitive) }
    }

    /** Prompts only while the status is not determined. Requires [Capability.PERMISSIONS]. */
    suspend fun requestPermission(permission: Permission): PermissionDialogResult {
        val result = client.call("permissionRequest", permission.payload())
        return decode("permission") { PermissionDialogResult.decode(Json.parseToJsonElement(result).jsonPrimitive) }
    }

    /** Asks again after a denial. Requires [Capability.PERMISSIONS]. */
    suspend fun openPermissionDialog(permission: Permission): PermissionDialogResult {
        val result = client.call("permissionDialog", permission.payload())
        return decode("permission") { PermissionDialogResult.decode(Json.parseToJsonElement(result).jsonPrimitive) }
    }

    private fun Permission.payload() = buildJsonObject {
        put("name", permissionName)
        put("access", access)
    }
}

/** The official `{name, access}` combinations. */
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

/** Result of an API that needs a device permission. A denial is a result, not an exception. */
sealed interface PermissionResult<out T> {
    data class Granted<T>(val value: T) : PermissionResult<T>
    data object Denied : PermissionResult<Nothing>
}

/** Decodes the bridge shape `{permission: "allowed", value}` or `{permission: "denied"}`. */
internal fun <T> JsonObject.permissionResult(value: (JsonElement) -> T): PermissionResult<T> = when (string("permission")) {
    "allowed" -> PermissionResult.Granted(value(getValue("value")))
    "denied" -> PermissionResult.Denied
    else -> error("Unknown permission result")
}
