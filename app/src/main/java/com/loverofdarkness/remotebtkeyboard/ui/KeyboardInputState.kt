package com.loverofdarkness.remotebtkeyboard.ui

import androidx.compose.runtime.Immutable

@Immutable
data class KeyboardInputState(
    val text: String = "",
    val lastSentCount: Int = 0,
    val sending: Boolean = false
)
