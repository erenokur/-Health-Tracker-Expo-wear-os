package com.yourname.healthtrackerwear.presentation

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.*
import androidx.wear.input.RemoteInputIntentHelper
import com.yourname.healthtrackerwear.data.MedicineRepository
import com.yourname.healthtrackerwear.data.PhoneSync
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val EXTRA_MED_NAME = "med_name_input"
private enum class MedSaveState { IDLE, SAVING, SUCCESS, FAILED }

@Composable
fun MedScreen(onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang = LocalLanguage.current

    // Reactively collect the medication list from the repository.
    // When the phone pushes a new /med-list while this screen is open,
    // the list will update instantly without needing a screen restart.
    val medicines by MedicineRepository.medicineList.collectAsState()

    var medName by remember { mutableStateOf("") }
    var isHungry by remember { mutableStateOf(true) } // true = Aç/Empty, false = Tok/Full
    var state by remember { mutableStateOf(MedSaveState.IDLE) }

    // On screen open: ask the phone to re-push its current medication list.
    // This handles cold starts and long disconnection periods where the cached
    // list may be stale or empty.
    LaunchedEffect(Unit) {
        scope.launch { PhoneSync.requestMedicineList(context) }
    }

    val voiceInputLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val results = android.app.RemoteInput.getResultsFromIntent(result.data!!)
            val spoken = results?.getCharSequence(EXTRA_MED_NAME)
            if (spoken != null) medName = spoken.toString()
        }
    }

    fun launchNameInput() {
        val remoteInputs = listOf(
            android.app.RemoteInput.Builder(EXTRA_MED_NAME)
                .setLabel(Strings.get("medNameLabel", lang))
                .setAllowFreeFormInput(true)
                .build(),
        )
        val intent: Intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
        voiceInputLauncher.launch(intent)
    }

    fun save() {
        if (medName.isBlank()) {
            state = MedSaveState.FAILED
            return
        }
        state = MedSaveState.SAVING
        scope.launch {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            // Meal type is always sent to the phone in Turkish ("Aç"/"Tok")
            // regardless of the watch's display language, since that's the
            // exact value the phone app's DB and UI expect.
            val json = JSONObject().apply {
                put("medName", medName.trim())
                put("mealType", if (isHungry) "Aç" else "Tok")
                put("timestamp", timestamp)
            }
            val connected = PhoneSync.hasConnectedPhone(context)
            if (!connected) {
                state = MedSaveState.FAILED
                return@launch
            }
            PhoneSync.sendMessage(context, "/med-log", json.toString())
            state = MedSaveState.SUCCESS
        }
    }

    LaunchedEffect(state) {
        if (state == MedSaveState.SUCCESS) {
            kotlinx.coroutines.delay(800)
            onSaved()
        }
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Text(Strings.get("medTaken", lang), style = MaterialTheme.typography.title3)
        }

        if (medicines.isNotEmpty()) {
            // ── Phone-synced list: radio-chip picker ──────────────────────────
            items(medicines) { name ->
                val isSelected = medName == name
                ToggleChip(
                    checked = isSelected,
                    onCheckedChange = { checked -> medName = if (checked) name else "" },
                    label = { Text(name, maxLines = 1) },
                    toggleControl = {
                        Icon(
                            imageVector = ToggleChipDefaults.radioIcon(checked = isSelected),
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }

            // "Other" chip: opens voice/keyboard for unlisted medicines.
            // Shows the typed name when a custom value has been entered.
            item {
                val isCustomSelected = medName.isNotBlank() && medName !in medicines
                Chip(
                    label = {
                        Text(
                            if (isCustomSelected) medName
                            else Strings.get("otherMed", lang),
                            maxLines = 1,
                        )
                    },
                    onClick = { launchNameInput() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }
        } else {
            // ── Fallback: no synced list yet ─────────────────────────────────
            item {
                Text(
                    text = Strings.get("medListEmpty", lang),
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                )
            }
            item {
                Text(
                    text = Strings.get("medListHint", lang),
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 4.dp),
                )
            }
            // "Refresh" — re-requests the list from the phone immediately
            item {
                Chip(
                    label = { Text(Strings.get("refreshList", lang), maxLines = 1) },
                    onClick = {
                        scope.launch { PhoneSync.requestMedicineList(context) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }
            // Manual text entry fallback
            item {
                Chip(
                    label = {
                        Text(
                            if (medName.isBlank()) Strings.get("medNamePrompt", lang)
                            else medName,
                            maxLines = 1,
                        )
                    },
                    onClick = { launchNameInput() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                )
            }
        }

        item {
            ToggleChip(
                checked = isHungry,
                onCheckedChange = { isHungry = it },
                label = {
                    Text(if (isHungry) Strings.get("hungry", lang) else Strings.get("full", lang))
                },
                toggleControl = {
                    Icon(
                        imageVector = ToggleChipDefaults.switchIcon(checked = isHungry),
                        contentDescription = null,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 3.dp),
            )
        }

        item {
            Chip(
                label = {
                    Text(
                        when (state) {
                            MedSaveState.SAVING  -> Strings.get("sending", lang)
                            MedSaveState.SUCCESS -> Strings.get("saved", lang)
                            MedSaveState.FAILED  -> Strings.get("errorRetry", lang)
                            MedSaveState.IDLE    -> Strings.get("saveAsTaken", lang)
                        },
                    )
                },
                onClick = { save() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 3.dp),
            )
        }
    }
}
