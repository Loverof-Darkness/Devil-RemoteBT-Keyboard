package com.loverofdarkness.remotebtkeyboard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KeyCap(
    key: KeyInfo,
    width: Dp,
    height: Dp,
    pressed: Boolean,
    palette: KeyboardPalette,
    modifier: Modifier = Modifier
) {
    val (bg, text) = when (key.category) {
        KeyColorCategory.ALPHA -> palette.alphaBg to palette.alphaText
        KeyColorCategory.MOD -> palette.modBg to palette.modText
        KeyColorCategory.ACCENT -> palette.accentBg to palette.accentText
    }
    val border = bg.darker()

    Box(
        modifier = modifier.width(width).height(height).padding(1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(1.dp, RoundedCornerShape(9.dp))
                .background(border, RoundedCornerShape(9.dp))
                .border(1.dp, Color.Black.copy(alpha = .75f), RoundedCornerShape(9.dp))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 5.dp, end = 5.dp, top = 3.dp, bottom = 9.dp)
                .background(if (pressed) border else bg, RoundedCornerShape(6.dp))
                .border(1.dp, Color.Black.copy(alpha = .12f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (key.shiftedLegend.isNotEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(key.shiftedLegend, fontSize = 8.sp, color = text.copy(alpha = .72f), fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                    Text(key.legend, fontSize = 12.sp, color = text, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                }
            } else {
                Text(
                    key.legend,
                    fontSize = when {
                        key.legend.length > 7 -> 8.sp
                        key.legend.length > 4 -> 9.sp
                        else -> 12.sp
                    },
                    color = text,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )
            }
        }
    }
}

private fun Color.darker() = Color(red * .82f, green * .82f, blue * .82f, alpha)
