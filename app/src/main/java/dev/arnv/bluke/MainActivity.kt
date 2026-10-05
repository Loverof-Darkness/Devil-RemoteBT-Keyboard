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
    ) { result ->
        if (::btManager.isInitialized) {
            btManager.checkBluetoothCapabilities()
        }
        if (hasRequiredBluetoothPermissions(result)) {
            startBluetoothHidService()
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

        val permissionsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.all { checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED }
        } else {
            checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (permissionsGranted) {
            startBluetoothHidService()
            btManager.checkBluetoothCapabilities()
        } else {
            permissionLauncher.launch(permissions)
        }

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

    private fun hasRequiredBluetoothPermissions(result: Map<String, Boolean>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return result[android.Manifest.permission.BLUETOOTH_CONNECT] == true &&
                result[android.Manifest.permission.BLUETOOTH_ADVERTISE] == true &&
                result[android.Manifest.permission.BLUETOOTH_SCAN] == true
        }
        return result[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    private fun startBluetoothHidService() {
        val serviceIntent = Intent(this, BluetoothHidService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
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
