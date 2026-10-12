package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChargingMonitorService : Service() {
    companion object {
        private const val TAG = "ChargingMonitorService"
        const val CHANNEL_ID = "charge_monitor_channel"
        const val NOTIFICATION_ID = 1001
        private const val ALERT_CHANNEL_ID = "charge_service_alert"
        private const val ALERT_NOTIFICATION_ID = 1003

        fun start(context: Context) {
            val intent = Intent(context, ChargingMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ChargingMonitorService::class.java)
            context.stopService(intent)
        }

        /**
         * 재부팅 후 포그라운드 서비스 시작이 차단된 경우(Android 15+),
         * 충전 감지가 꺼진 상태임을 사용자에게 알립니다.
         *
         * 조용히 실패하면 사용자는 "앱이 고장났다"고 생각하므로 반드시 알려야 합니다.
         */
        fun notifyServiceUnavailable(context: Context) {
            try {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (manager.getNotificationChannel(ALERT_CHANNEL_ID) == null) {
                        val channel = NotificationChannel(
                            ALERT_CHANNEL_ID,
                            context.getString(R.string.notif_monitor_channel),
                            NotificationManager.IMPORTANCE_DEFAULT
                        ).apply {
                            description = context.getString(R.string.notif_monitor_desc)
                        }
                        manager.createNotificationChannel(channel)
                    }
                    val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
                        .setSmallIcon(R.drawable.charge_sound_icon_1791457205401)
                        .setContentTitle(context.getString(R.string.notif_blocked_title))
                        .setContentText(context.getString(R.string.notif_blocked_text))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setContentIntent(
                            PendingIntent.getActivity(
                                context,
                                0,
                                Intent(context, MainActivity::class.java),
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        )
                        .build()
                    manager.notify(ALERT_NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to post service-unavailable notification", e)
            }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var dynamicReceiver: ChargingBroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerDynamicPowerReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        serviceScope.launch {
            val repo = SettingsRepository(applicationContext)
            val settings = repo.settingsFlow.first()
            if (!settings.isServiceEnabled) {
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterDynamicPowerReceiver()
        serviceScope.cancel()
        Log.d(TAG, "Service onDestroy")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerDynamicPowerReceiver() {
        if (dynamicReceiver == null) {
            dynamicReceiver = ChargingBroadcastReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
            }
            registerReceiver(dynamicReceiver, filter)
        }
    }

    private fun unregisterDynamicPowerReceiver() {
        dynamicReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister dynamic receiver", e)
            }
            dynamicReceiver = null
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_service_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_service_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.charge_sound_icon_1791457205401)
            .setContentTitle(getString(R.string.notif_active_title))
            .setContentText(getString(R.string.notif_active_text))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }
}
