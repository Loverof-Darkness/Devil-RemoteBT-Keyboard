package dev.arnv.bluke

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawscope.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp

/** Compact developer badge inspired by the generated Lover of Darkness emblem. */
@Composable
fun DeveloperLogoV2(modifier: Modifier = Modifier.size(44.dp)) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier) {
        val c = center
        val radius = size.minDimension * 0.47f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF241013), Color(0xFF050506))
            ),
            radius = radius,
            center = c
        )
        drawCircle(
            color = Color(0xFFFF2B2B),
            radius = radius * 0.92f,
            center = c,
            style = Stroke(width = size.minDimension * 0.035f)
        )

        val dragon = Path().apply {
            moveTo(c.x, c.y - radius * 0.55f)
            cubicTo(c.x + radius * 0.65f, c.y - radius * 0.8f, c.x + radius * 0.72f, c.y - radius * 0.15f, c.x + radius * 0.05f, c.y - radius * 0.08f)
            cubicTo(c.x - radius * 0.68f, c.y - radius * 0.02f, c.x - radius * 0.62f, c.y + radius * 0.48f, c.x + radius * 0.32f, c.y + radius * 0.42f)
            cubicTo(c.x + radius * 0.8f, c.y + radius * 0.38f, c.x + radius * 0.52f, c.y + radius * 0.75f, c.x, c.y + radius * 0.55f)
        }
        drawPath(
            path = dragon,
            color = Color(0xFF202226),
            style = Stroke(width = size.minDimension * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = dragon,
            color = Color(0xFF6F737B),
            style = Stroke(width = size.minDimension * 0.025f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Dragon head and eye.
        drawCircle(Color(0xFF9BA0A8), radius = size.minDimension * 0.085f, center = Offset(c.x - radius * 0.02f, c.y - radius * 0.53f))
        drawCircle(Color(0xFFFF2626), radius = size.minDimension * 0.018f, center = Offset(c.x + radius * 0.02f, c.y - radius * 0.54f))

        // Gothic "L" / "D" monogram beneath the dragon.
        val style = TextStyle(
            color = Color.White,
            fontSize = androidx.compose.ui.unit.sp(13),
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Serif
        )
        val top = textMeasurer.measure("L D", style)
        drawText(
            textLayoutResult = top,
            topLeft = Offset(c.x - top.size.width / 2f, c.y + radius * 0.43f)
        )

        drawLine(
            color = Color(0xFFFF2B2B),
            start = Offset(c.x - radius * 0.5f, c.y + radius * 0.18f),
            end = Offset(c.x + radius * 0.5f, c.y + radius * 0.18f),
            strokeWidth = size.minDimension * 0.018f
        )
        drawCircle(Color(0xFFFF2B2B), radius = size.minDimension * 0.018f, center = Offset(c.x, c.y + radius * 0.18f))
    }
}
