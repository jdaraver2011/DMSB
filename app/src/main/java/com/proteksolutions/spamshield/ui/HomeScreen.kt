package com.proteksolutions.spamshield.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.proteksolutions.spamshield.data.Settings

private fun hasScreeningRole(ctx: Context): Boolean =
    ctx.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true

private fun granted(ctx: Context, perm: String) =
    ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED

private fun wantedPermissions(): Array<String> = buildList {
    add(Manifest.permission.READ_CONTACTS)
    add(Manifest.permission.READ_CALL_LOG)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

@Composable
fun HomeScreen(refreshTick: Int) {
    val ctx = LocalContext.current
    val settings = remember { Settings(ctx) }
    var localTick by remember { mutableIntStateOf(0) }
    // Re-read on resume / after a grant.
    val roleHeld = remember(refreshTick, localTick) { hasScreeningRole(ctx) }
    val missingPerms = remember(refreshTick, localTick) { wantedPermissions().filterNot { granted(ctx, it) } }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        localTick++
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        localTick++
    }

    Column(
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (roleHeld) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (roleHeld) "Protection is ON" else "Protection is OFF",
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    if (roleHeld) "SpamShield screens every incoming call against your rules."
                    else "Set SpamShield as your caller ID & spam app so it can block calls."
                )
                if (!roleHeld) {
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        val rm = ctx.getSystemService(RoleManager::class.java)
                        rm?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)?.let { roleLauncher.launch(it) }
                    }) { Text("Enable call screening") }
                }
            }
        }

        if (missingPerms.isNotEmpty()) {
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Optional permissions", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Contacts lets your contacts always ring. Call log powers the Recent tab. " +
                            "Notifications show blocked calls with Report buttons."
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { permLauncher.launch(missingPerms.toTypedArray()) }) {
                        Text("Grant permissions")
                    }
                }
            }
        }

        Text("Blocking rules", style = MaterialTheme.typography.titleMedium)
        SettingSwitch("Screening enabled", "Master switch for all rules", settings.enabled) { settings.enabled = it }
        SettingSwitch("Block hidden numbers", "Private, restricted and unknown callers", settings.blockHidden) {
            settings.blockHidden = it
        }
        SettingSwitch(
            "Block failed caller ID verification",
            "Carrier flagged the number as spoofed (STIR/SHAKEN, Android 11+)",
            settings.blockFailedVerification,
        ) { settings.blockFailedVerification = it }
        SettingSwitch(
            "Block neighbor spoofing",
            "Calls that copy your area code + first 3 digits. Needs your number below.",
            settings.blockNeighborSpoof,
        ) { settings.blockNeighborSpoof = it }
        SettingSwitch(
            "Block everyone not in contacts",
            "Aggressive. Allow-listed numbers still ring.",
            settings.blockNonContacts,
        ) { settings.blockNonContacts = it }
        SettingSwitch(
            "Contacts always ring",
            "Contacts bypass area-code and prefix rules",
            settings.alwaysAllowContacts,
        ) { settings.alwaysAllowContacts = it }

        HorizontalDivider()
        Text("Behavior", style = MaterialTheme.typography.titleMedium)
        SettingSwitch(
            "Reject blocked calls",
            "On: caller is sent away. Off: the call is silenced and rings out quietly.",
            settings.rejectInsteadOfSilence,
        ) { settings.rejectInsteadOfSilence = it }
        SettingSwitch("Notify when a call is blocked", "Includes quick Report buttons", settings.notifyOnBlock) {
            settings.notifyOnBlock = it
        }

        var myNumber by remember { mutableStateOf(settings.myNumber) }
        OutlinedTextField(
            value = myNumber,
            onValueChange = { myNumber = it; settings.myNumber = it },
            label = { Text("My phone number (for neighbor spoofing)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider()
        Text("Reporting", style = MaterialTheme.typography.titleMedium)
        var template by remember { mutableStateOf(settings.sms7726Template) }
        OutlinedTextField(
            value = template,
            onValueChange = { template = it; settings.sms7726Template = it },
            label = { Text("Text sent to 7726") },
            supportingText = {
                Text("Most carriers expect \"Call\", then reply asking for the number (it's copied to your clipboard). Use {number} to include it.")
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
