package com.proteksolutions.spamshield.screening

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.Connection
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.proteksolutions.spamshield.BlockNotifier
import com.proteksolutions.spamshield.data.PhoneNumbers
import com.proteksolutions.spamshield.data.Settings
import com.proteksolutions.spamshield.data.SpamDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android calls [onScreenCall] for each incoming call once the user has given this app
 * the Call Screening role. We must respond (allow/block) within a few seconds.
 */
class SpamCallScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(details: Call.Details) {
        if (details.callDirection != Call.Details.DIRECTION_INCOMING) {
            respondToCall(details, CallResponse.Builder().build())
            return
        }
        scope.launch {
            val response = try {
                screen(details)
            } catch (t: Throwable) {
                Log.e(TAG, "Screening failed; allowing call", t)
                CallResponse.Builder().build()
            }
            respondToCall(details, response)
        }
    }

    private fun screen(details: Call.Details): CallResponse {
        val settings = Settings(this)
        val db = SpamDb.get(this)

        val raw = details.handle?.schemeSpecificPart
        val number = PhoneNumbers.normalize(raw).ifEmpty { null }
        val hidden = details.handlePresentation != TelecomManager.PRESENTATION_ALLOWED
        val verificationFailed = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            details.callerNumberVerificationStatus == Connection.VERIFICATION_STATUS_FAILED

        val call = IncomingCall(
            number = number,
            hidden = hidden,
            verificationFailed = verificationFailed,
            isContact = number != null && isContact(this, number),
        )
        val cfg = ScreeningConfig(
            enabled = settings.enabled,
            blockHidden = settings.blockHidden,
            blockNeighborSpoof = settings.blockNeighborSpoof,
            blockFailedVerification = settings.blockFailedVerification,
            blockNonContacts = settings.blockNonContacts,
            alwaysAllowContacts = settings.alwaysAllowContacts,
            myNumber = settings.myNumber,
        )

        return when (val d = CallRuleEngine.decide(call, db.rules(), cfg)) {
            is Decision.Allow -> CallResponse.Builder().build()
            is Decision.Block -> {
                Log.i(TAG, "Blocking $number: ${d.reason}")
                db.logBlocked(number, d.reason)
                if (settings.notifyOnBlock) BlockNotifier.notifyBlocked(this, number, d.reason)
                val builder = CallResponse.Builder()
                if (settings.rejectInsteadOfSilence) {
                    builder.setDisallowCall(true)
                        .setRejectCall(true)
                        .setSkipCallLog(false)      // keep it in the phone's call log for reference
                        .setSkipNotification(true)  // no "missed call" notification
                } else {
                    builder.setSilenceCall(true)
                }
                builder.build()
            }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "SpamShield"

        fun isContact(context: Context, number: String): Boolean {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED
            ) return false
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
            return runCatching {
                context.contentResolver.query(
                    uri, arrayOf(ContactsContract.PhoneLookup._ID), null, null, null
                )?.use { it.count > 0 } ?: false
            }.getOrDefault(false)
        }
    }
}
