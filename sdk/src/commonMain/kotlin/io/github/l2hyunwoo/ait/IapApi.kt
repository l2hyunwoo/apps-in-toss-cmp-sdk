package io.github.l2hyunwoo.ait

import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*

/**
 * In-app purchases. The app's server decides every grant: call it from `grant` and return its answer.
 * When `grant` returns false, throws, or does not finish (cancellation or [AppsInTossClient.close]), the host
 * records the grant as failed. Recover such orders with [getPendingOrders] and [completeProductGrant].
 */
class IapApi internal constructor(private val client: AppsInTossClient) {
    /** Opens the one-time order page; [grant] returns true once the app's server granted the order. Requires [Capability.IAP]. */
    suspend fun createOneTimePurchaseOrder(sku: String, grant: suspend (orderId: String) -> Boolean): PurchaseResult {
        require(sku.isNotBlank()) { "SKU cannot be blank" }
        return order("iapOneTimeOrder", buildJsonObject { put("sku", sku) }) { grant(it.string("orderId")) }
    }

    /** Opens the subscription order page; a null [offerId] uses the base price. Requires [Capability.IAP_SUBSCRIPTION]. */
    suspend fun createSubscriptionPurchaseOrder(
        sku: String,
        offerId: String? = null,
        grant: suspend (orderId: String, subscriptionId: String?) -> Boolean,
    ): PurchaseResult {
        require(sku.isNotBlank()) { "SKU cannot be blank" }
        require(offerId == null || offerId.isNotBlank()) { "Offer id cannot be blank" }
        val payload = buildJsonObject {
            put("sku", sku)
            put("offerId", offerId)
        }
        return order("iapSubscriptionOrder", payload) { grant(it.string("orderId"), it.optionalString("subscriptionId")) }
    }

    /** Requires [Capability.IAP]. */
    suspend fun getProductItemList(): List<IapProduct> {
        val result = client.call("iapProducts")
        return decode("product list") {
            Json.parseToJsonElement(result).jsonObject.getValue("products").jsonArray.map { it.jsonObject.toProduct() }
        }
    }

    /** Orders that were paid but not yet reported as granted. Requires [Capability.IAP]. */
    suspend fun getPendingOrders(): List<PendingOrder> {
        val result = client.call("iapPendingOrders")
        return decode("pending orders") {
            Json.parseToJsonElement(result).jsonObject.getValue("orders").jsonArray.map {
                val obj = it.jsonObject
                PendingOrder(obj.string("orderId"), obj.string("sku"), obj.string("paymentCompletedDate"))
            }
        }
    }

    /** Pass the previous page's [CompletedOrRefundedOrders.nextKey] as [key]; null reads the first page. Requires [Capability.IAP]. */
    suspend fun getCompletedOrRefundedOrders(key: String? = null): CompletedOrRefundedOrders {
        require(key == null || key.isNotBlank()) { "Page key cannot be blank" }
        val result = client.call("iapCompletedOrders", buildJsonObject { put("key", key) })
        return decode("completed orders") {
            val obj = Json.parseToJsonElement(result).jsonObject
            CompletedOrRefundedOrders(
                hasNext = obj.getValue("hasNext").jsonPrimitive.boolean,
                nextKey = obj.optionalString("nextKey"),
                orders = obj.getValue("orders").jsonArray.map {
                    val order = it.jsonObject
                    CompletedOrRefundedOrder(order.string("orderId"), order.string("sku"), OrderStatus.valueOf(order.string("status")), order.string("date"))
                },
            )
        }
    }

    /** Reports that the app's server granted [orderId]; returns the host's result. Requires [Capability.IAP]. */
    suspend fun completeProductGrant(orderId: String): Boolean {
        require(orderId.isNotBlank()) { "Order id cannot be blank" }
        val result = client.call("iapCompleteGrant", buildJsonObject { put("orderId", orderId) })
        return decode("grant completion") { Json.parseToJsonElement(result).jsonPrimitive.boolean }
    }

    /** Requires [Capability.IAP_SUBSCRIPTION]. */
    suspend fun getSubscriptionInfo(orderId: String): SubscriptionInfo {
        require(orderId.isNotBlank()) { "Order id cannot be blank" }
        val result = client.call("iapSubscriptionInfo", buildJsonObject { put("orderId", orderId) })
        return decode("subscription info") {
            val obj = Json.parseToJsonElement(result).jsonObject.getValue("subscription").jsonObject
            SubscriptionInfo(
                catalogId = obj.getValue("catalogId").jsonPrimitive.long,
                status = obj.string("status"),
                expiresAt = obj.optionalString("expiresAt"),
                isAutoRenew = obj.getValue("isAutoRenew").jsonPrimitive.boolean,
                gracePeriodExpiresAt = obj.optionalString("gracePeriodExpiresAt"),
                isAccessible = obj.getValue("isAccessible").jsonPrimitive.boolean,
            )
        }
    }

    private suspend fun order(method: String, payload: JsonObject, grant: suspend (JsonObject) -> Boolean): PurchaseResult {
        val event = client.stream(method, payload) { JsonPrimitive(grant(it)) }.first()
        return decode("purchase order") {
            when (event.string("type")) {
                "success" -> PurchaseResult.Success(event.getValue("data").jsonObject.toOrder())
                "error" -> PurchaseResult.Failure(PurchaseError.valueOf(event.string("code")))
                else -> error("Unknown order event")
            }
        }
    }
}

private fun JsonObject.optionalString(key: String): String? = this[key]?.takeUnless { it is JsonNull }?.let { string(key) }

private fun JsonObject.toOrder() = PurchaseOrder(
    orderId = string("orderId"),
    displayName = string("displayName"),
    displayAmount = string("displayAmount"),
    amount = getValue("amount").jsonPrimitive.double,
    currency = string("currency"),
    fraction = getValue("fraction").jsonPrimitive.int,
    miniAppIconUrl = optionalString("miniAppIconUrl"),
)

private fun JsonObject.toProduct(): IapProduct {
    val type = IapProductType.valueOf(string("type"))
    return IapProduct(
        sku = string("sku"),
        type = type,
        displayName = string("displayName"),
        displayAmount = string("displayAmount"),
        iconUrl = string("iconUrl"),
        description = string("description"),
        renewalCycle = if (type == IapProductType.SUBSCRIPTION) RenewalCycle.valueOf(string("renewalCycle")) else null,
        offers = this["offers"]?.takeUnless { it is JsonNull }?.jsonArray?.map {
            val offer = it.jsonObject
            val offerType = IapOfferType.valueOf(offer.string("type"))
            IapOffer(offerType, offer.string("offerId"), offer.string("period"), if (offerType == IapOfferType.FREE_TRIAL) null else offer.string("displayAmount"))
        }.orEmpty(),
    )
}

sealed interface PurchaseResult {
    data class Success(val order: PurchaseOrder) : PurchaseResult
    data class Failure(val error: PurchaseError) : PurchaseResult
}

/** Documented order error codes. [PRODUCT_NOT_GRANTED_BY_PARTNER] follows a false or failed grant. */
enum class PurchaseError {
    INVALID_PRODUCT_ID,
    PAYMENT_PENDING,
    NETWORK_ERROR,
    INVALID_USER_ENVIRONMENT,
    ITEM_ALREADY_OWNED,
    APP_MARKET_VERIFICATION_FAILED,
    TOSS_SERVER_VERIFICATION_FAILED,
    INTERNAL_ERROR,
    KOREAN_ACCOUNT_ONLY,
    USER_CANCELED,
    PRODUCT_NOT_GRANTED_BY_PARTNER,
}

data class PurchaseOrder(
    val orderId: String,
    val displayName: String,
    val displayAmount: String,
    val amount: Double,
    val currency: String,
    val fraction: Int,
    val miniAppIconUrl: String?,
)

enum class IapProductType { CONSUMABLE, NON_CONSUMABLE, SUBSCRIPTION }
enum class RenewalCycle { WEEKLY, MONTHLY, YEARLY }
enum class IapOfferType { FREE_TRIAL, NEW_SUBSCRIPTION, RETURNING }

/** [renewalCycle] is set only for [IapProductType.SUBSCRIPTION]. */
data class IapProduct(
    val sku: String,
    val type: IapProductType,
    val displayName: String,
    val displayAmount: String,
    val iconUrl: String,
    val description: String,
    val renewalCycle: RenewalCycle?,
    val offers: List<IapOffer>,
)

/** [displayAmount] is null for [IapOfferType.FREE_TRIAL]. */
data class IapOffer(val type: IapOfferType, val offerId: String, val period: String, val displayAmount: String?)

data class PendingOrder(val orderId: String, val sku: String, val paymentCompletedDate: String)

enum class OrderStatus { COMPLETED, REFUNDED }
data class CompletedOrRefundedOrder(val orderId: String, val sku: String, val status: OrderStatus, val date: String)
data class CompletedOrRefundedOrders(val hasNext: Boolean, val nextKey: String?, val orders: List<CompletedOrRefundedOrder>)

/** [status] stays a string: the official type admits values beyond the documented ones. */
data class SubscriptionInfo(
    val catalogId: Long,
    val status: String,
    val expiresAt: String?,
    val isAutoRenew: Boolean,
    val gracePeriodExpiresAt: String?,
    val isAccessible: Boolean,
)
