package com.proteksolutions.spamshield.data

import android.content.Context
import android.content.SharedPreferences

/** User preferences for the screening rules. */
class Settings(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("spamshield_settings", Context.MODE_PRIVATE)

    /** Master switch. */
    var enabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(v) = prefs.edit().putBoolean("enabled", v).apply()

    /** Block calls with no caller ID (private / restricted / unknown). */
    var blockHidden: Boolean
        get() = prefs.getBoolean("block_hidden", true)
        set(v) = prefs.edit().putBoolean("block_hidden", v).apply()

    /** Block numbers sharing your area code + exchange (classic "neighbor spoofing"). */
    var blockNeighborSpoof: Boolean
        get() = prefs.getBoolean("block_neighbor", false)
        set(v) = prefs.edit().putBoolean("block_neighbor", v).apply()

    /** Block calls whose caller ID failed carrier STIR/SHAKEN verification (Android 11+). */
    var blockFailedVerification: Boolean
        get() = prefs.getBoolean("block_failed_verstat", true)
        set(v) = prefs.edit().putBoolean("block_failed_verstat", v).apply()

    /** Aggressive: block every number that isn't a saved contact or on the allow list. */
    var blockNonContacts: Boolean
        get() = prefs.getBoolean("block_non_contacts", false)
        set(v) = prefs.edit().putBoolean("block_non_contacts", v).apply()

    /** Contacts always ring, even if they match an area-code / prefix rule. */
    var alwaysAllowContacts: Boolean
        get() = prefs.getBoolean("allow_contacts", true)
        set(v) = prefs.edit().putBoolean("allow_contacts", v).apply()

    /** true = reject (caller hits voicemail/busy); false = silently let it ring out. */
    var rejectInsteadOfSilence: Boolean
        get() = prefs.getBoolean("reject", true)
        set(v) = prefs.edit().putBoolean("reject", v).apply()

    /** Show a notification each time a call is blocked (with Report buttons). */
    var notifyOnBlock: Boolean
        get() = prefs.getBoolean("notify", true)
        set(v) = prefs.edit().putBoolean("notify", v).apply()

    /** Your own number; used for neighbor-spoofing detection. */
    var myNumber: String
        get() = prefs.getString("my_number", "").orEmpty()
        set(v) = prefs.edit().putString("my_number", v).apply()

    /**
     * Body of the text sent to 7726. "{number}" is replaced with the spam number.
     * The major US carriers expect "Call" for call reports, then reply asking for the number.
     */
    var sms7726Template: String
        get() = prefs.getString("sms_7726_template", DEFAULT_7726_TEMPLATE).orEmpty()
        set(v) = prefs.edit().putString("sms_7726_template", v).apply()

    companion object {
        const val DEFAULT_7726_TEMPLATE = "Call"
    }
}
