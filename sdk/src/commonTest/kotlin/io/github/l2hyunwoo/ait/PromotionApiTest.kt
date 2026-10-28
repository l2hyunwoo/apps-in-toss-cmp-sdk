package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class PromotionApiTest {
    @Test fun grantRewardSendsPayloadAndDecodesResults() = runTest {
        val t = FakeTransport().apply { reply = """{"key":"reward-key"}""" }
        val c = AppsInTossClient(t)
        assertEquals(PromotionRewardResult.Granted("reward-key"), c.promotion.grantReward("CODE", 1000))
        assertEquals("promotionGrantReward" to """{"promotionCode":"CODE","amount":1000}""", t.calls.last())
        t.reply = """{"errorCode":"4113"}"""
        assertEquals(PromotionRewardResult.Failed("4113"), c.promotion.grantReward("CODE", 1000))
    }

    @Test fun grantRewardRejectsInvalidInputAndResponses() = runTest {
        val t = FakeTransport()
        val c = AppsInTossClient(t)
        assertFailsWith<IllegalArgumentException> { c.promotion.grantReward(" ", 1) }
        assertTrue(t.calls.isEmpty())
        for (reply in listOf("""{"key":""}""", """{"errorCode":4100}""", "null")) {
            t.reply = reply
            assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.promotion.grantReward("CODE", 1) }.code, reply)
        }
    }
}
