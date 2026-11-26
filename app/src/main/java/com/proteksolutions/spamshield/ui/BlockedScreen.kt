package com.proteksolutions.spamshield.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.proteksolutions.spamshield.data.BlockedCall
import com.proteksolutions.spamshield.data.PhoneNumbers
import com.proteksolutions.spamshield.data.SpamDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BlockedScreen(refreshTick: Int) {
    val ctx = LocalContext.current
    val db = remember { SpamDb.get(ctx) }
    var localTick by remember { mutableIntStateOf(0) }
    val calls by produceState(emptyList<BlockedCall>(), refreshTick, localTick) {
        value = withContext(Dispatchers.IO) { db.blockedCalls() }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${calls.size} blocked", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            if (calls.isNotEmpty()) {
                TextButton(onClick = { db.clearBlockedLog(); localTick++ }) { Text("Clear history") }
            }
        }
        if (calls.isEmpty()) {
            EmptyState("No calls blocked yet.")
            return@Column
        }
        LazyColumn {
            items(calls, key = { it.id }) { call ->
                val badges = buildList {
                    if (call.reported7726) add("Reported 7726")
                    if (call.reportedFtc) add("Reported FTC")
                }.joinToString(" · ")
                ListItem(
                    headlineContent = { Text(PhoneNumbers.format(call.number)) },
                    supportingContent = {
                        Text(
                            call.reason + " · " +
                                DateUtils.getRelativeTimeSpanString(call.timestamp) +
                                if (badges.isNotEmpty()) "\n$badges" else ""
                        )
                    },
                    trailingContent = {
                        call.number?.let { NumberActionsMenu(it) { localTick++ } }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}
