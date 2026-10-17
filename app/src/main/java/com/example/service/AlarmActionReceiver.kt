package com.example.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 충전 알림의 액션 버튼 처리.
 *
 * 알림에 [ACTION_SNOOZE] / [ACTION_DISMISS] 버튼을 붙이고, 이 리시버가 받습니다.
 * 앱을 열지 않고 바로 처리되므로 알림을 실수로 넘쳤을 때 잃어버리는 일을 막습니다.
 */
class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        when (intent.action) {
            ACTION_SNOOZE -> {
                val minutes = intent.getIntExtra(EXTRA_MINUTES, 10)
                Log.d(TAG, "Snooze alarm for $minutes min")
                scope.launch {
                    try {
                        SettingsRepository(appContext).snoozeAlarm(minutes)
                    } catch (e: Exception) {
                        Log.w(TAG, "snooze failed", e)
                    }
                }
            }
            ACTION_DISMISS -> Log.d(TAG, "Alarm dismissed")
            else -> return
        }
        dismissNotification(appContext)
    }

    private fun dismissNotification(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(BatteryAlertManager.ALARM_NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.w(TAG, "cancel failed", e)
        }
    }

    companion object {
        private const val TAG = "AlarmActionReceiver"

        const val ACTION_SNOOZE = "com.chargesound.app.action.SNOOZE"
        const val ACTION_DISMISS = "com.chargesound.app.action.DISMISS_ALARM"
        const val EXTRA_MINUTES = "extra_minutes"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        fun snoozePendingIntent(context: Context, minutes: Int): PendingIntent {
            val intent = Intent(context, AlarmActionReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_MINUTES, minutes)
            }
            return PendingIntent.getBroadcast(
                context,
                1001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun dismissPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, AlarmActionReceiver::class.java).apply {
                action = ACTION_DISMISS
            }
            return PendingIntent.getBroadcast(
                context,
                1002,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}