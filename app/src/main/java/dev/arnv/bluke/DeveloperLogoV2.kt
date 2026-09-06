package dev.arnv.bluke

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Compact developer logo used by the About screen. */
@Composable
fun DeveloperLogoV2(modifier: Modifier = Modifier) {
    DeveloperLogo(modifier = modifier.size(56.dp))
}
