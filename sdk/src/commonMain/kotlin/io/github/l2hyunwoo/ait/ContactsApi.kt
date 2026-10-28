package io.github.l2hyunwoo.ait

import kotlinx.serialization.json.*

class ContactsApi internal constructor(private val client: AppsInTossClient) {
    /** 연락처를 한 페이지 읽는다. 처음에는 0을, 그다음에는 [ContactResult.nextOffset]을 넘긴다. [Capability.CONTACTS]가 필요하다. */
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
