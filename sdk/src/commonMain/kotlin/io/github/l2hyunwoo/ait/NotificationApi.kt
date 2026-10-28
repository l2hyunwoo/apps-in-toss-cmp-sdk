package io.github.l2hyunwoo.ait

import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*

class NotificationApi internal constructor(private val client: AppsInTossClient) {
    /** 콘솔 스마트 메시지 [templateCode]의 동의 화면을 보여 준다. [Capability.NOTIFICATION_AGREEMENT]가 필요하다. */
    suspend fun requestAgreement(templateCode: String): NotificationAgreementResult {
        require(templateCode.isNotBlank()) { "Template code cannot be blank" }
        val event = client.stream("notificationAgreement", buildJsonObject { put("templateCode", templateCode) }).first()
        return decode("notification agreement") {
            when (event.string("type")) {
                "newAgreement" -> NotificationAgreementResult.NEW_AGREEMENT
                "alreadyAgreed" -> NotificationAgreementResult.ALREADY_AGREED
                "agreementRejected" -> NotificationAgreementResult.AGREEMENT_REJECTED
                else -> error("Unknown agreement result")
            }
        }
    }
}

enum class NotificationAgreementResult { NEW_AGREEMENT, ALREADY_AGREED, AGREEMENT_REJECTED }
