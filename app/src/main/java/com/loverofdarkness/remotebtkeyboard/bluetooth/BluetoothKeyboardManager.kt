package com.loverofdarkness.remotebtkeyboard.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

sealed class BluetoothState {
    data object Unsupported : BluetoothState()
    data object PermissionRequired : BluetoothState()
    data object BluetoothOff : BluetoothState()
    data object ProfileNotSupported : BluetoothState()
    data object ReadyDisconnected : BluetoothState()
    data class Connected(val deviceName: String) : BluetoothState()
}

/**
 * HID transport derived from Bluke's BluetoothKeyboardManager architecture.
 * This application intentionally exposes only the keyboard transport.
 */
@SuppressLint("MissingPermission")
class BluetoothKeyboardManager(private val context: Context) {
    companion object {
        private const val TAG = "DevilBluetooth"
        private const val REPORT_ID_KEYBOARD = 1
        private const val HID_DEVICE_PROFILE = 19
    }

    private val adapter: BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val reportExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_FOREGROUND)
            runnable.run()
        }, "bt-report-sender")
    }
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var timeoutFuture: ScheduledFuture<*>? = null

    private var hid: BluetoothHidDevice? = null
    private var registered = false
    private var registering = false
    private var pendingDevice: BluetoothDevice? = null
    private var closed = false

    private val _state = MutableStateFlow<BluetoothState>(BluetoothState.ReadyDisconnected)
    val state: StateFlow<BluetoothState> = _state

    private val _status = MutableStateFlow("Initializing Bluetooth HID…")
    val status: StateFlow<String> = _status

    private val _bondedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val bondedDevices: StateFlow<List<BluetoothDevice>> = _bondedDevices

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hid = proxy as BluetoothHidDevice
            Log.d(TAG, "Bluetooth HID Device proxy ready")
            registerApp()
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile != BluetoothProfile.HID_DEVICE) return
            hid = null
            registered = false
            registering = false
            _connectedDevice.value = null
            _state.value = BluetoothState.ProfileNotSupported
            _status.value = "Bluetooth HID service disconnected; waiting for Android to restore it."
        }
    }

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, ok: Boolean) {
            registered = ok
            registering = false
            if (!ok) {
                _connectedDevice.value = null
                _state.value = BluetoothState.ReadyDisconnected
                _status.value = "HID keyboard registration stopped."
                return
            }

            _state.value = BluetoothState.ReadyDisconnected
            _status.value = "Phone is registered as a Bluetooth keyboard."
            val queued = pendingDevice
            if (queued != null) {
                pendingDevice = null
                scope.launch {
                    delay(350)
                    connectDevice(queued)
                }
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, connectionState: Int) {
            when (connectionState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    timeoutFuture?.cancel(false)
                    _connectedDevice.value = device
                    _state.value = BluetoothState.Connected(device.name ?: device.address)
                    _status.value = "Connected as a Bluetooth keyboard."
                    resetKeyboardState()
                }
                BluetoothProfile.STATE_CONNECTING -> {
                    _status.value = "Connecting to ${device.name ?: device.address} as a keyboard…"
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    timeoutFuture?.cancel(false)
                    if (_connectedDevice.value?.address == device.address) {
                        _connectedDevice.value = null
                    }
                    _state.value = BluetoothState.ReadyDisconnected
                    _status.value = "Keyboard connection disconnected."
                    resetKeyboardState()
                }
            }
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            if (type == BluetoothHidDevice.REPORT_TYPE_OUTPUT && id.toInt() == REPORT_ID_KEYBOARD) {
                val leds = if (data.size > 1 && data[0].toInt() == REPORT_ID_KEYBOARD) data[1].toInt() else data.firstOrNull()?.toInt() ?: 0
                Log.d(TAG, "Host LED state: caps=${(leds and 2) != 0}, num=${(leds and 1) != 0}, scroll=${(leds and 4) != 0}")
            }
            try { hid?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS) } catch (_: Exception) { }
        }

        override fun onInterruptData(device: BluetoothDevice, reportId: Byte, data: ByteArray) {
            Log.d(TAG, "Host interrupt report id=$reportId size=${data.size}")
        }
    }

    private val hidDescriptor = byteArrayOf(
        0x05, 0x01,             // Usage Page (Generic Desktop)
        0x09, 0x06,             // Usage (Keyboard)
        0xA1.toByte(), 0x01,    // Collection (Application)
        0x85, REPORT_ID_KEYBOARD.toByte(),
        0x05, 0x07,
        0x19.toByte(), 0xE0.toByte(),
        0x29, 0xE7.toByte(),
        0x15, 0x00,
        0x25, 0x01,
        0x75, 0x01,
        0x95, 0x08,
        0x81.toByte(), 0x02,
        0x95, 0x01,
        0x75, 0x08,
        0x81.toByte(), 0x01,
        0x95, 0x05,
        0x75, 0x01,
        0x05, 0x08,
        0x19, 0x01,
        0x29, 0x05,
        0x91.toByte(), 0x02,
        0x95, 0x01,
        0x75, 0x03,
        0x91.toByte(), 0x01,
        0x95, 0x06,
        0x75, 0x08,
        0x15, 0x00,
        0x25, 0x65,
        0x05, 0x07,
        0x19, 0x00,
        0x29, 0x65,
        0x81.toByte(), 0x00,
        0xC0.toByte()
    )

    private val sdpSettings by lazy {
        BluetoothHidDeviceAppSdpSettings(
            "Devil RemoteBT Keyboard",
            "Bluetooth keyboard",
            "Devil RemoteBT Keyboard",
            BluetoothHidDevice.SUBCLASS1_KEYBOARD,
            hidDescriptor
        )
    }

    private var modifiers = 0
    private val keys = ByteArray(6)

    init { checkBluetoothCapabilities() }

    fun checkBluetoothCapabilities() {
        if (adapter == null) {
            _state.value = BluetoothState.Unsupported
            _status.value = "Bluetooth is not available on this phone."
            return
        }
        if (!adapter.isEnabled) {
            _state.value = BluetoothState.BluetoothOff
            _status.value = "Turn Bluetooth on to use keyboard mode."
            return
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val c = context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val a = context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val s = context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!c || !a || !s) {
                _state.value = BluetoothState.PermissionRequired
                _status.value = "Nearby devices permission is required."
                return
            }
        }
        refreshBondedDevices()
        if (hid == null) connectProfileProxy() else if (!registered) registerApp()
    }

    private fun connectProfileProxy() {
        val a = adapter ?: return
        try {
            if (!a.getProfileProxy(context, profileListener, HID_DEVICE_PROFILE)) {
                _state.value = BluetoothState.ProfileNotSupported
                _status.value = "Android HID Device profile is unavailable on this phone."
            }
        } catch (t: Throwable) {
            Log.e(TAG, "getProfileProxy failed", t)
            _state.value = BluetoothState.ProfileNotSupported
            _status.value = "Could not access Android's HID Device profile."
        }
    }

    private fun refreshBondedDevices() {
        try { _bondedDevices.value = adapter?.bondedDevices?.toList().orEmpty() } catch (_: Exception) { }
    }

    fun connectDevice(device: BluetoothDevice) {
        if (closed) return
        if (device.bondState != BluetoothDevice.BOND_BONDED) {
            _status.value = "Pair ${device.name ?: device.address} in Android Bluetooth settings first."
            return
        }
        pendingDevice = device
        if (hid == null) {
            _status.value = "Waiting for Android HID service…"
            connectProfileProxy()
            return
        }
        if (!registered) {
            _status.value = "Waiting for HID registration…"
            registerApp()
            return
        }

        val profile = hid ?: return
        pendingDevice = null
        val name = device.name ?: device.address
        _status.value = "Connecting to $name as a keyboard…"

        scope.launch {
            try {
                val active = profile.connectedDevices.any { it.address == device.address }
                if (active) {
                    profile.disconnect(device)
                    delay(350)
                }

                var accepted = profile.connect(device)
                Log.d(TAG, "hid.connect($name)=$accepted")
                if (!accepted) {
                    delay(500)
                    accepted = profile.connect(device)
                    Log.d(TAG, "hid.connect($name) retry=$accepted")
                }
                if (!accepted) {
                    _status.value = "Android rejected the HID connection request."
                    return@launch
                }
                with(java.util.concurrent.TimeUnit.SECONDS) {
                    timeoutFuture?.cancel(false)
                    timeoutFuture = scheduler.schedule({
                        if (_connectedDevice.value == null) {
                            _status.value = "Connection timed out. Keep the laptop paired and retry."
                        }
                    }, 15, java.util.concurrent.TimeUnit.SECONDS)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "HID connection failed", t)
                _status.value = "HID connection failed: ${t.message ?: "unknown error"}"
            }
        }
    }

    private fun registerApp() {
        val profile = hid ?: return
        if (registering || registered) return
        registering = true
        scope.launch {
            try {
                try { profile.unregisterApp() } catch (_: Exception) { }
                delay(300)
                var ok = false
                repeat(3) { attempt ->
                    if (ok) return@repeat
                    try {
                        ok = profile.registerApp(sdpSettings, null, null, reportExecutor, hidCallback)
                        Log.d(TAG, "registerApp attempt=${attempt + 1} ok=$ok")
                    } catch (t: Throwable) {
                        Log.w(TAG, "registerApp attempt=${attempt + 1} failed", t)
                    }
                    if (!ok) delay(400)
                }
                if (!ok) {
                    registering = false
                    _state.value = BluetoothState.ProfileNotSupported
                    _status.value = "HID registration failed. Toggle Bluetooth and retry."
                }
            } finally {
                registering = false
            }
        }
    }

    fun sendKey(keyCode: Int, pressed: Boolean) {
        val device = _connectedDevice.value ?: return
        synchronized(keys) {
            if (keyCode in 0xE0..0xE7) {
                val mask = 1 shl (keyCode - 0xE0)
                modifiers = if (pressed) modifiers or mask else modifiers and mask.inv()
            } else if (pressed) {
                if (keys.none { it.toInt() == keyCode }) {
                    val slot = keys.indexOfFirst { it.toInt() == 0 }
                    if (slot >= 0) keys[slot] = keyCode.toByte()
                }
            } else {
                for (i in keys.indices) if (keys[i].toInt() == keyCode) keys[i] = 0
                val compact = keys.filter { it.toInt() != 0 }.toByteArray()
                keys.fill(0)
                compact.copyInto(keys)
            }
            val report = ByteArray(8)
            report[0] = modifiers.toByte()
            keys.copyInto(report, destinationOffset = 2)
            submitReport(device, REPORT_ID_KEYBOARD, report)
        }
    }

    private fun submitReport(device: BluetoothDevice, reportId: Int, report: ByteArray) {
        val profile = hid ?: return
        reportExecutor.execute {
            try {
                val accepted = profile.sendReport(device, reportId, report)
                Log.d(TAG, "sendReport id=$reportId accepted=$accepted data=${report.joinToString(" ") { "%02X".format(it) }}")
            } catch (t: Throwable) {
                Log.e(TAG, "sendReport failed", t)
            }
        }
    }

    fun resetKeyboardState() {
        synchronized(keys) {
            modifiers = 0
            keys.fill(0)
        }
    }

    fun close() {
        if (closed) return
        closed = true
        timeoutFuture?.cancel(false)
        resetKeyboardState()
        try { hid?.unregisterApp() } catch (_: Exception) { }
        try { adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hid) } catch (_: Exception) { }
        hid = null
        registered = false
        reportExecutor.shutdownNow()
        scheduler.shutdownNow()
        scope.coroutineContext[Job]?.cancel()
    }
}
