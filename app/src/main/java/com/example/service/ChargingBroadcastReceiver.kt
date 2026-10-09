package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.battery.BatteryInfoHelper
import com.example.data.AnimationMode
import com.example.data.SettingsRepository
import com.example.data.SoundMode
import com.example.sound.SoundManager
import com.example.ui.ChargingAnimationActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChargingBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ChargingReceiver"
        private const val FALLBACK_NOTIFICATION_ID = 1002
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received action: $action")

        val isPowerConnected = action == Intent.ACTION_POWER_CONNECTED
        val isPowerDisconnected = action == Intent.ACTION_POWER_DISCONNECTED

        if (!isPowerConnected && !isPowerDisconnected) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = SettingsRepository(context)
                val settings = repository.settingsFlow.first()

                if (!settings.isServiceEnabled) {
                    Log.d(TAG, "Service is disabled in settings. Skipping.")
                    return@launch
                }

                if (isPowerConnected) {
                    handlePowerConnected(context, settings)
                } else if (isPowerDisconnected) {
                    handlePowerDisconnected(context, settings)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling power action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handlePowerConnected(context: Context, settings: com.example.data.ChargeSettings) {
        // 1. Vibration
        if (settings.vibrationEnabled) {
            triggerVibration(context)
        }

        // 2. Play Audio with duration-synchronized fade out
        if (settings.soundEnabled) {
            val soundPath = when (settings.soundMode) {
                SoundMode.CUSTOM -> settings.customSoundPath
                SoundMode.PRESET -> SoundManager.getPresetFile(context, settings.presetSoundId).absolutePath
            }
            SoundManager.playSound(
                context = context,
                filePath = soundPath,
                volume = settings.volume,
                durationSeconds = settings.videoDurationSeconds
            )
        }

        // 3. Show Animation Screen
        if (settings.animationEnabled) {
            val batteryInfo = BatteryInfoHelper.getBatteryInfo(context)
            val canOverlay = canShowOverOtherApps(context)

            if (canOverlay) {
                launchAnimationActivity(context, settings, batteryInfo)
            } else {
                // 권한이 없으면 시스템이 백그라운드 액티비티 시작을 차단합니다(BAL).
                // Play 정책에 따라 거부 시 대체 경로를 제공해야 하므로 알림으로 처리합니다.
                Log.i(TAG, "SYSTEM_ALERT_WINDOW not granted. Falling back to notification.")
                notifyAnimationFallback(context, batteryInfo)
            }
        }
    }

    /**
     * "다른 앱 위에 표시" 권한 확인.
     * 이 권한이 없으면 Android 10+ 에서 백그라운드 액티비티 시작이 차단됩니다.
     */
    private fun canShowOverOtherApps(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }

    private fun launchAnimationActivity(
        context: Context,
        settings: com.example.data.ChargeSettings,
        batteryInfo: com.example.battery.BatteryInfo
    ) {
        val animIntent = Intent(context, ChargingAnimationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(ChargingAnimationActivity.EXTRA_BATTERY_LEVEL, batteryInfo.level)
            putExtra(ChargingAnimationActivity.EXTRA_PLUG_NAME, batteryInfo.plugType.displayName)
            putExtra(ChargingAnimationActivity.EXTRA_ANIM_MODE, settings.animationMode.name)
            putExtra(ChargingAnimationActivity.EXTRA_VIDEO_PATH, settings.customVideoPath)
            putExtra(ChargingAnimationActivity.EXTRA_DURATION_SEC, settings.videoDurationSeconds)
            putExtra(ChargingAnimationActivity.EXTRA_LOOP, settings.videoLoop)
        }
        try {
            context.startActivity(animIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch charging animation activity", e)
            notifyAnimationFallback(context, batteryInfo)
        }
    }

    /**
     * 오버레이 권한이 없을 때의 대체 동작.
     * 사용자가 권한을 거부해도 앱의 핵심 기능(알림 + 사운드)이 계속 동작해야 합니다.
     */
    private fun notifyAnimationFallback(context: Context, batteryInfo: com.example.battery.BatteryInfo) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = "charge_animation_fallback"
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (manager.getNotificationChannel(channelId) == null) {
                    val channel = NotificationChannel(
                        channelId,
                        "충전 시작 알림",
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = "화면 오버레이 권한이 없을 때 충전 시작을 알려줍니다."
                        setShowBadge(false)
                    }
                    manager.createNotificationChannel(channel)
                }
                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(com.example.R.drawable.charge_sound_icon_1791457205401)
                    .setContentTitle("충전 시작 ⚡ ${batteryInfo.level}%")
                    .setContentText("${batteryInfo.plugType.displayName}에 연결되었습니다")
                    .setPriority(NotificationCompat.PRIORITY_LOW)
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
                manager.notify(FALLBACK_NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post fallback notification", e)
        }
    }

    private fun handlePowerDisconnected(context: Context, settings: com.example.data.ChargeSettings) {
        SoundManager.fadeOutAndStop(400L)
        if (settings.playOnDisconnect) {
            val disconnectTone = SoundManager.getPresetFile(context, "gentle_ding").absolutePath
            SoundManager.playSound(context, disconnectTone, settings.volume * 0.7f, 2)
        }
    }

    private fun triggerVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                val pattern = VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 150), intArrayOf(0, 180, 0, 255), -1)
                vibrator?.vibrate(pattern)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 150), -1)
                    vibrator?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(250)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed", e)
        }
    }
}
