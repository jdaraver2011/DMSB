package com.proteksolutions.spamshield.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.proteksolutions.spamshield.data.PhoneNumbers
import com.proteksolutions.spamshield.data.Rule
import com.proteksolutions.spamshield.data.RuleType
import com.proteksolutions.spamshield.data.SpamDb
import com.proteksolutions.spamshield.report.SpamReporter

/** Overflow menu offering block / allow / report actions for one phone number. */
@Composable
fun NumberActionsMenu(number: String, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    val db = remember { SpamDb.get(ctx) }
    var open by remember { mutableStateOf(false) }
    val area = PhoneNumbers.areaCode(number)

    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Actions")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Report to carrier (7726)") }, onClick = {
                open = false; SpamReporter.reportTo7726(ctx, number); onChanged()
            })
            DropdownMenuItem(text = { Text("Report to FTC") }, onClick = {
                open = false; SpamReporter.reportToFtc(ctx, number); onChanged()
            })
            DropdownMenuItem(text = { Text("Block this number") }, onClick = {
                open = false
                db.addRule(Rule(type = RuleType.BLOCK_NUMBER, value = number))
                Toast.makeText(ctx, "Blocked ${PhoneNumbers.format(number)}", Toast.LENGTH_SHORT).show()
                onChanged()
            })
            if (area != null) {
                DropdownMenuItem(text = { Text("Block all of area code $area") }, onClick = {
                    open = false
                    db.addRule(Rule(type = RuleType.BLOCK_AREA_CODE, value = area))
                    Toast.makeText(ctx, "Blocking area code $area", Toast.LENGTH_SHORT).show()
                    onChanged()
                })
            }
            DropdownMenuItem(text = { Text("Always allow") }, onClick = {
                open = false
                db.addRule(Rule(type = RuleType.ALLOW_NUMBER, value = number))
                Toast.makeText(ctx, "${PhoneNumbers.format(number)} will always ring", Toast.LENGTH_SHORT).show()
                onChanged()
            })
        }
    }
}

@Composable
fun SettingSwitch(
    title: String,
    subtitle: String,
    initial: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    var checked by remember { mutableStateOf(initial) }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, enabled = enabled, onCheckedChange = { checked = it; onChange(it) })
    }
}

@Composable
fun EmptyState(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
