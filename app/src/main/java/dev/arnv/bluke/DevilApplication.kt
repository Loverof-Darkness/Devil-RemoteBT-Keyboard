package dev.arnv.bluke

import android.app.Application
import dev.arnv.bluke.bluetooth.BluetoothKeyboardManager
import dev.arnv.bluke.utils.DeveloperLogManager

class DevilApplication : Application() {
    lateinit var bluetoothKeyboardManager: BluetoothKeyboardManager
        private set

    override fun onCreate() {
        super.onCreate()
        DeveloperLogManager.init(this)
        bluetoothKeyboardManager = BluetoothKeyboardManager(this)
    }
}
