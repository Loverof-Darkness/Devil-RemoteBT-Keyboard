package com.loverofdarkness.remotebtkeyboard

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.loverofdarkness.remotebtkeyboard.bluetooth.BluetoothKeyboardManager
import com.loverofdarkness.remotebtkeyboard.ui.KeyboardScreen

class MainActivity : ComponentActivity() {
    private lateinit var manager: BluetoothKeyboardManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        manager.checkBluetoothCapabilities()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager = BluetoothKeyboardManager(applicationContext)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    KeyboardScreen(manager)
                }
            }
        }

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        permissionLauncher.launch(permissions)
    }

    override fun onResume() {
        super.onResume()
        if (::manager.isInitialized) manager.checkBluetoothCapabilities()
    }

    override fun onDestroy() {
        if (::manager.isInitialized) manager.close()
        super.onDestroy()
    }
}
