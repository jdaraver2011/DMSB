package com.proteksolutions.spamshield.ui

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.proteksolutions.spamshield.data.PhoneNumbers
import com.proteksolutions.spamshield.data.Rule
import com.proteksolutions.spamshield.data.RuleType
import com.proteksolutions.spamshield.data.SpamDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Caribbean area codes the FCC has warned about for "one ring" callback scams.
 * They look domestic but are billed as international calls.
 */
val ONE_RING_SCAM_AREA_CODES = listOf("268", "284", "473", "649", "664", "767", "809", "829", "849", "876")

@Composable
fun RulesScreen(refreshTick: Int) {
    val ctx = LocalContext.current
    val db = remember { SpamDb.get(ctx) }
    var localTick by remember { mutableIntStateOf(0) }
    var filter by remember { mutableStateOf<RuleType?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    val rules by produceState(emptyList<Rule>(), refreshTick, localTick, filter) {
        value = withContext(Dispatchers.IO) { db.rules(filter) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(selected = filter == null, onClick = { filter = null }, label = { Text("All") })
                RuleType.entries.forEach { t ->
                    FilterChip(selected = filter == t, onClick = { filter = t }, label = { Text(t.label) })
                }
            }
            if (rules.isEmpty()) {
                EmptyState("No rules yet. Tap Add to block a number, prefix or area code.")
            } else {
                LazyColumn {
                    items(rules, key = { it.id }) { rule ->
                        ListItem(
                            overlineContent = { Text(rule.type.label) },
                            headlineContent = { Text(displayValue(rule)) },
                            supportingContent = { if (rule.label.isNotBlank()) Text(rule.label) },
                            trailingContent = {
                                IconButton(onClick = { db.deleteRule(rule.id); localTick++ }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete rule")
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { showAdd = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Add") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (showAdd) {
        AddRuleDialog(
            onDismiss = { showAdd = false },
            onAdd = { type, value, label ->
                val added = addRules(db, type, value, label)
                if (added.isNotEmpty()) {
                    Toast.makeText(ctx, added, Toast.LENGTH_LONG).show()
                    showAdd = false
                    localTick++
                }
            },
            onAddPreset = {
                ONE_RING_SCAM_AREA_CODES.forEach {
                    db.addRule(Rule(type = RuleType.BLOCK_AREA_CODE, value = it, label = "One-ring scam"))
                }
                Toast.makeText(ctx, "Added ${ONE_RING_SCAM_AREA_CODES.size} area codes", Toast.LENGTH_SHORT).show()
                showAdd = false
                localTick++
            },
        )
    }
}

private fun displayValue(rule: Rule) = when (rule.type) {
    RuleType.BLOCK_NUMBER, RuleType.ALLOW_NUMBER -> PhoneNumbers.format(rule.value)
    RuleType.BLOCK_PREFIX -> "${rule.value}…"
    RuleType.BLOCK_AREA_CODE -> "(${rule.value}) xxx-xxxx"
}

/** Returns a user-facing result message, or "" if nothing valid was entered. */
private fun addRules(db: SpamDb, type: RuleType, input: String, label: String): String {
    return when (type) {
        RuleType.BLOCK_AREA_CODE -> {
            val (valid, invalid) = PhoneNumbers.parseAreaCodes(input)
            valid.forEach { db.addRule(Rule(type = type, value = it, label = label)) }
            buildString {
                if (valid.isNotEmpty()) append("Blocking area code(s): ${valid.joinToString(", ")}")
                if (invalid.isNotEmpty()) {
                    if (isNotEmpty()) append("\n")
                    append("Skipped invalid: ${invalid.joinToString(", ")}")
                }
            }.takeIf { valid.isNotEmpty() }.orEmpty()
        }
        else -> {
            if (PhoneNumbers.digits(input).isEmpty()) "" else {
                db.addRule(Rule(type = type, value = input, label = label))
                "Added ${type.label.lowercase()} rule"
            }
        }
    }
}

@Composable
private fun AddRuleDialog(
    onDismiss: () -> Unit,
    onAdd: (RuleType, String, String) -> Unit,
    onAddPreset: () -> Unit,
) {
    var type by remember { mutableStateOf(RuleType.BLOCK_AREA_CODE) }
    var value by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }

    val hint = when (type) {
        RuleType.BLOCK_AREA_CODE -> "Area codes, comma separated (e.g. 876, 809)"
        RuleType.BLOCK_PREFIX -> "Starting digits (e.g. 1800555 or 900)"
        RuleType.BLOCK_NUMBER -> "Phone number to block"
        RuleType.ALLOW_NUMBER -> "Phone number to always allow"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RuleType.entries.forEach { t ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = type == t, onClick = { type = t }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = type == t, onClick = { type = t })
                        Text(
                            when (t) {
                                RuleType.BLOCK_AREA_CODE -> "Block area code(s)"
                                RuleType.BLOCK_PREFIX -> "Block number prefix"
                                RuleType.BLOCK_NUMBER -> "Block a number"
                                RuleType.ALLOW_NUMBER -> "Always allow a number"
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(hint) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (type == RuleType.BLOCK_AREA_CODE) {
                    TextButton(onClick = onAddPreset) {
                        Text("Add known one-ring scam area codes")
                    }
                    Text(
                        "Area code rules apply to North American (+1) numbers. Contacts still ring if " +
                            "\"Contacts always ring\" is on.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(type, value, label) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
