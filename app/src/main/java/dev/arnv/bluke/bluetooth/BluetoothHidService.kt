package dev.arnv.bluke.bluetooth

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dev.arnv.bluke.DevilApplication
import dev.arnv.bluke.MainActivity
import dev.arnv.bluke.R

class BluetoothHidService : Service() {

    companion object {
        private const val CHANNEL_ID = "bluetooth_hid_bridge"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var bluetoothKeyboardManager: BluetoothKeyboardManager

    override fun onCreate() {
        super.onCreate()
        bluetoothKeyboardManager =
            (application as DevilApplication).bluetoothKeyboardManager
        createNotificationChannel()
        startAsForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Keep the HID bridge owned by this service while the app task exists.
        // Do not recreate or close the manager when MainActivity is backgrounded.
        bluetoothKeyboardManager.checkBluetoothCapabilities()
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // stopWithTask=true also handles normal task removal. Explicitly stop here
        // so the HID profile is released when the user closes the app task.
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        // This is the lifecycle boundary for the HID bridge. Backgrounding or
        // Activity recreation never reaches this point.
        bluetoothKeyboardManager.close()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForegroundService() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Bluetooth HID bridge is active")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bluetooth HID bridge",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the Bluetooth HID bridge active while Devil RemoteBT Keyboard is running."
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }
}
