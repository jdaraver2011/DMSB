package com.proteksolutions.spamshield

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.app.NotificationManagerCompat
import com.proteksolutions.spamshield.report.SpamReporter
import com.proteksolutions.spamshield.ui.SpamShieldRoot
import com.proteksolutions.spamshield.ui.Tab

class MainActivity : ComponentActivity() {

    /** Bumped on resume so screens re-read the DB, role status and permissions. */
    private val refreshTick = mutableIntStateOf(0)
    private val startTab = mutableIntStateOf(Tab.HOME.ordinal)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            SpamShieldRoot(refreshTick = refreshTick.intValue, startTab = startTab)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        refreshTick.intValue++
    }

    private fun handleIntent(intent: Intent?) {
        val i = intent ?: return
        val number = i.getStringExtra(EXTRA_NUMBER)
        when (i.action) {
            ACTION_OPEN_BLOCKED -> Unit
            ACTION_REPORT_7726 -> number?.let { SpamReporter.reportTo7726(this, it) }
            ACTION_REPORT_FTC -> number?.let { SpamReporter.reportToFtc(this, it) }
            else -> return
        }
        startTab.intValue = Tab.BLOCKED.ordinal
        if (number != null) NotificationManagerCompat.from(this).cancel(number.hashCode())
        i.action = null // don't re-run on configuration change
    }

    companion object {
        const val ACTION_OPEN_BLOCKED = "com.proteksolutions.spamshield.OPEN_BLOCKED"
        const val ACTION_REPORT_7726 = "com.proteksolutions.spamshield.REPORT_7726"
        const val ACTION_REPORT_FTC = "com.proteksolutions.spamshield.REPORT_FTC"
        const val EXTRA_NUMBER = "number"
    }
}
