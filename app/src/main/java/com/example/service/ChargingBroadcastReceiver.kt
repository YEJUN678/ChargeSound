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
import com.example.R
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
        // 조용한 시간대에는 사운드만 끕니다.
        // 진동 · 애니메이션 · 알림은 그대로 동작합니다 (기능 손실 없음).
        val quietNow = settings.isQuietHourNow(currentMinuteOfDay())
        if (quietNow) {
            Log.i(TAG, "Quiet hours active - suppressing sound")
        }

        // 1. Vibration
        if (settings.vibrationEnabled) {
            triggerVibration(context)
        }

        // 2. Play Audio with duration-synchronized fade out
        if (settings.soundEnabled && !quietNow) {
            // 충전 방식(AC/USB/무선)에 따라 다른 프리셋을 쓸 수 있습니다.
            val soundPath = when {
                settings.soundMode == SoundMode.CUSTOM -> settings.customSoundPath

                settings.plugSpecificSounds -> {
                    val presetId = presetIdForPlug(settings, batteryInfoOf(context))
                    SoundManager.getPresetFile(context, presetId).absolutePath
                }

                else -> SoundManager.getPresetFile(context, settings.presetSoundId).absolutePath
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

    /** 현재 시각을 하루 기준 분(0~1439)으로 반환 */
    private fun currentMinuteOfDay(): Int {
        val cal = java.util.Calendar.getInstance()
        return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
    }

    private fun batteryInfoOf(context: Context) = BatteryInfoHelper.getBatteryInfo(context)

    /** 충전 방식에 대응하는 프리셋 ID */
    private fun presetIdForPlug(
        settings: com.example.data.ChargeSettings,
        info: com.example.battery.BatteryInfo
    ): String = when (info.plugType) {
        com.example.battery.PlugType.AC -> settings.acPresetId
        com.example.battery.PlugType.USB -> settings.usbPresetId
        com.example.battery.PlugType.WIRELESS -> settings.wirelessPresetId
        com.example.battery.PlugType.NONE -> settings.presetSoundId
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
            putExtra(
                ChargingAnimationActivity.EXTRA_PLUG_NAME,
                context.getString(batteryInfo.plugType.displayNameRes)
            )
            putExtra(ChargingAnimationActivity.EXTRA_ANIM_MODE, settings.animationMode.name)
            putExtra(ChargingAnimationActivity.EXTRA_VIDEO_PATH, settings.customVideoPath)
            putExtra(ChargingAnimationActivity.EXTRA_DURATION_SEC, settings.videoDurationSeconds)
            putExtra(ChargingAnimationActivity.EXTRA_LOOP, settings.videoLoop)
            // 구간 선택: 사용자가 고른 부분만 반복 재생합니다.
            putExtra(ChargingAnimationActivity.EXTRA_VIDEO_START_MS, settings.customVideoStartMs)
            putExtra(ChargingAnimationActivity.EXTRA_VIDEO_END_MS, settings.customVideoEndMs)
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
                        context.getString(R.string.notif_charge_channel),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = context.getString(R.string.notif_charge_desc)
                        setShowBadge(false)
                    }
                    manager.createNotificationChannel(channel)
                }
                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(com.example.R.drawable.charge_sound_icon_1791457205401)
                    .setContentTitle(
                        context.getString(R.string.notif_charge_title, batteryInfo.level)
                    )
                    .setContentText(
                        context.getString(
                            R.string.notif_charge_text,
                            context.getString(batteryInfo.plugType.displayNameRes)
                        )
                    )
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
