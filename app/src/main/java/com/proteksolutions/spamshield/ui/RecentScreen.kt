package com.proteksolutions.spamshield.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.proteksolutions.spamshield.data.PhoneNumbers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RecentCall(val number: String, val name: String?, val type: Int, val date: Long)

private fun loadRecentCalls(ctx: Context, max: Int = 150): List<RecentCall> {
    val out = ArrayList<RecentCall>()
    runCatching {
        ctx.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME, CallLog.Calls.TYPE, CallLog.Calls.DATE),
            null, null, "${CallLog.Calls.DATE} DESC"
        )?.use { c ->
            while (c.moveToNext() && out.size < max) {
                val number = c.getString(0)
                if (number.isNullOrBlank()) continue
                out += RecentCall(number, c.getString(1), c.getInt(2), c.getLong(3))
            }
        }
    }
    return out
}

private fun typeLabel(type: Int) = when (type) {
    CallLog.Calls.INCOMING_TYPE -> "Incoming"
    CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
    CallLog.Calls.MISSED_TYPE -> "Missed"
    CallLog.Calls.REJECTED_TYPE -> "Rejected"
    CallLog.Calls.BLOCKED_TYPE -> "Blocked"
    CallLog.Calls.VOICEMAIL_TYPE -> "Voicemail"
    else -> "Call"
}

/** Recent calls from the phone's call log, so spam that got through can be reported or blocked. */
@Composable
fun RecentScreen(refreshTick: Int) {
    val ctx = LocalContext.current
    var localTick by remember { mutableIntStateOf(0) }
    val hasPerm = remember(refreshTick, localTick) {
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { localTick++ }

    if (!hasPerm) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Allow call log access to see recent calls and report spam that slipped through.")
            Button(onClick = { launcher.launch(Manifest.permission.READ_CALL_LOG) }) { Text("Allow access") }
        }
        return
    }

    val calls by produceState(emptyList<RecentCall>(), refreshTick, localTick) {
        value = withContext(Dispatchers.IO) { loadRecentCalls(ctx) }
    }
    if (calls.isEmpty()) {
        EmptyState("No recent calls.")
        return
    }
    LazyColumn {
        itemsIndexed(calls) { _, call ->
            ListItem(
                headlineContent = { Text(call.name ?: PhoneNumbers.format(call.number)) },
                supportingContent = {
                    val sub = if (call.name != null) PhoneNumbers.format(call.number) + " · " else ""
                    Text(sub + typeLabel(call.type) + " · " + DateUtils.getRelativeTimeSpanString(call.date))
                },
                trailingContent = { NumberActionsMenu(PhoneNumbers.normalize(call.number)) { localTick++ } },
            )
            HorizontalDivider()
        }
    }
}
