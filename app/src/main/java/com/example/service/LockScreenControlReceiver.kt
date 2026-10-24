package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 잠금 화면에서도 충전 화면을 열지 않고 핵심 설정을 바꾸는 알림 컨트롤입니다. */
class LockScreenControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = SettingsRepository(context.applicationContext)
                val settings = repo.settingsFlow.first()
                when (intent.action) {
                    ACTION_TOGGLE_ANIMATION -> repo.setAnimationEnabled(!settings.animationEnabled)
                    ACTION_TOGGLE_SOUND -> repo.setSoundEnabled(!settings.soundEnabled)
                    ACTION_NEXT_TARGET -> {
                        val choices = listOf(80, 85, 90, 100)
                        val next = choices.firstOrNull { it > settings.alarmTargetPercent } ?: choices.first()
                        repo.setAlarmTargets((settings.alarmTargets.dropLast(1) + next).distinct().sorted())
                    }
                }
                show(context.applicationContext, repo.settingsFlow.first())
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_ANIMATION = "com.chargesound.app.TOGGLE_ANIMATION"
        const val ACTION_TOGGLE_SOUND = "com.chargesound.app.TOGGLE_SOUND"
        const val ACTION_NEXT_TARGET = "com.chargesound.app.NEXT_TARGET"
        private const val CHANNEL_ID = "charging_controls"
        private const val NOTIFICATION_ID = 2010

        fun show(context: Context, settings: com.example.data.ChargeSettings) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.lock_control_channel),
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = context.getString(R.string.lock_control_channel_sub) })
            }
            fun action(action: String, label: String, requestCode: Int) = NotificationCompat.Action.Builder(
                0, label,
                PendingIntent.getBroadcast(context, requestCode, Intent(context, LockScreenControlReceiver::class.java).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            ).build()
            val openApp = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            manager.notify(NOTIFICATION_ID, NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.charge_sound_icon_1791457205401)
                .setContentTitle(context.getString(R.string.lock_control_title))
                .setContentText(context.getString(R.string.lock_control_summary, settings.alarmTargetPercent))
                .setContentIntent(openApp)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(action(ACTION_TOGGLE_ANIMATION, context.getString(if (settings.animationEnabled) R.string.lock_control_animation_off else R.string.lock_control_animation_on), 1))
                .addAction(action(ACTION_TOGGLE_SOUND, context.getString(if (settings.soundEnabled) R.string.lock_control_sound_off else R.string.lock_control_sound_on), 2))
                .addAction(action(ACTION_NEXT_TARGET, context.getString(R.string.lock_control_target), 3))
                .build())
        }

        fun hide(context: Context) {
            context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        }
    }
}
