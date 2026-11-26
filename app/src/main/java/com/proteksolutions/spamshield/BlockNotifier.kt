package com.proteksolutions.spamshield

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.proteksolutions.spamshield.data.PhoneNumbers

/** Posts a "call blocked" notification with one-tap Report buttons. */
object BlockNotifier {
    private const val CHANNEL_ID = "blocked_calls"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Blocked calls", NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shown when SpamShield blocks a call" }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun notifyBlocked(context: Context, number: String?, reason: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val id = (number ?: "hidden").hashCode()
        val open = activityIntent(context, MainActivity.ACTION_OPEN_BLOCKED, number, id)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_block)
            .setContentTitle("Blocked ${PhoneNumbers.format(number)}")
            .setContentText(reason)
            .setContentIntent(open)
            .setAutoCancel(true)

        if (number != null) {
            builder.addAction(0, "Report 7726",
                activityIntent(context, MainActivity.ACTION_REPORT_7726, number, id + 1))
            builder.addAction(0, "Report FTC",
                activityIntent(context, MainActivity.ACTION_REPORT_FTC, number, id + 2))
        }
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    private fun activityIntent(context: Context, action: String, number: String?, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setAction(action)
            .putExtra(MainActivity.EXTRA_NUMBER, number)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
