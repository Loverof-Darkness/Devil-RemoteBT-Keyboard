package com.loverofdarkness.remotebtkeyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/**
 * Bluke-inspired physical keyboard surface. Touch state is translated into real HID
 * press/release reports by the Bluetooth keyboard manager.
 */
@Composable
fun KeyboardView(
    enabled: Boolean,
    activePressedKeys: Set<Int>,
    onKeyPressChange: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = KeyboardPaletteDefaults.default
    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val rows = KeyboardLayouts.rows
        val gap = 2.dp
        val unit = ((maxWidth - 4.dp) / 15.75f).coerceAtLeast(14.dp)
        val keyHeight = unit
        val keyWidth = unit

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = .35f))
                .background(palette.background, RoundedCornerShape(12.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(gap), horizontalAlignment = Alignment.CenterHorizontally) {
                rows.forEach { row ->
                    var pointerDown by remember(row) { mutableStateOf<Int?>(null) }
                    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                        row.forEach { key ->
                            val isPressed = activePressedKeys.contains(key.keyCode)
                            KeyCap(
                                key = key,
                                width = keyWidth * key.width,
                                height = keyHeight,
                                pressed = isPressed,
                                palette = palette,
                                modifier = Modifier.pointerInput(enabled, key.keyCode) {
                                    awaitPointerEventScope {
                                        var wasPressed = false
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull() ?: continue
                                            val nowPressed = change.pressed
                                            if (enabled && nowPressed != wasPressed) {
                                                onKeyPressChange(key.keyCode, nowPressed)
                                                change.consume()
                                            }
                                            wasPressed = nowPressed
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
