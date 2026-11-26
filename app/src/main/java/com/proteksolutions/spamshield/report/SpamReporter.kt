package com.proteksolutions.spamshield.report

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.proteksolutions.spamshield.data.PhoneNumbers
import com.proteksolutions.spamshield.data.Settings
import com.proteksolutions.spamshield.data.SpamDb

/**
 * Hands spam reports off to the two US reporting channels. Neither has a public API,
 * so both open a pre-filled screen and the user taps Send/Submit.
 */
object SpamReporter {

    /** FTC National Do Not Call Registry complaint form. */
    const val FTC_REPORT_URL = "https://www.donotcall.gov/report.html"

    /** Carrier spam short code (S-P-A-M), supported by AT&T, T-Mobile, Verizon and most MVNOs. */
    const val CARRIER_SHORT_CODE = "7726"

    fun reportTo7726(context: Context, number: String) {
        val template = Settings(context).sms7726Template
        val body = template.replace("{number}", PhoneNumbers.format(number))
        copyToClipboard(context, number)
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$CARRIER_SHORT_CODE"))
            .putExtra("sms_body", body)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            SpamDb.get(context).markReported(number, via7726 = true)
            Toast.makeText(
                context,
                "Number copied. Paste it when your carrier replies asking for it.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No SMS app found", Toast.LENGTH_SHORT).show()
        }
    }

    fun reportToFtc(context: Context, number: String) {
        copyToClipboard(context, number)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(FTC_REPORT_URL))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            SpamDb.get(context).markReported(number, via7726 = false)
            Toast.makeText(
                context,
                "Number ${PhoneNumbers.format(number)} copied. Paste it into the FTC form.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(context: Context, number: String) {
        val cm = context.getSystemService(ClipboardManager::class.java) ?: return
        cm.setPrimaryClip(ClipData.newPlainText("Spam number", PhoneNumbers.nationalNanp(number) ?: number))
    }
}
