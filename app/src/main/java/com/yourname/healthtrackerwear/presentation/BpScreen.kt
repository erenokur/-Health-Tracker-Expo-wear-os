package com.yourname.healthtrackerwear.presentation

import android.text.InputType
import android.widget.EditText
import androidx.compose.foundation.layout.*
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material.*
import com.yourname.healthtrackerwear.data.PhoneSync
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONObject

private enum class SaveState { IDLE, SAVING, SUCCESS, FAILED }

@Composable
fun BpScreen(onSaved: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang = LocalLanguage.current

    var sys by remember { mutableStateOf("") }
    var dia by remember { mutableStateOf("") }
    var pulse by remember { mutableStateOf("") }
    var state by remember { mutableStateOf(SaveState.IDLE) }

    fun save() {
        val sysVal = sys.toIntOrNull()
        val diaVal = dia.toIntOrNull()
        if (sysVal == null || diaVal == null) {
            state = SaveState.FAILED
            return
        }
        state = SaveState.SAVING
        scope.launch {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val json = JSONObject().apply {
                put("sys", sysVal)
                put("dia", diaVal)
                put("pulse", pulse.toIntOrNull())
                put("timestamp", timestamp)
                put("note", "")
            }
            val connected = PhoneSync.hasConnectedPhone(context)
            if (!connected) {
                state = SaveState.FAILED
                return@launch
            }
            PhoneSync.sendMessage(context, "/bp-log", json.toString())
            state = SaveState.SUCCESS
        }
    }

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
    ) {
        item {
            Text(Strings.get("addBp", lang), style = MaterialTheme.typography.title3)
        }
        item { NumberField(label = Strings.get("sys", lang), value = sys, onValueChange = { sys = it }) }
        item { NumberField(label = Strings.get("dia", lang), value = dia, onValueChange = { dia = it }) }
        item { NumberField(label = Strings.get("pulse", lang), value = pulse, onValueChange = { pulse = it }) }
        item {
            Chip(
                label = {
                    Text(
                        when (state) {
                            SaveState.SAVING -> Strings.get("sending", lang)
                            SaveState.SUCCESS -> Strings.get("saved", lang)
                            SaveState.FAILED -> Strings.get("errorRetry", lang)
                            SaveState.IDLE -> Strings.get("save", lang)
                        }
                    )
                },
                onClick = { save() },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
        item {
            LaunchedEffect(state) {
                if (state == SaveState.SUCCESS) {
                    kotlinx.coroutines.delay(800)
                    onSaved()
                }
            }
        }
    }
}

/**
 * Wear Compose Material 1.x has no built-in numeric text field, so we wrap
 * a plain Android EditText — it still shows the system number keyboard when
 * tapped on a watch with a touchscreen.
 */
@Composable
fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.caption2)
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                EditText(ctx).apply {
                    inputType = InputType.TYPE_CLASS_NUMBER
                    setSingleLine(true)
                    addTextChangedListener(object : android.text.TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                        override fun afterTextChanged(s: android.text.Editable?) {
                            onValueChange(s?.toString() ?: "")
                        }
                    })
                }
            },
            update = { editText ->
                if (editText.text.toString() != value) {
                    editText.setText(value)
                    editText.setSelection(value.length)
                }
            },
        )
    }
}
