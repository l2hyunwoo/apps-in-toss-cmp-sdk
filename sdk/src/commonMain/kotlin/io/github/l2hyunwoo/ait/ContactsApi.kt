package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class ContactsApi internal constructor(private val client: AppsInTossClient) {
    /** Reads one page of contacts; pass 0 first, then [ContactResult.nextOffset]. Requires [Capability.CONTACTS]. */
    suspend fun getContacts(size: Int, offset: Int, contains: String? = null): PermissionResult<ContactResult> {
        require(size > 0 && offset >= 0)
        val result = client.call(
            "contactsGet",
            buildJsonObject {
                put("size", size)
                put("offset", offset)
                contains?.let { put("contains", it) }
            },
        )
        return decode("contacts") {
            Json.parseToJsonElement(result).jsonObject.permissionResult { value ->
                val obj = value.jsonObject
                val nextOffset = obj.getValue("nextOffset").let { if (it is JsonNull) null else it.jsonPrimitive.also { p -> require(!p.isString) }.int }
                ContactResult(
                    result = obj.getValue("result").jsonArray.map { ContactEntity(it.jsonObject.string("name"), it.jsonObject.string("phoneNumber")) },
                    nextOffset = nextOffset,
                    done = obj.getValue("done").jsonPrimitive.also { require(!it.isString) }.boolean,
                )
            }
        }
    }
}

data class ContactEntity(val name: String, val phoneNumber: String)
data class ContactResult(val result: List<ContactEntity>, val nextOffset: Int?, val done: Boolean)
