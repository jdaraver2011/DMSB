package com.proteksolutions.spamshield

import com.proteksolutions.spamshield.data.PhoneNumbers
import com.proteksolutions.spamshield.data.Rule
import com.proteksolutions.spamshield.data.RuleType
import com.proteksolutions.spamshield.screening.CallRuleEngine
import com.proteksolutions.spamshield.screening.Decision
import com.proteksolutions.spamshield.screening.IncomingCall
import com.proteksolutions.spamshield.screening.ScreeningConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallRuleEngineTest {

    private fun call(number: String?, contact: Boolean = false, hidden: Boolean = false, failed: Boolean = false) =
        IncomingCall(PhoneNumbers.normalize(number).ifEmpty { null }, hidden, failed, contact)

    private fun blocked(d: Decision) = d is Decision.Block

    @Test fun normalizesNanpNumbers() {
        assertEquals("+15125550100", PhoneNumbers.normalize("(512) 555-0100"))
        assertEquals("+15125550100", PhoneNumbers.normalize("1-512-555-0100"))
        assertEquals("512", PhoneNumbers.areaCode("+1 512 555 0100"))
        assertNull(PhoneNumbers.areaCode("+44 20 7946 0958"))
    }

    @Test fun parsesAreaCodeInput() {
        val (valid, invalid) = PhoneNumbers.parseAreaCodes("876, (809) 123 512;512")
        assertEquals(listOf("876", "809", "512"), valid)
        assertEquals(listOf("123"), invalid)
    }

    @Test fun blocksAreaCode() {
        val rules = listOf(Rule(type = RuleType.BLOCK_AREA_CODE, value = "876"))
        assertTrue(blocked(CallRuleEngine.decide(call("876-555-1234"), rules, ScreeningConfig())))
        assertTrue(blocked(CallRuleEngine.decide(call("+18765551234"), rules, ScreeningConfig())))
        assertTrue(!blocked(CallRuleEngine.decide(call("512-876-1234"), rules, ScreeningConfig())))
    }

    @Test fun contactsBypassAreaCodeUnlessDisabled() {
        val rules = listOf(Rule(type = RuleType.BLOCK_AREA_CODE, value = "876"))
        assertTrue(!blocked(CallRuleEngine.decide(call("8765551234", contact = true), rules, ScreeningConfig())))
        assertTrue(blocked(CallRuleEngine.decide(call("8765551234", contact = true), rules,
            ScreeningConfig(alwaysAllowContacts = false))))
    }

    @Test fun allowListBeatsEverything() {
        val rules = listOf(
            Rule(type = RuleType.BLOCK_AREA_CODE, value = "876"),
            Rule(type = RuleType.ALLOW_NUMBER, value = "+18765551234"),
        )
        assertTrue(!blocked(CallRuleEngine.decide(call("876 555 1234"), rules,
            ScreeningConfig(blockNonContacts = true))))
    }

    @Test fun blocksExactNumberAndPrefix() {
        val rules = listOf(
            Rule(type = RuleType.BLOCK_NUMBER, value = "+15125550100"),
            Rule(type = RuleType.BLOCK_PREFIX, value = "900"),
        )
        assertTrue(blocked(CallRuleEngine.decide(call("512-555-0100"), rules, ScreeningConfig())))
        assertTrue(blocked(CallRuleEngine.decide(call("1-900-555-0000"), rules, ScreeningConfig())))
        assertTrue(!blocked(CallRuleEngine.decide(call("512-555-0101"), rules, ScreeningConfig())))
    }

    @Test fun hiddenAndVerification() {
        assertTrue(blocked(CallRuleEngine.decide(call(null, hidden = true), emptyList(), ScreeningConfig())))
        assertTrue(!blocked(CallRuleEngine.decide(call(null, hidden = true), emptyList(),
            ScreeningConfig(blockHidden = false))))
        assertTrue(blocked(CallRuleEngine.decide(call("5125550100", failed = true), emptyList(), ScreeningConfig())))
    }

    @Test fun neighborSpoofing() {
        val cfg = ScreeningConfig(blockNeighborSpoof = true, myNumber = "512-555-0100")
        assertTrue(blocked(CallRuleEngine.decide(call("512-555-9999"), emptyList(), cfg)))
        assertTrue(!blocked(CallRuleEngine.decide(call("512-556-9999"), emptyList(), cfg)))
    }

    @Test fun disabledAllowsAll() {
        val rules = listOf(Rule(type = RuleType.BLOCK_AREA_CODE, value = "876"))
        assertTrue(!blocked(CallRuleEngine.decide(call("8765551234"), rules, ScreeningConfig(enabled = false))))
    }
}
