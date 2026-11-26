package com.proteksolutions.spamshield.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext

enum class Tab(val title: String, val icon: ImageVector) {
    HOME("Protection", Icons.Filled.Home),
    BLOCKED("Blocked", Icons.Filled.Warning),
    RECENT("Recent", Icons.Filled.Call),
    RULES("Block list", Icons.AutoMirrored.Filled.List),
}

@Composable
fun SpamShieldTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpamShieldRoot(refreshTick: Int, startTab: MutableIntState) {
    SpamShieldTheme {
        val tab = Tab.entries[startTab.intValue]
        Scaffold(
            topBar = { TopAppBar(title = { Text("SpamShield · ${tab.title}") }) },
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = t == tab,
                            onClick = { startTab.intValue = t.ordinal },
                            icon = { Icon(t.icon, contentDescription = t.title) },
                            label = { Text(t.title) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                when (tab) {
                    Tab.HOME -> HomeScreen(refreshTick)
                    Tab.BLOCKED -> BlockedScreen(refreshTick)
                    Tab.RECENT -> RecentScreen(refreshTick)
                    Tab.RULES -> RulesScreen(refreshTick)
                }
            }
        }
    }
}
