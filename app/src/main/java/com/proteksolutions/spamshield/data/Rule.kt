package com.proteksolutions.spamshield.data

enum class RuleType(val label: String) {
    /** Block one specific number. */
    BLOCK_NUMBER("Number"),
    /** Block every number starting with these digits (e.g. 1800555). */
    BLOCK_PREFIX("Prefix"),
    /** Block every call from a 3-digit area code (e.g. 876). NANP numbers only. */
    BLOCK_AREA_CODE("Area code"),
    /** Always let this number ring, overriding every block rule. */
    ALLOW_NUMBER("Allowed"),
}

data class Rule(
    val id: Long = 0,
    val type: RuleType,
    /** Normalized number, digit prefix, or 3-digit area code depending on [type]. */
    val value: String,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

data class BlockedCall(
    val id: Long = 0,
    val number: String?,
    val reason: String,
    val timestamp: Long,
    val reported7726: Boolean = false,
    val reportedFtc: Boolean = false,
)
