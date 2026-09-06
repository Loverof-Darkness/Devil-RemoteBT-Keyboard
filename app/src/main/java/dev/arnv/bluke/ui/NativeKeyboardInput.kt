package dev.arnv.bluke.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private enum class NativeInputMode {
    LIVE,
    BUFFER
}

// Internal control character used only to request HID Shift+Enter.
private const val LINE_BREAK_TOKEN = "\u000B"

@Composable
fun NativeKeyboardInput(enabled: Boolean, onSend: (String) -> Int) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val prefs = remember { context.getSharedPreferences("app_prefs", 0) }

    var mode by rememberSaveable {
        mutableStateOf(
            when (prefs.getString("native_input_mode", NativeInputMode.BUFFER.name)) {
                NativeInputMode.LIVE.name -> NativeInputMode.LIVE
                else -> NativeInputMode.BUFFER
            }
        )
    }
    var text by rememberSaveable { mutableStateOf("") }
    var liveSourceText by rememberSaveable { mutableStateOf("") }

    fun selectMode(newMode: NativeInputMode) {
        mode = newMode
        prefs.edit().putString("native_input_mode", newMode.name).apply()
        liveSourceText = text
    }

    fun clearInput() {
        text = ""
        liveSourceText = ""
    }

    fun sendBufferedText() {
        if (!enabled || text.isEmpty()) return
        // WhatsApp/Messenger-style Send: transmit the composed message, then Enter.
        onSend(text + "\n")
        // Clear immediately so the sent message cannot remain in the composer.
        clearInput()
        keyboardController?.hide()
    }

    fun applyLiveTextChange(newText: String) {
        val oldText = liveSourceText
        if (!enabled) {
            liveSourceText = newText
            text = newText
            return
        }

        if (newText == oldText) return

        // Gboard/Android IMEs can report deletion of the dedicated trailing line break
        // as a one-character edit. Handle it explicitly so that the matching HID
        // Backspace is emitted even though the line break itself was sent via a control token.
        if (oldText.endsWith("\n") && newText == oldText.dropLast(1)) {
            onSend("\b")
            liveSourceText = newText
            text = newText
            return
        }

        if (newText.startsWith(oldText)) {
            val appended = newText.substring(oldText.length)
            if (appended.isNotEmpty()) onSend(appended)
        } else {
            var commonPrefix = 0
            val limit = minOf(oldText.length, newText.length)
            while (commonPrefix < limit && oldText[commonPrefix] == newText[commonPrefix]) {
                commonPrefix++
            }

            val deletedCount = oldText.length - commonPrefix
            if (deletedCount > 0) {
                onSend("\b".repeat(deletedCount))
            }

            val replacement = newText.substring(commonPrefix)
            if (replacement.isNotEmpty()) onSend(replacement)
        }

        liveSourceText = newText
        text = newText
    }

    fun sendLineBreak() {
        if (!enabled) return

        when (mode) {
            NativeInputMode.LIVE -> {
                // Locally create a new line and send the HID equivalent of Shift+Enter.
                val newText = text + "\n"
                val oldText = liveSourceText
                text = newText
                liveSourceText = newText
                if (newText.startsWith(oldText)) {
                    onSend(LINE_BREAK_TOKEN)
                }
            }
            NativeInputMode.BUFFER -> {
                text += "\n"
                liveSourceText = text
            }
        }
    }

    fun sendAction() {
        when (mode) {
            NativeInputMode.BUFFER -> sendBufferedText()
            NativeInputMode.LIVE -> {
                if (enabled) {
                    // Send button is the HID Enter action in Live mode.
                    applyLiveTextChange(text + "\n")
                    // Clear the Live composer after the Enter/send action as well.
                    clearInput()
                    keyboardController?.hide()
                }
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Native keyboard input", style = MaterialTheme.typography.titleLarge)
            Text(
                text = when (mode) {
                    NativeInputMode.LIVE -> "Live mode: typing and end-of-text backspace are sent immediately."
                    NativeInputMode.BUFFER -> "Buffer mode: compose the message first, then press Send."
                },
                style = MaterialTheme.typography.bodyMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = mode == NativeInputMode.LIVE,
                    onClick = { selectMode(NativeInputMode.LIVE) },
                    label = { Text("Live") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = mode == NativeInputMode.BUFFER,
                    onClick = { selectMode(NativeInputMode.BUFFER) },
                    label = { Text("Buffer") },
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = text,
                onValueChange = { newText ->
                    when (mode) {
                        NativeInputMode.LIVE -> applyLiveTextChange(newText)
                        NativeInputMode.BUFFER -> {
                            text = newText
                            liveSourceText = newText
                        }
                    }
                },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                label = {
                    Text(
                        when (mode) {
                            NativeInputMode.LIVE -> "Type live with your system keyboard"
                            NativeInputMode.BUFFER -> "Type a message"
                        }
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    // Keep the system keyboard in multiline editing mode. Sending is done by the in-field button.
                    imeAction = ImeAction.Default
                ),
                keyboardActions = KeyboardActions.Default,
                trailingIcon = {
                    IconButton(
                        enabled = enabled && when (mode) {
                            NativeInputMode.LIVE -> true
                            NativeInputMode.BUFFER -> text.isNotEmpty()
                        },
                        onClick = { sendAction() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send / Enter"
                        )
                    }
                }
            )

            Button(
                enabled = enabled,
                onClick = { sendLineBreak() },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardReturn,
                    contentDescription = null
                )
                Text(
                    text = "Line break (Shift+Enter)",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
