package dev.arnv.bluke

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.core.net.toUri
import dev.arnv.bluke.ui.SettingsCardGroup
import dev.arnv.bluke.ui.SettingsGroup
import dev.arnv.bluke.ui.SettingsItemData
import dev.arnv.bluke.ui.theme.MyApplicationTheme
import dev.arnv.bluke.ui.theme.getCookieShape

class AboutActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                fun openUrl(url: String) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                }

                val logoShape = getCookieShape(7)
                val developerShape = getCookieShape(7)
                var clickCount by remember { mutableIntStateOf(0) }
                val sharedPrefs = context.getSharedPreferences("app_prefs", MODE_PRIVATE)
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        LargeTopAppBar(
                            title = { Text("About") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                                }
                            },
                            colors = TopAppBarDefaults.largeTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                titleContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            scrollBehavior = scrollBehavior
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            shape = logoShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(100.dp)
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                ) {
                                    clickCount++
                                    if (clickCount == 5) {
                                        sharedPrefs.edit().putBoolean("is_developer_mode", true).apply()
                                        android.widget.Toast.makeText(context, "You are now a developer!", android.widget.Toast.LENGTH_SHORT).show()
                                    } else if (clickCount > 5 && clickCount % 3 == 0) {
                                        android.widget.Toast.makeText(context, "No need, you are already a developer.", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Image(
                                painter = painterResource(id = R.mipmap.ic_launcher),
                                contentDescription = "App Logo",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Text(
                            text = getString(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Version $versionName",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(bottom = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = { openUrl("https://github.com/Loverof-Darkness/Devil-RemoteBT-Keyboard") },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_github),
                                        contentDescription = "GitHub Repository",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "GitHub",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        SettingsGroup(title = "Developer") {
                            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = developerShape,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .padding(end = 16.dp)
                                            .size(44.dp)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_wordmark),
                                                contentDescription = "Project logo",
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Loverof-Darkness",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "@Loverof-Darkness",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PlayfulSocialButton(
                                        onClick = { openUrl("https://github.com/Loverof-Darkness/Devil-RemoteBT-Keyboard") },
                                        label = "GitHub",
                                        icon = {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_github),
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        SettingsGroup(title = "Credits & Attribution") {
                            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)) {
                                Text(
                                    text = "Original Project — Bluke",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "This project is based on and derived from Bluke by Arnav Kumar (@arnav-kr). The original project provides the core Bluetooth HID architecture and original application functionality on which Devil RemoteBT Keyboard is built.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                PlayfulSocialButton(
                                    onClick = { openUrl("https://github.com/arnav-kr/Bluke") },
                                    label = "Original Bluke",
                                    icon = {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_github),
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }

                        SettingsCardGroup(
                            title = "App",
                            items = listOf(
                                SettingsItemData(
                                    title = "Changelogs",
                                    subtitle = "History of all the changes made to Devil RemoteBT Keyboard",
                                    icon = { Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = { openUrl("https://github.com/Loverof-Darkness/Devil-RemoteBT-Keyboard/releases") }
                                ),
                                SettingsItemData(
                                    title = "Licenses",
                                    subtitle = "View the licenses that the app and libraries are using",
                                    icon = { Icon(Icons.Default.Policy, null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = { startActivity(Intent(this@AboutActivity, LicensesActivity::class.java)) }
                                ),
                                SettingsItemData(
                                    title = "Report issue",
                                    subtitle = "Report an issue or bug in Devil RemoteBT Keyboard",
                                    icon = { Icon(Icons.Default.BugReport, null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = { openUrl("https://github.com/Loverof-Darkness/Devil-RemoteBT-Keyboard/issues/new?template=bug_report.md") }
                                ),
                                SettingsItemData(
                                    title = "Feature request",
                                    subtitle = "Suggest improvements or new features for Devil RemoteBT Keyboard",
                                    icon = { Icon(Icons.Default.Lightbulb, null, tint = MaterialTheme.colorScheme.primary) },
                                    onClick = { openUrl("https://github.com/Loverof-Darkness/Devil-RemoteBT-Keyboard/issues/new?template=feature_request.md") }
                                )
                            )
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PlayfulSocialButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
