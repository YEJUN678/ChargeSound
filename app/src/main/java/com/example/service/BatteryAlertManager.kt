package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.battery.BatteryInfo
import com.example.battery.BatteryInfoHelper
import com.example.data.ChargeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 충전 알람 / 배터리 경고 처리.
 *
 * 충전 감지 모니터링 서비스가 배터리 변화마다 호출합니다.
 *
 * ## 정책
 * 알람은 **사용자가 설정한 충전율에 정확히 도달했을 때만** 울립니다.
 * 한 번 울린 뒤 전원을 뽑을 때까지 다시 울리지 않으므로 과도한 알림이 발생하지 않습니다.
 */
object BatteryAlertManager {

    private const val TAG = "BatteryAlertManager"

    private const val ALARM_CHANNEL_ID = "charge_alarm_channel"
    private const val ALARM_NOTIFICATION_ID = 2001
    private const val HEALTH_CHANNEL_ID = "battery_health_channel"
    private const val HEALTH_NOTIFICATION_ID = 2002

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 직전 평가 결과와 비교하기 위한 상태 (알림 반복 방지) */
    private var lastHealthNotifiedAt = 0L
    private var lastTempNotifiedAt = 0L
    private const val HEALTH_NOTIFY_COOLDOWN_MS = 6L * 60L * 60L * 1000L

    /**
     * 배터리 상태가 바뀔 때마다 호출됩니다.
     * 충전 중일 때만 알람/경고를 평가합니다.
     */
    fun evaluate(context: Context) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                val repository = com.example.data.SettingsRepository(appContext)
                val settings = repository.settingsFlow.first()
                if (!settings.isServiceEnabled) return@launch

                val info = BatteryInfoHelper.getBatteryInfo(appContext)

                // 충전기가 연결된 상태에서만 알람이 의미를 가집니다.
                if (info.isCharging) {
                    if (settings.alarmEnabled) evaluateAlarm(appContext, settings, info, repository)
                } else {
                    // 전원을 뽑으면 다음 충전 세션을 위해 발화 기록을 초기화합니다.
                    if (settings.alarmFiredThisSession) repository.setAlarmFiredThisSession(false)
                }

                if (settings.tempAlertEnabled) evaluateTemperature(appContext, settings, info)
                if (settings.healthAlertEnabled) evaluateHealth(appContext, settings, info)
                if (settings.lowBatteryAlertEnabled) evaluateLowBattery(appContext, settings, info)
                if (settings.rapidDischargeAlertEnabled) evaluateRapidDischarge(appContext, settings, info)

                // 충전 세션 추적 (경과 시간 · ETA 계산용)
                ChargingSessionTracker.record(appContext, info, settings.alarmTargetPercent)
            } catch (e: Exception) {
                Log.e(TAG, "evaluate failed", e)
            }
        }
    }

    // ─────────────────────── 충전 알람 ───────────────────────

    private suspend fun evaluateAlarm(
        context: Context,
        settings: ChargeSettings,
        info: BatteryInfo,
        repository: com.example.data.SettingsRepository
    ) {
        val target = settings.alarmTargetPercent
        val alreadyFired = settings.alarmFiredThisSession

        // 목표에 도달했고 아직 울리지 않았다면 알람
        if (!alreadyFired && info.level >= target) {
            repository.setAlarmFiredThisSession(true)
            fireAlarm(context, settings, info, target)
            return
        }

        // 목표를 지나쳤는데 아직 울리지 않았다면 (앱이 꺼져 있다가 깨어난 경우) 발화
        if (!alreadyFired && info.level >= target - 5) {
            repository.setAlarmFiredThisSession(true)
            fireAlarm(context, settings, info, target)
        }
    }

    private fun fireAlarm(
        context: Context,
        settings: ChargeSettings,
        info: BatteryInfo,
        target: Int
    ) {
        Log.d(TAG, "Firing charge alarm at ${info.level}% (target $target)")

        // 사용자가 진동을 켜둔 상태를 따릅니다.
        if (settings.vibrationEnabled) vibrate(context, longArrayOf(0, 400, 200, 400, 200, 800))

        if (settings.alarmWithSound) {
            playAlarmTone(context)
        }

        postAlarmNotification(context, info, target)
    }

    private fun postAlarmNotification(context: Context, info: BatteryInfo, target: Int) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (manager.getNotificationChannel(ALARM_CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        ALARM_CHANNEL_ID,
                        context.getString(R.string.alarm_channel_name),
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = context.getString(R.string.alarm_channel_desc)
                        enableVibration(true)
                    }
                    manager.createNotificationChannel(channel)
                }
            }

            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, ALARM_CHANNEL_ID)
                .setSmallIcon(R.drawable.charge_sound_icon_1791457205401)
                .setContentTitle(context.getString(R.string.alarm_notification_title, info.level))
                .setContentText(context.getString(R.string.alarm_notification_text, target))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .build()

            manager.notify(ALARM_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post alarm notification", e)
        }
    }

    /** 기본 알람 소리를 사용합니다 (사용자가 시스템에서 고른 알람음). */
    private fun playAlarmTone(context: Context) {
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: return
            val player = android.media.MediaPlayer()
            player.setDataSource(context, uri)
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.isLooping = true
            player.setOnPreparedListener { it.start() }
            // 알람이 끝날 때까지 유지했다가 해제합니다 (누수 방지).
            player.setOnCompletionListener { it.release() }
            player.setOnErrorListener { _, _, _ -> player.release(); true }
            player.prepareAsync()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play alarm tone", e)
        }
    }

    // ─────────────────────── 온도 / 건강도 경고 ───────────────────────

    private fun evaluateTemperature(context: Context, settings: ChargeSettings, info: BatteryInfo) {
        if (info.temperatureC < settings.tempAlertThresholdC) {
            lastTempNotifiedAt = 0L
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastTempNotifiedAt < HEALTH_NOTIFY_COOLDOWN_MS) return
        lastTempNotifiedAt = now

        postHealthNotification(
            context,
            title = context.getString(R.string.temp_alert_title),
            body = context.getString(
                R.string.temp_alert_body,
                settings.tempAlertThresholdC,
                info.temperatureC
            )
        )
    }

    private fun evaluateHealth(context: Context, settings: ChargeSettings, info: BatteryInfo) {
        val healthRes = info.healthRes
        val bad = healthRes == R.string.battery_health_overheat ||
            healthRes == R.string.battery_health_dead ||
            healthRes == R.string.battery_health_overvoltage ||
            healthRes == R.string.battery_health_fail ||
            healthRes == R.string.battery_health_cold
        if (!bad) {
            lastHealthNotifiedAt = 0L
            return
        }
        val now = System.currentTimeMillis()
        if (now - lastHealthNotifiedAt < HEALTH_NOTIFY_COOLDOWN_MS) return
        lastHealthNotifiedAt = now

        postHealthNotification(
            context,
            title = context.getString(R.string.health_alert_title),
            body = context.getString(healthRes)
        )
    }

    // ─────────────────────── 저전력 · 급방전 ───────────────────────

private fun evaluateLowBattery(context: Context, settings: ChargeSettings, info: BatteryInfo) {
        // 충전 중이거나 이미 경고를 보낸 구간이면 조용합니다.
        if (info.isCharging) {
            lowBatteryNotified = false
            return
        }
        if (info.level > settings.lowBatteryThreshold) {
            lowBatteryNotified = false
            return
        }
        if (lowBatteryNotified) return
        lowBatteryNotified = true

        postHealthNotification(
            context,
            title = context.getString(R.string.low_battery_title),
            body = context.getString(R.string.low_battery_body, settings.lowBatteryThreshold, info.level)
        )
    }

    /**
     * 급방전 감지.
     *
     * 비충전 중 [ChargeSettings.rapidDischargeWindowMinutes] 분 이내에
     * [ChargeSettings.rapidDischargeDropPercent] % 이상 떨어지면
     * 고부하(게임/내비/화면 밝기) 등으로 배터리가 빠르게 소모되는 것으로 판단합니다.
     */
    private fun evaluateRapidDischarge(context: Context, settings: ChargeSettings, info: BatteryInfo) {
        if (info.isCharging) {
            rapidWindowStartAt = 0L
            rapidWindowStartLevel = info.level
            rapidDischargeNotified = false
            return
        }

        val now = System.currentTimeMillis()
        val windowMillis = settings.rapidDischargeWindowMinutes * 60_000L

        // 기록이 없거나 너무 오래되면 새로 시작합니다.
        if (rapidWindowStartAt == 0L || now - rapidWindowStartAt > windowMillis) {
            rapidWindowStartAt = now
            rapidWindowStartLevel = info.level
            rapidDischargeNotified = false
            return
        }

        val drop = rapidWindowStartLevel - info.level
        if (drop < settings.rapidDischargeDropPercent) return
        if (rapidDischargeNotified) return

        rapidDischargeNotified = true
        postHealthNotification(
            context,
            title = context.getString(R.string.rapid_discharge_title),
            body = context.getString(
                R.string.rapid_discharge_body,
                settings.rapidDischargeWindowMinutes,
                drop
            )
        )
    }

    @Volatile
    private var lowBatteryNotified = false

    @Volatile
    private var rapidDischargeNotified = false
    @Volatile
    private var rapidWindowStartAt = 0L
    @Volatile
    private var rapidWindowStartLevel = 0

    private fun postHealthNotification(context: Context, title: String, body: String) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (manager.getNotificationChannel(HEALTH_CHANNEL_ID) == null) {
                    val channel = NotificationChannel(
                        HEALTH_CHANNEL_ID,
                        context.getString(R.string.health_channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = context.getString(R.string.health_channel_desc)
                    }
                    manager.createNotificationChannel(channel)
                }
            }

            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, HEALTH_CHANNEL_ID)
                .setSmallIcon(R.drawable.charge_sound_icon_1791457205401)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(openApp)
                .build()

            manager.notify(HEALTH_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post health notification", e)
        }
    }

    // ─────────────────────── 진동 ───────────────────────

    private fun vibrate(context: Context, pattern: LongArray) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                mgr?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Vibration failed", e)
        }
    }

    /** 앱 시작 시 알림 쿨다운 상태를 초기화합니다. */
    fun resetCooldowns() {
        lastHealthNotifiedAt = 0L
        lastTempNotifiedAt = 0L
        lowBatteryNotified = false
        rapidDischargeNotified = false
        rapidWindowStartAt = 0L
    }
}