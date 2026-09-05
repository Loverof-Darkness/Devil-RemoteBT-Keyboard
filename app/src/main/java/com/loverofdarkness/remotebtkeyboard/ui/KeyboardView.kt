package com.loverofdarkness.remotebtkeyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

/** Bluke-inspired keyboard surface: touch state becomes HID press/release events. */
@Composable
fun KeyboardView(
    enabled: Boolean,
    activePressedKeys: Set<Int>,
    onKeyPressChange: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = KeyboardPaletteDefaults.default
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val rows = KeyboardLayouts.rows
        val gap = 2.dp
        val unit = ((maxWidth - 12.dp) / 15.75f).coerceAtLeast(14.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(12.dp), spotColor = Color.Black.copy(alpha = .35f))
                .background(palette.background, RoundedCornerShape(12.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                rows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                        row.forEach { key ->
                            val isPressed = activePressedKeys.contains(key.keyCode)
                            KeyCap(
                                key = key,
                                width = unit * key.width,
                                height = unit,
                                pressed = isPressed,
                                palette = palette,
                                modifier = Modifier.pointerInput(enabled, key.keyCode) {
                                    awaitPointerEventScope {
                                        var wasPressed = false
                                        while (true) {
                                            val change = awaitPointerEvent().changes.firstOrNull() ?: continue
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
