package com.proteksolutions.spamshield.data

/**
 * Pure helpers for normalizing and comparing phone numbers. Kept free of Android
 * APIs so it can be unit tested on the JVM.
 *
 * Numbers are assumed to be North American (NANP, +1) when they have 10 digits,
 * or 11 digits starting with 1. Anything else is kept as international digits.
 */
object PhoneNumbers {

    /** Digits only, e.g. "+1 (512) 555-0100" -> "15125550100". */
    fun digits(raw: String?): String = raw.orEmpty().filter { it.isDigit() }

    /** E.164-ish form, e.g. "5125550100" -> "+15125550100". Empty string if no digits. */
    fun normalize(raw: String?): String {
        val d = digits(raw)
        if (d.isEmpty()) return ""
        return when {
            d.length == 10 -> "+1$d"
            d.length == 11 && d.startsWith("1") -> "+$d"
            else -> "+$d"
        }
    }

    /** True if the number is a NANP (+1) number with a full 10-digit national part. */
    fun isNanp(raw: String?): Boolean {
        val d = digits(raw)
        return d.length == 10 || (d.length == 11 && d.startsWith("1"))
    }

    /** 10-digit national number for NANP numbers ("5125550100"), otherwise null. */
    fun nationalNanp(raw: String?): String? {
        val d = digits(raw)
        return when {
            d.length == 10 -> d
            d.length == 11 && d.startsWith("1") -> d.substring(1)
            else -> null
        }
    }

    /** Area code (NPA), e.g. "512" for "+1 512 555 0100". Null for non-NANP numbers. */
    fun areaCode(raw: String?): String? = nationalNanp(raw)?.substring(0, 3)

    /** Area code + exchange (NPA-NXX), e.g. "512555". Used for neighbor-spoofing detection. */
    fun npaNxx(raw: String?): String? = nationalNanp(raw)?.substring(0, 6)

    /** Same number, ignoring formatting and the +1 country code. */
    fun sameNumber(a: String?, b: String?): Boolean {
        val na = normalize(a)
        return na.isNotEmpty() && na == normalize(b)
    }

    /**
     * Prefix match. The prefix is compared against both the full digits (with country
     * code) and the 10-digit national form, so "800" and "1800" both match +1 800 ...
     */
    fun matchesPrefix(number: String?, prefix: String): Boolean {
        val p = digits(prefix)
        if (p.isEmpty()) return false
        val full = digits(number)
        val national = nationalNanp(number)
        return full.startsWith(p) || (national != null && national.startsWith(p))
    }

    /**
     * Parses user input like "512, 737 , (888)" into a list of valid 3-digit area codes.
     * Invalid entries are returned separately so the UI can report them.
     */
    fun parseAreaCodes(input: String): Pair<List<String>, List<String>> {
        val valid = mutableListOf<String>()
        val invalid = mutableListOf<String>()
        input.split(',', ' ', ';', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { token ->
                val d = digits(token)
                // NANP area codes are 3 digits and never start with 0 or 1.
                if (d.length == 3 && d[0] in '2'..'9') valid += d else invalid += token
            }
        return valid.distinct() to invalid
    }

    /** Pretty display: "+15125550100" -> "(512) 555-0100". */
    fun format(raw: String?): String {
        if (raw.isNullOrBlank()) return "Private / Unknown"
        val n = nationalNanp(raw) ?: return raw
        return "(${n.substring(0, 3)}) ${n.substring(3, 6)}-${n.substring(6)}"
    }
}
