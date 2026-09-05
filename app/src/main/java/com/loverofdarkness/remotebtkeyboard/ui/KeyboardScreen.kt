package com.loverofdarkness.remotebtkeyboard.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.loverofdarkness.remotebtkeyboard.bluetooth.BluetoothKeyboardManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardScreen(manager: BluetoothKeyboardManager) {
    val context = LocalContext.current
    val state by manager.state.collectAsState()
    val status by manager.status.collectAsState()
    val devices by manager.bondedDevices.collectAsState()
    val connected by manager.connectedDevice.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    var selectedAddress by remember { mutableStateOf<String?>(null) }
    var nativeText by remember { mutableStateOf("") }
    var sentCount by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    val canReadBluetoothDevice = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(devices, connected?.address) {
        if (selectedAddress == null || devices.none { it.address == selectedAddress }) {
            selectedAddress = connected?.address ?: devices.firstOrNull()?.address
        }
    }

    LaunchedEffect(Unit) {
        manager.checkBluetoothCapabilities()
        while (true) {
            delay(5000)
            manager.checkBluetoothCapabilities()
        }
    }

    val selectedDevice = devices.firstOrNull { it.address == selectedAddress }
    val selectedDeviceLabel = if (!canReadBluetoothDevice) {
        "Bluetooth device details unavailable until Nearby devices permission is granted"
    } else {
        selectedDevice?.let { "${it.name ?: "Bluetooth device"}\n${it.address}" } ?: "No paired devices"
    }
    val isConnected = connected?.address == selectedAddress

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Devil RemoteBT Keyboard", style = MaterialTheme.typography.headlineSmall)
                    Text("Bluetooth HID + native Android IME", style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = manager::refresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Bluetooth")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null)
                        Text(status, style = MaterialTheme.typography.bodyLarge)
                    }
                    Text(
                        when (state) {
                            is com.loverofdarkness.remotebtkeyboard.bluetooth.BluetoothState.Connected -> "HID keyboard connected"
                            com.loverofdarkness.remotebtkeyboard.bluetooth.BluetoothState.ReadyDisconnected -> "Ready — select a paired computer"
                            com.loverofdarkness.remotebtkeyboard.bluetooth.BluetoothState.BluetoothOff -> "Bluetooth is off"
                            else -> "HID status"
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        OutlinedTextField(
                            value = selectedDeviceLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Paired computer") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            devices.forEach { device ->
                                val deviceLabel = if (canReadBluetoothDevice) {
                                    "${device.name ?: "Bluetooth device"}\n${device.address}"
                                } else {
                                    "Bluetooth device"
                                }
                                DropdownMenuItem(
                                    text = { Text(deviceLabel) },
                                    onClick = {
                                        if (canReadBluetoothDevice) selectedAddress = device.address
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = selectedDevice != null && !isConnected && canReadBluetoothDevice,
                            onClick = { selectedDevice?.let(manager::connectDevice) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Connect") }
                        TextButton(enabled = isConnected, onClick = manager::disconnect) {
                            Text("Disconnect")
                        }
                    }
                    Button(
                        onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Open Bluetooth settings") }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Native keyboard input", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "This field uses the phone's normal Android input method. Gboard, Samsung Keyboard, voice input, and other installed IMEs can type here.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = nativeText,
                        onValueChange = { nativeText = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        enabled = isConnected,
                        minLines = 3,
                        maxLines = 6,
                        label = { Text("Type with Gboard / system keyboard") },
                        placeholder = { Text("Write here, then send to the computer") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (nativeText.isNotEmpty()) sentCount = manager.sendText(nativeText)
                            keyboardController?.hide()
                        })
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = isConnected,
                            onClick = {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Show keyboard") }
                        Button(
                            enabled = isConnected && nativeText.isNotEmpty(),
                            onClick = { sentCount = manager.sendText(nativeText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                            Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                            Text("Send to laptop")
                        }
                    }
                    if (sentCount > 0) {
                        Text("Queued $sentCount supported characters.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item { Text("On-screen keyboard", style = MaterialTheme.typography.titleLarge) }
        item {
            KeyboardView(
                enabled = isConnected,
                activePressedKeys = emptySet(),
                onKeyPressChange = manager::sendKey
            )
        }
    }
}
