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
     * ## 다중 알람 정책
 * [ChargeSettings.alarmTargets] 에 들어 있는 모든 임계값에서 각각 한 번씩 울립니다.
 * 발화한 임계값은 [ChargeSettings.firedAlarmTargets] 에 기록되고, 전원을 뽑을 때까지
 * 다시 울리지 않습니다.
 *
 * 사용자가 알림에서 스누즈를 누르면 [ChargeSettings.snoozeUntilMillis] 시각까지
 * **어떤 임계값도 새로 발화하지 않습니다.** (동시 도달한 다른 임계값도 함께 미뤄집니다.
 * 서로 다른 알람을 동시에 울리면 소음으로 이어지므로 의도적으로 그렇게 했습니다.)
 *
 * 앱이 꺼져 있다가 깨어난 경우를 위해 목표보다 5% 지점까지 여유를 둡니다.
 */
object BatteryAlertManager {

    private const val TAG = "BatteryAlertManager"

    private const val ALARM_CHANNEL_ID = "charge_alarm_channel"
    internal const val ALARM_NOTIFICATION_ID = 2001
    private const val HEALTH_CHANNEL_ID = "battery_health_channel"
    private const val HEALTH_NOTIFICATION_ID = 2002
    private const val LIMIT_CHANNEL_ID = "charge_limit_channel"
    private const val LIMIT_NOTIFICATION_ID = 2003

    /** 앱이 꺼져 있다 켜졌을 때, 목표 지점을 이만큼 지나쳤어도 발화시킵니다. */
    private const val FIRE_TOLERANCE_PERCENT = 5

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 직전 평가 결과와 비교하기 위한 상태 (알림 반복 방지) */
    private var lastHealthNotifiedAt = 0L
    private var lastTempNotifiedAt = 0L
    private const val HEALTH_NOTIFY_COOLDOWN_MS = 6L * 60L * 60L * 1000L

    /** 잔량 톤을 울릴 마일스톤 */
    private val LEVEL_TONE_MILESTONES = intArrayOf(50, 80, 100)

    /** 이번 충전에서 이미 울린 마일스톤 (재충전하면 초기화) */
    private val firedLevelTones = mutableSetOf<Int>()

    /** 직전에 본 잔량 — 새로 넘은 마일스톤만 울리게 하는 기준 */
    private var lastLevelForTones = -1

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

                // 잔량 톤은 알람 설정과 무관하게 항상 추적합니다
                // (껃다가 켰을 때 지나간 구간을 몰아서 울리지 않도록).
                evaluateLevelTones(appContext, settings, info)

                if (info.isCharging) {
                    if (settings.alarmEnabled) evaluateAlarm(appContext, settings, info, repository)
                    if (settings.chargeLimitEnabled) {
                        evaluateChargeLimit(appContext, settings, info, repository)
                    }
                } else {
                    // 전원을 뽑으면 통계를 기록하고 발화 상태를 초기화합니다.
                    // 마지막으로 도달한 임계값이 있었는지도 함께 남깁니다.
                    ChargingSessionTracker.endSession(appContext, settings.firedAlarmTargets.isNotEmpty())
                    if (settings.firedAlarmTargets.isNotEmpty()) repository.resetAlarmFired()
                    if (settings.chargeLimitNotifiedThisSession) repository.resetChargeLimitNotified()
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

    /**
     * 아직 울리지 않은 임계값 중 하나를 발화시킵니다.
     *
     * 한 번에 하나만 울립니다 — 50·80·100 을 한꺼번에 통과했다면
     * 세 번 연속 울리는 대신 가장 높은 임계값 하나만 알립니다.
     */
    /**
     * 잔량이 50/80/100% 를 지날 때 짧은 톤을 재생합니다.
     *
     * 꽂은 시점의 잔량(예: 82%)을 그대로 "통과"로 보면 꽂자마자 울리므로,
     * **직전에 관측한 잔량보다 높아졌을 때**만 울립니다.
     */
    private fun evaluateLevelTones(
        context: Context,
        settings: ChargeSettings,
        info: BatteryInfo
    ) {
        if (!info.isCharging) {
            lastLevelForTones = -1
            firedLevelTones.clear()
            return
        }

        val previous = lastLevelForTones
        lastLevelForTones = info.level
        if (!settings.levelTonesEnabled) return
        // 이번 세션의 첫 관측 — 기준점만 잡고 넘어갑니다.
        if (previous < 0) return

        val crossed = LEVEL_TONE_MILESTONES.filter {
            it > previous && it <= info.level && it !in firedLevelTones
        }
        if (crossed.isEmpty()) return
        firedLevelTones.addAll(crossed)

        val minuteOfDay = java.util.Calendar.getInstance().let {
            it.get(java.util.Calendar.HOUR_OF_DAY) * 60 + it.get(java.util.Calendar.MINUTE)
        }
        if (settings.isQuietHourNow(minuteOfDay)) return

        val chime = com.example.sound.SoundManager.ensureLevelChime(context) ?: return
        com.example.sound.SoundManager.playSound(
            context = context,
            filePath = chime.absolutePath,
            volume = settings.volume * 0.8f,
            durationSeconds = 2
        )
    }

    private suspend fun evaluateAlarm(
        context: Context,
        settings: ChargeSettings,
        info: BatteryInfo,
        repository: com.example.data.SettingsRepository
    ) {
        val now = System.currentTimeMillis()
        if (now < settings.snoozeUntilMillis) return  // 스누즈 보류 중

        val pending = settings.alarmTargets
            .filter { it !in settings.firedAlarmTargets }
            .filter { info.level >= it - FIRE_TOLERANCE_PERCENT }
        if (pending.isEmpty()) return

        val target = pending.last()  // 여러 개가 동시에 조건을 만족하면 가장 높은 값
        repository.markAlarmFired(setOf(target))
        fireAlarm(context, settings, info, target)
    }

    private fun fireAlarm(
        context: Context,
        settings: ChargeSettings,
        info: BatteryInfo,
        target: Int
    ) {
        Log.d(TAG, "Firing charge alarm at ${info.level}% (target $target)")

        // 루틴에서 켠 동작만 실행합니다. 앱 전체 진동 설정도 함께 존중합니다.
        if (settings.completionRoutineVibration && settings.vibrationEnabled) {
            vibrate(context, longArrayOf(0, 400, 200, 400, 200, 800))
        }

        if (settings.completionRoutineSound) {
            playAlarmTone(context)
        }

        if (settings.completionRoutineTts && settings.voiceAnnounceEnabled) {
            com.example.voice.VoiceAnnouncer.announce(
                context,
                com.example.voice.VoiceTemplate.expand(
                    settings.voiceTargetTemplate,
                    info.level,
                    etaMillis = null
                )
            )
        }

        if (settings.completionRoutineNotification) {
            postAlarmNotification(context, info, target, settings.alarmSnoozeMinutes)
        }

        if (settings.completionRoutineAnimation && settings.animationEnabled) {
            launchCompletionAnimation(context, settings, info)
        }
    }

    /** 백그라운드 시작 제한에 막히면 무시합니다. 알림 루틴은 별도로 계속 보장됩니다. */
    private fun launchCompletionAnimation(context: Context, settings: ChargeSettings, info: BatteryInfo) {
        try {
            context.startActivity(
                Intent(context, com.example.ui.ChargingAnimationActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(com.example.ui.ChargingAnimationActivity.EXTRA_BATTERY_LEVEL, info.level)
                    putExtra(com.example.ui.ChargingAnimationActivity.EXTRA_PLUG_NAME, context.getString(info.plugType.displayNameRes))
                    putExtra(com.example.ui.ChargingAnimationActivity.EXTRA_ANIM_MODE, settings.animationMode.name)
                    putExtra(com.example.ui.ChargingAnimationActivity.EXTRA_VIDEO_PATH, settings.customVideoPath)
                    putExtra(com.example.ui.ChargingAnimationActivity.EXTRA_DURATION_SEC, 5)
                    putExtra(com.example.ui.ChargingAnimationActivity.EXTRA_LOOP, false)
                }
            )
        } catch (e: Exception) {
            Log.i(TAG, "Completion animation could not start", e)
        }
    }

    private fun postAlarmNotification(
        context: Context,
        info: BatteryInfo,
        target: Int,
        snoozeMinutes: Int
    ) {
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
                // 알림에서 바로 제어할 수 있게 합니다 — 앱을 열어야 하는 불편을 없앱니다.
                .addAction(
                    R.drawable.charge_sound_icon_1791457205401,
                    context.getString(R.string.action_snooze, snoozeMinutes),
                    AlarmActionReceiver.snoozePendingIntent(context, snoozeMinutes)
                )
                .addAction(
                    R.drawable.charge_sound_icon_1791457205401,
                    context.getString(R.string.action_dismiss),
                    AlarmActionReceiver.dismissPendingIntent(context)
                )
                .build()

            manager.notify(ALARM_NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post alarm notification", e)
        }
    }

    // ─────────────────────── 충전 중단 알림 ───────────────────────

    /**
     * 설정한 충전율에 도달하면 "이제 충분합니다"를 알립니다.
     *
     * 리튬 배터리를 100%까지 계속 충전하면 화학적 노화가 빨라집니다.
     * 80%에서 끊는 것만으로 수命이 크게 늘기 때문에, 앱이 대신 알려줍니다.
     */
    private suspend fun evaluateChargeLimit(
        context: Context,
        settings: ChargeSettings,
        info: BatteryInfo,
        repository: com.example.data.SettingsRepository
    ) {
        if (settings.chargeLimitNotifiedThisSession) return
        if (info.level < settings.chargeLimitPercent) return

        repository.setChargeLimitNotified()
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (manager.getNotificationChannel(LIMIT_CHANNEL_ID) == null) {
                    manager.createNotificationChannel(
                        NotificationChannel(
                            LIMIT_CHANNEL_ID,
                            context.getString(R.string.limit_channel_name),
                            NotificationManager.IMPORTANCE_DEFAULT
                        ).apply {
                            description = context.getString(R.string.limit_channel_desc)
                        }
                    )
                }
            }
            val openApp = PendingIntent.getActivity(
                context,
                1,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            manager.notify(
                LIMIT_NOTIFICATION_ID,
                NotificationCompat.Builder(context, LIMIT_CHANNEL_ID)
                    .setSmallIcon(R.drawable.charge_sound_icon_1791457205401)
                    .setContentTitle(context.getString(R.string.limit_title))
                    .setContentText(context.getString(R.string.limit_body, settings.chargeLimitPercent))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(openApp)
                    .build()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post charge limit notification", e)
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
