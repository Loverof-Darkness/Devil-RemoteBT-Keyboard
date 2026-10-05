package dev.arnv.bluke

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import dev.arnv.bluke.bluetooth.BluetoothKeyboardManager
import dev.arnv.bluke.bluetooth.BluetoothHidService
import dev.arnv.bluke.sound.KeyboardSoundSynthesizer
import dev.arnv.bluke.ui.theme.MyApplicationTheme
import dev.arnv.bluke.ui.HomeScreen

class MainActivity : ComponentActivity() {
    private lateinit var btManager: BluetoothKeyboardManager
    private lateinit var soundSynth: KeyboardSoundSynthesizer

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        if (::btManager.isInitialized) {
            btManager.checkBluetoothCapabilities()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (!sharedPrefs.getBoolean("has_seen_onboarding", false)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        btManager = (application as DevilApplication).bluetoothKeyboardManager
        soundSynth = KeyboardSoundSynthesizer(applicationContext)

        // The HID bridge is owned by BluetoothHidService, not by this Activity.
        // Starting the service here keeps the bridge alive while the UI is minimized,
        // recreated, or temporarily removed from the foreground.
        val serviceIntent = Intent(this, BluetoothHidService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        permissionLauncher.launch(permissions)

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                HomeScreen(
                    btManager = btManager,
                    soundSynth = soundSynth
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::btManager.isInitialized) {
            // Sync UI state only. This does not recreate or close the HID bridge.
            btManager.checkBluetoothCapabilities()
        }
    }

    override fun onDestroy() {
        // Activity destruction is no longer the HID lifecycle boundary.
        // The service owns the bridge and releases it when the app task/service closes.
        if (::soundSynth.isInitialized) {
            soundSynth.release()
        }
        super.onDestroy()
    }
}
