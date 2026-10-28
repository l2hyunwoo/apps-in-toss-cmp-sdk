package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class StorageApi internal constructor(private val client: AppsInTossClient) {
    /** Reads the host's persistent storage; returns null when the key is absent. Requires [Capability.STORAGE]. */
    suspend fun getItem(key: String): String? {
        val result = client.call("storageGet", payload(key))
        return decode("storage") { nullableString(Json.parseToJsonElement(result)) }
    }

    /** Reads several keys at once; absent keys map to null. Requires [Capability.STORAGE_BATCH]. */
    suspend fun getItems(keys: List<String>): Map<String, String?> {
        keys.forEach(::requireKey)
        val result = client.call("storageGetItems", buildJsonObject { putJsonArray("keys") { keys.forEach { add(it) } } })
        return decode("storage items") {
            val obj = Json.parseToJsonElement(result).jsonObject
            keys.associateWith { nullableString(obj.getValue(it)) }
        }
    }

    /** Requires [Capability.STORAGE]. */
    suspend fun setItem(key: String, value: String) {
        client.call("storageSet", payload(key) { put("value", value) })
    }

    /** Requires [Capability.STORAGE_BATCH]. */
    suspend fun setItems(items: Map<String, String>) {
        items.keys.forEach(::requireKey)
        client.call("storageSetItems", buildJsonObject { putJsonObject("items") { items.forEach { (key, value) -> put(key, value) } } })
    }

    /** Requires [Capability.STORAGE]. */
    suspend fun removeItem(key: String) {
        client.call("storageRemove", payload(key))
    }

    /** Removes every stored value. Requires [Capability.STORAGE]. */
    suspend fun clearItems() {
        client.call("storageClear")
    }

    private fun payload(key: String, extra: JsonObjectBuilder.() -> Unit = {}): JsonObject {
        requireKey(key)
        return buildJsonObject {
            put("key", key)
            extra()
        }
    }

    private fun requireKey(key: String) = require(key.isNotBlank()) { "Storage key cannot be blank" }

    private fun nullableString(value: JsonElement): String? = if (value is JsonNull) null else value.jsonPrimitive.also { require(it.isString) }.content
}
