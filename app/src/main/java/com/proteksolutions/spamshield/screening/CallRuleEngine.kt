package com.proteksolutions.spamshield.screening

import com.proteksolutions.spamshield.data.PhoneNumbers
import com.proteksolutions.spamshield.data.Rule
import com.proteksolutions.spamshield.data.RuleType

/** Everything the engine needs to know about one incoming call. */
data class IncomingCall(
    val number: String?,
    /** Caller ID withheld / restricted / unknown. */
    val hidden: Boolean,
    /** Carrier reported STIR/SHAKEN verification FAILED. */
    val verificationFailed: Boolean,
    val isContact: Boolean,
)

/** Snapshot of the user's settings relevant to screening. */
data class ScreeningConfig(
    val enabled: Boolean = true,
    val blockHidden: Boolean = true,
    val blockNeighborSpoof: Boolean = false,
    val blockFailedVerification: Boolean = true,
    val blockNonContacts: Boolean = false,
    val alwaysAllowContacts: Boolean = true,
    val myNumber: String = "",
)

sealed class Decision {
    data class Allow(val why: String) : Decision()
    data class Block(val reason: String) : Decision()
}

/**
 * Decides whether to block a call. Pure Kotlin (no Android APIs) so it is unit tested.
 *
 * Order of precedence:
 *  1. Screening disabled           -> allow
 *  2. Allow list                   -> allow
 *  3. Saved contact (if enabled)   -> allow
 *  4. Hidden caller ID             -> block (if enabled)
 *  5. Exact number block list      -> block
 *  6. Area-code block list         -> block
 *  7. Prefix block list            -> block
 *  8. Failed STIR/SHAKEN           -> block (if enabled)
 *  9. Neighbor spoofing            -> block (if enabled)
 * 10. Not a contact                -> block (if enabled)
 * 11. Otherwise                    -> allow
 */
object CallRuleEngine {

    fun decide(call: IncomingCall, rules: List<Rule>, cfg: ScreeningConfig): Decision {
        if (!cfg.enabled) return Decision.Allow("Screening off")

        val number = call.number
        val hasNumber = !number.isNullOrBlank() && PhoneNumbers.digits(number).isNotEmpty()

        if (hasNumber && rules.any { it.type == RuleType.ALLOW_NUMBER && PhoneNumbers.sameNumber(it.value, number) }) {
            return Decision.Allow("On allow list")
        }
        if (call.isContact && cfg.alwaysAllowContacts) return Decision.Allow("Contact")

        if (!hasNumber || call.hidden) {
            return if (cfg.blockHidden) Decision.Block("Hidden caller ID")
            else Decision.Allow("Hidden caller ID (allowed)")
        }

        rules.firstOrNull { it.type == RuleType.BLOCK_NUMBER && PhoneNumbers.sameNumber(it.value, number) }
            ?.let { return Decision.Block("Block list" + labelSuffix(it)) }

        val area = PhoneNumbers.areaCode(number)
        if (area != null) {
            rules.firstOrNull { it.type == RuleType.BLOCK_AREA_CODE && it.value == area }
                ?.let { return Decision.Block("Area code $area" + labelSuffix(it)) }
        }

        rules.firstOrNull { it.type == RuleType.BLOCK_PREFIX && PhoneNumbers.matchesPrefix(number, it.value) }
            ?.let { return Decision.Block("Prefix ${it.value}" + labelSuffix(it)) }

        if (cfg.blockFailedVerification && call.verificationFailed) {
            return Decision.Block("Failed caller ID verification")
        }

        if (cfg.blockNeighborSpoof && !call.isContact) {
            val mine = PhoneNumbers.npaNxx(cfg.myNumber)
            val theirs = PhoneNumbers.npaNxx(number)
            if (mine != null && mine == theirs && !PhoneNumbers.sameNumber(cfg.myNumber, number)) {
                return Decision.Block("Neighbor spoofing")
            }
        }

        if (cfg.blockNonContacts && !call.isContact) return Decision.Block("Not in contacts")

        return Decision.Allow("No rule matched")
    }

    private fun labelSuffix(rule: Rule) = if (rule.label.isNotBlank()) " (${rule.label})" else ""
}
