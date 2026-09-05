package com.loverofdarkness.remotebtkeyboard.ui

import androidx.compose.ui.graphics.Color

enum class KeyColorCategory { ALPHA, MOD, ACCENT }

data class KeyInfo(
    val legend: String,
    val shiftedLegend: String = "",
    val width: Float = 1f,
    val keyCode: Int,
    val category: KeyColorCategory = KeyColorCategory.ALPHA
)

data class KeyboardPalette(
    val background: Color,
    val alphaBg: Color,
    val alphaText: Color,
    val modBg: Color,
    val modText: Color,
    val accentBg: Color,
    val accentText: Color
)

object KeyboardPaletteDefaults {
    val default = KeyboardPalette(
        background = Color(0xFF242427),
        alphaBg = Color(0xFFE5E1D8),
        alphaText = Color(0xFF2B2B2B),
        modBg = Color(0xFF1B1B1E),
        modText = Color(0xFFC8A392),
        accentBg = Color(0xFFE5C4B4),
        accentText = Color(0xFF242427)
    )
}

object KeyboardLayouts {
    const val CTRL = 0xE0
    const val SHIFT = 0xE1
    const val ALT = 0xE2
    const val GUI = 0xE3
    const val ENTER = 0x28
    const val ESC = 0x29
    const val BACKSPACE = 0x2A
    const val TAB = 0x2B
    const val SPACE = 0x2C
    const val DELETE = 0x4C
    const val UP = 0x52
    const val DOWN = 0x51
    const val LEFT = 0x50
    const val RIGHT = 0x4F

    private fun letter(c: Char) = KeyInfo(c.uppercase(), keyCode = 0x04 + (c - 'a'))

    val rows: List<List<KeyInfo>> = listOf(
        listOf(KeyInfo("Esc", keyCode = ESC, category = KeyColorCategory.ACCENT)) +
            listOf(
                KeyInfo("1", "!", keyCode = 0x1E), KeyInfo("2", "@", keyCode = 0x1F),
                KeyInfo("3", "#", keyCode = 0x20), KeyInfo("4", "$", keyCode = 0x21),
                KeyInfo("5", "%", keyCode = 0x22), KeyInfo("6", "^", keyCode = 0x23),
                KeyInfo("7", "&", keyCode = 0x24), KeyInfo("8", "*", keyCode = 0x25),
                KeyInfo("9", "(", keyCode = 0x26), KeyInfo("0", ")", keyCode = 0x27),
                KeyInfo("-", "_", keyCode = 0x2D), KeyInfo("=", "+", keyCode = 0x2E),
                KeyInfo("Backspace", keyCode = BACKSPACE, width = 2f, category = KeyColorCategory.MOD)
            ),
        listOf(KeyInfo("Tab", keyCode = TAB, width = 1.5f, category = KeyColorCategory.MOD)) +
            "qwertyuiop".map { letter(it) } + listOf(
                KeyInfo("[", "{", keyCode = 0x2F), KeyInfo("]", "}", keyCode = 0x30),
                KeyInfo("\\", "|", keyCode = 0x31), KeyInfo("Delete", keyCode = DELETE, width = 1.5f, category = KeyColorCategory.MOD)
            ),
        listOf(KeyInfo("Caps", keyCode = 0x39, width = 1.75f, category = KeyColorCategory.MOD)) +
            "asdfghjkl".map { letter(it) } + listOf(
                KeyInfo(";", ":", keyCode = 0x33), KeyInfo("'", "\"", keyCode = 0x34),
                KeyInfo("Enter", keyCode = ENTER, width = 2.25f, category = KeyColorCategory.ACCENT)
            ),
        listOf(KeyInfo("Shift", keyCode = SHIFT, width = 2.25f, category = KeyColorCategory.ACCENT)) +
            "zxcvbnm".map { letter(it) } + listOf(
                KeyInfo(",", "<", keyCode = 0x36), KeyInfo(".", ">", keyCode = 0x37),
                KeyInfo("/", "?", keyCode = 0x38),
                KeyInfo("Shift", keyCode = SHIFT, width = 1.75f, category = KeyColorCategory.ACCENT),
                KeyInfo("↑", keyCode = UP, category = KeyColorCategory.ACCENT)
            ),
        listOf(
            KeyInfo("Ctrl", keyCode = CTRL, width = 1.25f, category = KeyColorCategory.MOD),
            KeyInfo("Win", keyCode = GUI, width = 1.25f, category = KeyColorCategory.MOD),
            KeyInfo("Alt", keyCode = ALT, width = 1.25f, category = KeyColorCategory.MOD),
            KeyInfo("Space", keyCode = SPACE, width = 6.25f, category = KeyColorCategory.ACCENT),
            KeyInfo("Alt", keyCode = ALT, width = 1.25f, category = KeyColorCategory.MOD),
            KeyInfo("←", keyCode = LEFT, category = KeyColorCategory.ACCENT),
            KeyInfo("↓", keyCode = DOWN, category = KeyColorCategory.ACCENT),
            KeyInfo("→", keyCode = RIGHT, category = KeyColorCategory.ACCENT)
        )
    )
}
