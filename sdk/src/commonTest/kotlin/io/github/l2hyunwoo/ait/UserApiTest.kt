package io.github.l2hyunwoo.ait

import kotlin.test.*
import kotlinx.coroutines.test.runTest

class UserApiTest {
    @Test fun anonymousKeyAuthCode() = runTest {
        val t = FakeTransport().apply { reply = """{"code":"c1","expiresIn":300}""" }
        val c = AppsInTossClient(t)
        assertEquals(AnonymousKeyAuthCode("c1", 300), c.user.createAnonymousKeyAuthCode())
        assertEquals("anonymousKeyAuthCode", t.calls.last().first)
        t.reply = """{"code":"c1"}"""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.user.createAnonymousKeyAuthCode() }.code)
    }

    @Test fun consentedDataResults() = runTest {
        val t = FakeTransport().apply { reply = """{"data":{"USER_NAME":"nunu","USER_PHONE":"010"}}""" }
        val c = AppsInTossClient(t)
        assertEquals(ConsentedDataResult.Consented(mapOf("USER_NAME" to "nunu", "USER_PHONE" to "010")), c.user.getConsentedData("cud_delivery"))
        assertEquals("consentedData" to """{"consentedUserDataKey":"cud_delivery"}""", t.calls.last())
        t.reply = """{"errorCode":"USER_DECLINED"}"""
        assertEquals(ConsentedDataResult.Failed(ConsentedDataError.USER_DECLINED), c.user.getConsentedData("k", shouldRequestAgreementWhenUserDeclined = true))
        assertEquals("""{"consentedUserDataKey":"k","shouldRequestAgreementWhenUserDeclined":true}""", t.calls.last().second)
        for (bad in listOf("""{"errorCode":"OTHER"}""", """{"data":{"USER_NAME":1}}""", "{}")) {
            t.reply = bad
            assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.user.getConsentedData("k") }.code, bad)
        }
        val sent = t.calls.size
        assertFailsWith<IllegalArgumentException> { c.user.getConsentedData(" ") }
        assertEquals(sent, t.calls.size)
    }

    @Test fun declaredAgeRange() = runTest {
        val t = FakeTransport().apply { reply = """{"status":"SHARING","lowerBound":18,"activeParentalControls":["communicationLimits"],"isEligibleForAgeFeatures":true}""" }
        val c = AppsInTossClient(t)
        assertEquals(
            DeclaredAgeRange(DeclaredAgeRangeStatus.SHARING, 18, null, null, listOf("communicationLimits"), true, null),
            c.user.getDeclaredAgeRange(listOf(13, 18)),
        )
        assertEquals("declaredAgeRange" to """{"ageGates":[13,18]}""", t.calls.last())
        t.reply = """{"status":"SHARING"}"""
        assertEquals(ErrorCode.INVALID_RESPONSE, assertFailsWith<AppsInTossException> { c.user.getDeclaredAgeRange(listOf(18)) }.code)
    }
}
