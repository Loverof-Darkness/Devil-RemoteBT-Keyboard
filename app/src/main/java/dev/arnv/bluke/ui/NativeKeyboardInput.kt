package dev.arnv.bluke.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.material.icons.filled.Backspace
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class NativeInputMode { LIVE, BUFFER }

// Internal control character used only to request HID Shift+Enter.
private const val LINE_BREAK_TOKEN = "\u000B"

// Invisible local-only anchor used in Live mode. It is kept at the END of the
// editable value so normal typing stays in the expected cursor position.
private const val LIVE_EDIT_ANCHOR = "\u200B"

private const val BACKSPACE_INITIAL_DELAY_MS = 400L
private const val BACKSPACE_REPEAT_INTERVAL_MS = 45L

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

    var text by rememberSaveable {
        mutableStateOf(if (mode == NativeInputMode.LIVE) LIVE_EDIT_ANCHOR else "")
    }
    var liveSourceText by rememberSaveable {
        mutableStateOf(if (mode == NativeInputMode.LIVE) LIVE_EDIT_ANCHOR else "")
    }

    fun liveVisibleValue(value: String): String = value.removeSuffix(LIVE_EDIT_ANCHOR)

    fun selectMode(newMode: NativeInputMode) {
        if (newMode == mode) return
        val visible = liveVisibleValue(text)
        mode = newMode
        text = if (newMode == NativeInputMode.LIVE) visible + LIVE_EDIT_ANCHOR else visible
        liveSourceText = text
        prefs.edit().putString("native_input_mode", newMode.name).apply()
    }

    fun clearInput() {
        text = if (mode == NativeInputMode.LIVE) LIVE_EDIT_ANCHOR else ""
        liveSourceText = text
    }

    fun sendBufferedText() {
        if (!enabled || text.isEmpty()) return
        onSend(text + "\n")
        clearInput()
        keyboardController?.hide()
    }

    fun applyLiveTextChange(newText: String) {
        if (!enabled) {
            text = newText
            liveSourceText = newText
            return
        }

        val oldRaw = liveSourceText
        val oldVisible = liveVisibleValue(oldRaw)
        val newVisible = liveVisibleValue(newText)
        val anchorWasDeleted = oldRaw.endsWith(LIVE_EDIT_ANCHOR) && !newText.endsWith(LIVE_EDIT_ANCHOR)

        if (anchorWasDeleted) {
            // The IME consumed the local anchor. That is our signal for one physical
            // Backspace at the remote host, including when the visible field is empty.
            onSend("\b")
        }

        if (newVisible != oldVisible) {
            if (newVisible.startsWith(oldVisible)) {
                val appended = newVisible.substring(oldVisible.length)
                if (appended.isNotEmpty()) onSend(appended)
            } else {
                var commonPrefix = 0
                val limit = minOf(oldVisible.length, newVisible.length)
                while (commonPrefix < limit && oldVisible[commonPrefix] == newVisible[commonPrefix]) {
                    commonPrefix++
                }

                val deletedCount = oldVisible.length - commonPrefix
                if (deletedCount > 0) onSend("\b".repeat(deletedCount))

                val replacement = newVisible.substring(commonPrefix)
                if (replacement.isNotEmpty()) onSend(replacement)
            }
        }

        // Only repair the anchor when the IME actually removed it. We deliberately do
        // not rewrite the value during ordinary typing, which preserves Gboard's cursor.
        text = if (newText.endsWith(LIVE_EDIT_ANCHOR)) newText else newVisible + LIVE_EDIT_ANCHOR
        liveSourceText = text
    }

    fun sendLineBreak() {
        if (!enabled) return

        when (mode) {
            NativeInputMode.LIVE -> {
                val visible = liveVisibleValue(text)
                text = visible + "\n" + LIVE_EDIT_ANCHOR
                liveSourceText = text
                onSend(LINE_BREAK_TOKEN)
            }
            NativeInputMode.BUFFER -> {
                text += "\n"
                liveSourceText = text
            }
        }
    }

    fun sendNativeBackspace() {
        if (!enabled) return
        // Direct physical HID Backspace. This does not depend on the Android IME
        // producing a text-edit callback, so it works repeatedly even when the
        // native composer is already empty.
        onSend("\b")
    }

    fun handleBackspacePress() {
        sendNativeBackspace()
    }

    fun handleBackspaceGesture() {
        if (!enabled) return

        // The first Backspace is immediate. Holding the button then behaves like a
        // physical keyboard key repeat: after a brief initial delay, additional HID
        // Backspace presses are emitted until the finger is released.
        androidx.compose.runtime.rememberCoroutineScope()
    }

    fun sendAction() {
        when (mode) {
            NativeInputMode.BUFFER -> sendBufferedText()
            NativeInputMode.LIVE -> {
                if (enabled) {
                    onSend("\n")
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
                    NativeInputMode.LIVE -> "Live mode: typing and backspace are sent immediately."
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    enabled = enabled,
                    onClick = { },
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(enabled) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)

                                sendNativeBackspace()

                                var repeatJob: Job? = null
                                try {
                                    kotlinx.coroutines.coroutineScope {
                                        repeatJob = launch {
                                            delay(BACKSPACE_INITIAL_DELAY_MS)
                                            while (true) {
                                                sendNativeBackspace()
                                                delay(BACKSPACE_REPEAT_INTERVAL_MS)
                                            }
                                        }

                                        awaitPointerRelease()
                                    }
                                } finally {
                                    repeatJob?.cancel()
                                }
                            }
                        },
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Backspace"
                    )
                    Text(
                        text = "Backspace",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Button(
                    enabled = enabled,
                    onClick = { sendLineBreak() },
                    modifier = Modifier.weight(1f),
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
}
