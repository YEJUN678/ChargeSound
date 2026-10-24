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
import com.example.widget.ChargingWidgetProvider
import com.example.wear.PhoneWearSync
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

        /** 위젯을 다시 그리는 최소 간격. 충전 중 1%마다 그리면 과합니다. */
        private const val WIDGET_MIN_INTERVAL_MS = 30_000L

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
    private var batteryReceiver: android.content.BroadcastReceiver? = null

    /**
     * 위젯 갱신 스로틀.
     *
     * ACTION_BATTERY_CHANGED 는 충전 중 1%마다, 그리고 전압/온도 변화마다
     * 훨씬 자주 발생합니다. 매번 RemoteViews 를 다시 만들면 배터리를 낭비하므로
     * [WIDGET_MIN_INTERVAL_MS] 보다 자주 갱신하지 않습니다.
     */
    private var lastWidgetUpdateMs = 0L

    /** 위젯 인스턴스가 없으면 [ChargingWidgetProvider.requestUpdate] 가 조용히 무시합니다. */
    private fun maybeUpdateWidget(context: Context) {
        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastWidgetUpdateMs < WIDGET_MIN_INTERVAL_MS) return
        lastWidgetUpdateMs = now
        ChargingWidgetProvider.requestUpdate(context)
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerDynamicPowerReceiver()
        registerBatteryLevelReceiver()
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
        unregisterBatteryLevelReceiver()
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

    /**
     * 배터리 잔량 변화를 지속적으로 감시합니다.
     *
     * 충전기가 꽂혀 있는 동안 1%마다 ACTION_BATTERY_CHANGED 가 발생하며,
     * 이를 이용해 충전 알람(목표 %)과 온도·건강도 경고를 평가합니다.
     * 충전기가 빠져 있을 때는 명시적으로 필터링하여 불필요한 연산을 피합니다.
     */
    private fun registerBatteryLevelReceiver() {
        if (batteryReceiver != null) return
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
                val appContext = ctx ?: return
                maybeUpdateWidget(appContext)
                BatteryAlertManager.evaluate(appContext)
                // 워치가 연결돼 있으면 상태를 밀어 넣습니다 (60초 스로틀).
                PhoneWearSync.requestPush(appContext)
            }
        }
        batteryReceiver = receiver
        // 이 브로드캐스트는 시스템이 보내는 sticky broadcast 이므로, 마지막 상태를
        // 즉시 받으려면 플래그를 지정해야 합니다.
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, filter)
        }
    }

    private fun unregisterBatteryLevelReceiver() {
        batteryReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to unregister battery receiver", e)
            }
        }
        batteryReceiver = null
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
