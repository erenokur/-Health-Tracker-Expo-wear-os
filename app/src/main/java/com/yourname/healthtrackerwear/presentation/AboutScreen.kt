package com.yourname.healthtrackerwear.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.*
import com.yourname.healthtrackerwear.data.PhoneSync
import kotlinx.coroutines.launch

private enum class ConnectionState { CHECKING, CONNECTED, DISCONNECTED }

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang = LocalLanguage.current
    val toggleLanguage = LocalOnToggleLanguage.current

    var connection by remember { mutableStateOf(ConnectionState.CHECKING) }

    // Check connection on first open.
    LaunchedEffect(Unit) {
        connection = if (PhoneSync.hasConnectedPhone(context))
            ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
    }

    fun refreshConnection() {
        connection = ConnectionState.CHECKING
        scope.launch {
            connection = if (PhoneSync.hasConnectedPhone(context))
                ConnectionState.CONNECTED else ConnectionState.DISCONNECTED
        }
    }

    // Read version name from PackageManager — no BuildConfig changes required.
    val versionName = remember {
        try {
            context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "—"
        } catch (_: Exception) { "—" }
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(
                text = Strings.get("aboutTitle", lang),
                style = MaterialTheme.typography.title3,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }

        // Version
        item {
            Text(
                text = "${Strings.get("version", lang)}: $versionName",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        // Connection status chip — tappable to refresh
        item {
            val (statusText, statusColor) = when (connection) {
                ConnectionState.CHECKING     ->
                    Strings.get("checkingConnection", lang) to MaterialTheme.colors.onSurfaceVariant
                ConnectionState.CONNECTED    ->
                    Strings.get("phoneConnected", lang) to MaterialTheme.colors.secondary
                ConnectionState.DISCONNECTED ->
                    Strings.get("phoneDisconnected", lang) to MaterialTheme.colors.error
            }
            Chip(
                label = { Text(statusText, maxLines = 1) },
                onClick = { refreshConnection() },
                colors = ChipDefaults.chipColors(
                    contentColor = statusColor,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 3.dp),
            )
        }

        // Language toggle — moved here from main menu
        item {
            Chip(
                label = {
                    Text(
                        "${Strings.get("language", lang)}: ${Strings.get("languageToggle", lang)}",
                        maxLines = 1,
                    )
                },
                onClick = { toggleLanguage() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 3.dp),
            )
        }
    }
}
