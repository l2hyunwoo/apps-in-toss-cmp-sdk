package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class StorageApi internal constructor(private val client: AppsInTossClient) {
    /** Reads the host's persistent storage; returns null when the key is absent. Requires [Capability.STORAGE]. */
    suspend fun getItem(key: String): String? {
        val result = client.call("storageGet", payload(key))
        return decode("storage") {
            val value = Json.parseToJsonElement(result)
            if (value is JsonNull) null else value.jsonPrimitive.also { require(it.isString) }.content
        }
    }

    /** Requires [Capability.STORAGE]. */
    suspend fun setItem(key: String, value: String) {
        client.call("storageSet", payload(key) { put("value", value) })
    }

    /** Requires [Capability.STORAGE]. */
    suspend fun removeItem(key: String) {
        client.call("storageRemove", payload(key))
    }

    private fun payload(key: String, extra: JsonObjectBuilder.() -> Unit = {}): JsonObject {
        require(key.isNotBlank()) { "Storage key cannot be blank" }
        return buildJsonObject {
            put("key", key)
            extra()
        }
    }
}
