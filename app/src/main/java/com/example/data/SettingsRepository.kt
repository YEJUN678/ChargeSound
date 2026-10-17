package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "charge_settings")

/** 충전 세션 상태 (경과 시간 · ETA 계산용) */
data class ChargingSession(
    val startedAt: Long = 0L,
    val startLevel: Int = 0,
    val lastLevel: Int = 0,
    val lastChangedAt: Long = 0L
)

class SettingsRepository(private val context: Context) {
    companion object {
        private const val TAG = "SettingsRepository"

        val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_VOLUME = floatPreferencesKey("volume")
        val KEY_SOUND_MODE = stringPreferencesKey("sound_mode")
        val KEY_PRESET_SOUND_ID = stringPreferencesKey("preset_sound_id")
        val KEY_CUSTOM_SOUND_NAME = stringPreferencesKey("custom_sound_name")
        val KEY_CUSTOM_SOUND_PATH = stringPreferencesKey("custom_sound_path")
        val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")

        val KEY_ANIMATION_ENABLED = booleanPreferencesKey("animation_enabled")
        val KEY_ANIMATION_MODE = stringPreferencesKey("animation_mode")
        val KEY_CUSTOM_VIDEO_NAME = stringPreferencesKey("custom_video_name")
        val KEY_CUSTOM_VIDEO_PATH = stringPreferencesKey("custom_video_path")
        val KEY_VIDEO_DURATION = intPreferencesKey("video_duration")
        val KEY_VIDEO_LOOP = booleanPreferencesKey("video_loop")
        val KEY_PLAY_ON_DISCONNECT = booleanPreferencesKey("play_on_disconnect")

        val KEY_UNLOCKED_PREMIUM = stringSetPreferencesKey("unlocked_premium")
        val KEY_REWARD_PROGRESS = stringPreferencesKey("reward_progress")
        val KEY_AD_FREE_UNTIL = longPreferencesKey("ad_free_until")
        val KEY_OVERLAY_GRANTED = booleanPreferencesKey("overlay_granted")
        val KEY_OVERLAY_PROMPT_SEEN = booleanPreferencesKey("overlay_prompt_seen")
        val KEY_PREMIUM_UNLOCKED = booleanPreferencesKey("premium_unlocked")
        val KEY_FREE_UNLOCK_CREDITS = intPreferencesKey("free_unlock_credits")

    // ── 충전 알람 ──
    val KEY_ALARM_ENABLED = booleanPreferencesKey("alarm_enabled")
    val KEY_ALARM_TARGET = intPreferencesKey("alarm_target_percent")
    val KEY_ALARM_FIRED = booleanPreferencesKey("alarm_fired_session")
    val KEY_ALARM_WITH_SOUND = booleanPreferencesKey("alarm_with_sound")

    // ── 배터리 경고 ──
    val KEY_TEMP_ALERT_ENABLED = booleanPreferencesKey("temp_alert_enabled")
    val KEY_TEMP_ALERT_THRESHOLD = floatPreferencesKey("temp_alert_threshold")
    val KEY_HEALTH_ALERT_ENABLED = booleanPreferencesKey("health_alert_enabled")

    // ── 잠금 화면 ──
    val KEY_SHOW_ON_LOCK_SCREEN = booleanPreferencesKey("show_on_lock_screen")

    // ── 충전 방식별 사운드 ──
    val KEY_PLUG_SPECIFIC = booleanPreferencesKey("plug_specific_sounds")
    val KEY_AC_PRESET = stringPreferencesKey("ac_preset_id")
    val KEY_USB_PRESET = stringPreferencesKey("usb_preset_id")
    val KEY_WIRELESS_PRESET = stringPreferencesKey("wireless_preset_id")

    // ── 저전력 · 급방전 ──
    val KEY_LOW_BATTERY_ALERT = booleanPreferencesKey("low_battery_alert")
    val KEY_LOW_BATTERY_THRESHOLD = intPreferencesKey("low_battery_threshold")
    val KEY_RAPID_DISCHARGE_ALERT = booleanPreferencesKey("rapid_discharge_alert")
    val KEY_RAPID_WINDOW_MIN = intPreferencesKey("rapid_window_min")
    val KEY_RAPID_DROP_PERCENT = intPreferencesKey("rapid_drop_percent")

    // ── 조용한 시간 ──
    val KEY_QUIET_ENABLED = booleanPreferencesKey("quiet_enabled")
    val KEY_QUIET_START = intPreferencesKey("quiet_start_minute")
    val KEY_QUIET_END = intPreferencesKey("quiet_end_minute")
    val KEY_ACCENT = stringPreferencesKey("accent_key")

    // ── 충전 세션 추적 (ETA) ──
    val KEY_SESSION_STARTED_AT = longPreferencesKey("session_started_at")
    val KEY_SESSION_START_LEVEL = intPreferencesKey("session_start_level")
    val KEY_SESSION_LAST_LEVEL = intPreferencesKey("session_last_level")
    val KEY_SESSION_LAST_CHANGED_AT = longPreferencesKey("session_last_changed_at")

    // ── 다중 알람 ──
    val KEY_ALARM_TARGETS = stringPreferencesKey("alarm_targets_csv")
    val KEY_FIRED_TARGETS = stringPreferencesKey("fired_alarm_targets_csv")
    val KEY_SNOOZE_UNTIL = longPreferencesKey("snooze_until")
    val KEY_ALARM_SNOOZE_MIN = intPreferencesKey("alarm_snooze_minutes")

    // ── 충전 중단 알림 ──
    val KEY_CHARGE_LIMIT_ENABLED = booleanPreferencesKey("charge_limit_enabled")
    val KEY_CHARGE_LIMIT_PERCENT = intPreferencesKey("charge_limit_percent")
    val KEY_CHARGE_LIMIT_NOTIFIED = booleanPreferencesKey("charge_limit_notified")

    // ── 접근성 ──
    val KEY_FONT_SCALE = floatPreferencesKey("font_scale")

    // ── 충전 기록 (통계) ──
    val KEY_CHARGE_HISTORY = stringPreferencesKey("charge_history_json")
    }

    /**
     * IntSet 직렬화 — "50,80,100" 형태.
     *
     * 빈 값은 빈 문자열이 됩니다. DataStore 에 빈 문자열을 쓰는 것은
     * 키가 "없음"인 상태와 구분하기 위해 필요합니다(마이그레이션 판단용).
     */
    private fun encodeIntSet(values: Set<Int>): String =
        values.sorted().joinToString(",")

    private fun decodeIntSet(raw: String?): Set<Int> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    /** List<Int> 직렬화. 범위를 벗어나는 값과 빈 리스트는 걸러냅니다. */
    private fun encodeIntList(values: List<Int>): String =
        values.distinct().sorted().joinToString(",")

    private fun decodeIntList(raw: String?): List<Int> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 10..100 }
            .distinct()
            .sorted()
    }

    /** "id:count,id:count" 형태의 직렬화 */
    private fun encodeProgress(map: Map<String, Int>): String =
        map.entries.joinToString(",") { "${it.key}:${it.value}" }

    private fun decodeProgress(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val count = parts[1].toIntOrNull() ?: return@mapNotNull null
            parts[0] to count
        }.toMap()
    }

    val settingsFlow: Flow<ChargeSettings> = context.dataStore.data.map { prefs ->
        val soundModeStr = prefs[KEY_SOUND_MODE] ?: SoundMode.PRESET.name
        val soundMode = try {
            SoundMode.valueOf(soundModeStr)
        } catch (e: Exception) {
            SoundMode.PRESET
        }

        val animModeStr = prefs[KEY_ANIMATION_MODE] ?: AnimationMode.BUILTIN_LIGHTNING.name
        val animMode = try {
            AnimationMode.valueOf(animModeStr)
        } catch (e: Exception) {
            AnimationMode.BUILTIN_LIGHTNING
        }

        // 단일 임계값 → 다중 임계값 마이그레이션.
        // KEY_ALARM_TARGETS 키 자체가 없을 때만 이전 단일 값을 목록으로 승격합니다.
        // 한 번 마이그레이션되면 목록이 단일값의 유일한 원천이 됩니다.
        val legacyTarget = (prefs[KEY_ALARM_TARGET] ?: 80).coerceIn(10, 100)
        val alarmTargets = if (prefs.contains(KEY_ALARM_TARGETS)) {
            decodeIntList(prefs[KEY_ALARM_TARGETS]).ifEmpty { listOf(legacyTarget) }
        } else {
            listOf(legacyTarget)
        }
        // 이전 KEY_ALARM_FIRED(불리언)가 참이면 현재 목록의 모든 임계값을 발화된 것으로 봅니다.
        // 한 개뿐인 알람에서 마이그레이션하므로 정확한 동작입니다.
        val legacyFired = prefs[KEY_ALARM_FIRED] ?: false
        val firedTargets = decodeIntSet(prefs[KEY_FIRED_TARGETS])
            .let { if (prefs.contains(KEY_FIRED_TARGETS)) it else if (legacyFired) alarmTargets.toSet() else emptySet() }

        ChargeSettings(
            isServiceEnabled = prefs[KEY_SERVICE_ENABLED] ?: true,
            soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true,
            volume = prefs[KEY_VOLUME] ?: 0.85f,
            soundMode = soundMode,
            presetSoundId = prefs[KEY_PRESET_SOUND_ID] ?: "cyber_chime",
            customSoundName = prefs[KEY_CUSTOM_SOUND_NAME],
            customSoundPath = prefs[KEY_CUSTOM_SOUND_PATH],
            vibrationEnabled = prefs[KEY_VIBRATION_ENABLED] ?: true,
            animationEnabled = prefs[KEY_ANIMATION_ENABLED] ?: true,
            animationMode = animMode,
            customVideoName = prefs[KEY_CUSTOM_VIDEO_NAME],
            customVideoPath = prefs[KEY_CUSTOM_VIDEO_PATH],
            videoDurationSeconds = prefs[KEY_VIDEO_DURATION] ?: 5,
            videoLoop = prefs[KEY_VIDEO_LOOP] ?: true,
            playOnDisconnect = prefs[KEY_PLAY_ON_DISCONNECT] ?: false,
            unlockedPremiumIds = prefs[KEY_UNLOCKED_PREMIUM] ?: emptySet(),
            rewardProgress = decodeProgress(prefs[KEY_REWARD_PROGRESS]),
            adFreeUntil = prefs[KEY_AD_FREE_UNTIL] ?: 0L,
            overlayPermissionGranted = prefs[KEY_OVERLAY_GRANTED] ?: false,
            overlayPromptSeen = prefs[KEY_OVERLAY_PROMPT_SEEN] ?: false,
            premiumUnlocked = prefs[KEY_PREMIUM_UNLOCKED] ?: false,
            freeUnlockCredits = prefs[KEY_FREE_UNLOCK_CREDITS] ?: 0,
            alarmEnabled = prefs[KEY_ALARM_ENABLED] ?: false,
            alarmTargetPercent = alarmTargets.last(),
            alarmTargets = alarmTargets,
            firedAlarmTargets = firedTargets,
            snoozeUntilMillis = prefs[KEY_SNOOZE_UNTIL] ?: 0L,
            alarmSnoozeMinutes = (prefs[KEY_ALARM_SNOOZE_MIN] ?: 10).coerceIn(1, 60),
            alarmWithSound = prefs[KEY_ALARM_WITH_SOUND] ?: true,
            chargeLimitEnabled = prefs[KEY_CHARGE_LIMIT_ENABLED] ?: false,
            chargeLimitPercent = (prefs[KEY_CHARGE_LIMIT_PERCENT] ?: 80).coerceIn(50, 95),
            chargeLimitNotifiedThisSession = prefs[KEY_CHARGE_LIMIT_NOTIFIED] ?: false,
            tempAlertEnabled = prefs[KEY_TEMP_ALERT_ENABLED] ?: false,
            tempAlertThresholdC = prefs[KEY_TEMP_ALERT_THRESHOLD] ?: 45f,
            healthAlertEnabled = prefs[KEY_HEALTH_ALERT_ENABLED] ?: true,
            showOnLockScreen = prefs[KEY_SHOW_ON_LOCK_SCREEN] ?: true,
            plugSpecificSounds = prefs[KEY_PLUG_SPECIFIC] ?: false,
            acPresetId = prefs[KEY_AC_PRESET] ?: "cyber_chime",
            usbPresetId = prefs[KEY_USB_PRESET] ?: "lightning_spark",
            wirelessPresetId = prefs[KEY_WIRELESS_PRESET] ?: "gentle_ding",
            lowBatteryAlertEnabled = prefs[KEY_LOW_BATTERY_ALERT] ?: true,
            lowBatteryThreshold = (prefs[KEY_LOW_BATTERY_THRESHOLD] ?: 20).coerceIn(5, 50),
            rapidDischargeAlertEnabled = prefs[KEY_RAPID_DISCHARGE_ALERT] ?: true,
            rapidDischargeWindowMinutes = (prefs[KEY_RAPID_WINDOW_MIN] ?: 30).coerceIn(5, 180),
            rapidDischargeDropPercent = (prefs[KEY_RAPID_DROP_PERCENT] ?: 10).coerceIn(3, 40),
            quietHoursEnabled = prefs[KEY_QUIET_ENABLED] ?: false,
            quietStartMinute = (prefs[KEY_QUIET_START] ?: 1320).coerceIn(0, 1440),
            quietEndMinute = (prefs[KEY_QUIET_END] ?: 420).coerceIn(0, 1440),
            accentKey = prefs[KEY_ACCENT] ?: "cyan",
            fontScale = (prefs[KEY_FONT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.5f)
        )
    }
suspend fun readSession(): ChargingSession = context.dataStore.data.first().let { prefs ->
        ChargingSession(
            startedAt = prefs[KEY_SESSION_STARTED_AT] ?: 0L,
            startLevel = prefs[KEY_SESSION_START_LEVEL] ?: 0,
            lastLevel = prefs[KEY_SESSION_LAST_LEVEL] ?: 0,
            lastChangedAt = prefs[KEY_SESSION_LAST_CHANGED_AT] ?: 0L
        )
    }

    suspend fun writeSession(session: ChargingSession) {
        context.dataStore.edit {
            it[KEY_SESSION_STARTED_AT] = session.startedAt
            it[KEY_SESSION_START_LEVEL] = session.startLevel
            it[KEY_SESSION_LAST_LEVEL] = session.lastLevel
            it[KEY_SESSION_LAST_CHANGED_AT] = session.lastChangedAt
        }
    }

    // ── 충전 방식별 사운드 ──

    suspend fun setPlugSpecificSounds(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PLUG_SPECIFIC] = enabled }
    }

    suspend fun setPlugPreset(plugType: com.example.battery.PlugType, presetId: String) {
        context.dataStore.edit {
            when (plugType) {
                com.example.battery.PlugType.AC -> it[KEY_AC_PRESET] = presetId
                com.example.battery.PlugType.USB -> it[KEY_USB_PRESET] = presetId
                com.example.battery.PlugType.WIRELESS -> it[KEY_WIRELESS_PRESET] = presetId
                com.example.battery.PlugType.NONE -> Unit
            }
        }
    }

    // ── 저전력 · 급방전 ──

    suspend fun setLowBatteryAlertEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_LOW_BATTERY_ALERT] = enabled }
    }

    suspend fun setLowBatteryThreshold(percent: Int) {
        context.dataStore.edit { it[KEY_LOW_BATTERY_THRESHOLD] = percent }
    }

    suspend fun setRapidDischargeAlertEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_RAPID_DISCHARGE_ALERT] = enabled }
    }

    suspend fun setRapidDischargeWindow(minutes: Int) {
        context.dataStore.edit { it[KEY_RAPID_WINDOW_MIN] = minutes }
    }

    suspend fun setRapidDischargeDrop(percent: Int) {
        context.dataStore.edit { it[KEY_RAPID_DROP_PERCENT] = percent }
    }

    // ── 조용한 시간 ──

    suspend fun setAccentKey(key: String) {
        context.dataStore.edit { it[KEY_ACCENT] = key }
    }

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_QUIET_ENABLED] = enabled }
    }

    suspend fun setQuietHours(startMinute: Int, endMinute: Int) {
        context.dataStore.edit {
            it[KEY_QUIET_START] = startMinute.coerceIn(0, 1440)
            it[KEY_QUIET_END] = endMinute.coerceIn(0, 1440)
        }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SERVICE_ENABLED] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    }

    suspend fun setVolume(volume: Float) {
        context.dataStore.edit { it[KEY_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setSoundMode(mode: SoundMode) {
        context.dataStore.edit { it[KEY_SOUND_MODE] = mode.name }
    }

    suspend fun setPresetSoundId(id: String) {
        context.dataStore.edit {
            it[KEY_PRESET_SOUND_ID] = id
            it[KEY_SOUND_MODE] = SoundMode.PRESET.name
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATION_ENABLED] = enabled }
    }

    suspend fun setAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ANIMATION_ENABLED] = enabled }
    }

    suspend fun setAnimationMode(mode: AnimationMode) {
        context.dataStore.edit { it[KEY_ANIMATION_MODE] = mode.name }
    }

    suspend fun setVideoDuration(seconds: Int) {
        context.dataStore.edit { it[KEY_VIDEO_DURATION] = seconds }
    }

    suspend fun setVideoLoop(loop: Boolean) {
        context.dataStore.edit { it[KEY_VIDEO_LOOP] = loop }
    }

    suspend fun setPlayOnDisconnect(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PLAY_ON_DISCONNECT] = enabled }
    }

    suspend fun setUnlockedPremiumIds(ids: Set<String>) {
        context.dataStore.edit { it[KEY_UNLOCKED_PREMIUM] = ids }
    }

    suspend fun setRewardProgress(progress: Map<String, Int>) {
        context.dataStore.edit {
            if (progress.isEmpty()) it.remove(KEY_REWARD_PROGRESS)
            else it[KEY_REWARD_PROGRESS] = encodeProgress(progress)
        }
    }

    suspend fun setAdFreeUntil(epochMillis: Long) {
        context.dataStore.edit { it[KEY_AD_FREE_UNTIL] = epochMillis }
    }

    suspend fun setOverlayPermissionGranted(granted: Boolean) {
        context.dataStore.edit { it[KEY_OVERLAY_GRANTED] = granted }
    }

    suspend fun setOverlayPromptSeen(seen: Boolean) {
        context.dataStore.edit { it[KEY_OVERLAY_PROMPT_SEEN] = seen }
    }

    suspend fun setPremiumUnlocked(unlocked: Boolean) {
        context.dataStore.edit { it[KEY_PREMIUM_UNLOCKED] = unlocked }
    }

    // ── 충전 알람 ──

    suspend fun setAlarmEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[KEY_ALARM_ENABLED] = enabled
            // 새로 켜면 발화 기록을 초기화합니다.
            if (enabled) it[KEY_ALARM_FIRED] = false
        }
    }

    /**
     * 알람 임계값 목록을 교체합니다.
     *
     * 목록이 바뀌면 이번 세션의 발화 기록도 초기화합니다 — 새 임계값이
     * 이미 울렸다고 잘못 판단하지 않도록 하기 위함입니다.
     */
    suspend fun setAlarmTargets(targets: List<Int>) {
        val clean = targets.filter { it in 10..100 }.distinct().sorted()
        if (clean.isEmpty()) return
        context.dataStore.edit {
            it[KEY_ALARM_TARGETS] = encodeIntList(clean)
            // 구버전에서 읽는 단일 임계값도 갱신해 두어 상호 호환성을 유지합니다.
            it[KEY_ALARM_TARGET] = clean.last()
            it[KEY_FIRED_TARGETS] = ""
            it[KEY_ALARM_FIRED] = false
        }
    }

    /** 이번 충전 세션에서 발화된 임계값을 기록합니다. */
    suspend fun markAlarmFired(targets: Collection<Int>) {
        if (targets.isEmpty()) return
        context.dataStore.edit { prefs ->
            val merged = decodeIntSet(prefs[KEY_FIRED_TARGETS]) + targets
            prefs[KEY_FIRED_TARGETS] = encodeIntSet(merged)
            prefs[KEY_ALARM_FIRED] = true
        }
    }

    /** 전원을 뽑었을 때 발화 기록을 비웁니다. */
    suspend fun resetAlarmFired() {
        context.dataStore.edit {
            it[KEY_FIRED_TARGETS] = ""
            it[KEY_ALARM_FIRED] = false
        }
    }

    /** 스누즈: now + minutes 분 동안 알람을 보류합니다. */
    suspend fun snoozeAlarm(minutes: Int) {
        val until = System.currentTimeMillis() + minutes.coerceAtLeast(1) * 60_000L
        context.dataStore.edit {
            it[KEY_SNOOZE_UNTIL] = until
            // 스누즈한 알람은 발화한 것으로 표시해 중복 발화를 막습니다.
            it[KEY_FIRED_TARGETS] = ""
            it[KEY_ALARM_FIRED] = false
        }
    }

    suspend fun setAlarmSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { it[KEY_ALARM_SNOOZE_MIN] = minutes.coerceIn(1, 60) }
    }

    suspend fun setChargeLimitEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[KEY_CHARGE_LIMIT_ENABLED] = enabled
            it[KEY_CHARGE_LIMIT_NOTIFIED] = false
        }
    }

    suspend fun setChargeLimitPercent(percent: Int) {
        context.dataStore.edit { it[KEY_CHARGE_LIMIT_PERCENT] = percent.coerceIn(50, 95) }
    }

    suspend fun setChargeLimitNotified() {
        context.dataStore.edit { it[KEY_CHARGE_LIMIT_NOTIFIED] = true }
    }

    suspend fun resetChargeLimitNotified() {
        context.dataStore.edit { it[KEY_CHARGE_LIMIT_NOTIFIED] = false }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[KEY_FONT_SCALE] = scale.coerceIn(0.85f, 1.5f) }
    }

    // ── 충전 기록 (통계) ──

    val historyFlow: Flow<List<ChargeRecord>> = context.dataStore.data.map { prefs ->
        ChargeHistoryRepository.decode(prefs[KEY_CHARGE_HISTORY])
    }

    suspend fun readHistory(): List<ChargeRecord> =
        ChargeHistoryRepository.decode(context.dataStore.data.first()[KEY_CHARGE_HISTORY])

    suspend fun writeHistory(records: List<ChargeRecord>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CHARGE_HISTORY] = ChargeHistoryRepository.encode(records)
        }
    }

    suspend fun setAlarmWithSound(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ALARM_WITH_SOUND] = enabled }
    }

    // 발화 기록은 [markAlarmFired] / [resetAlarmFired] 로 관리합니다.
    // 불리언 하나로는 "어느 임계값이 울렸는지"를 표현할 수 없어 다중 알람에서 쓸 수 없습니다.
    suspend fun setAlarmFiredLegacy(fired: Boolean) {
        context.dataStore.edit {
            it[KEY_ALARM_FIRED] = fired
            it[KEY_FIRED_TARGETS] = if (fired) (it[KEY_ALARM_TARGETS] ?: "") else ""
        }
    }

    // ── 배터리 경고 ──

    suspend fun setTempAlertEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_TEMP_ALERT_ENABLED] = enabled }
    }

    suspend fun setTempAlertThreshold(celsius: Float) {
        context.dataStore.edit { it[KEY_TEMP_ALERT_THRESHOLD] = celsius }
    }

    suspend fun setHealthAlertEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HEALTH_ALERT_ENABLED] = enabled }
    }

    // ── 잠금 화면 ──

    suspend fun setShowOnLockScreen(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_ON_LOCK_SCREEN] = enabled }
    }

    /**
     * 보상형 광고 1회 시청분(1 credit)을 적립합니다.
     *
     * 정책: 광고가 미노출(fill 실패)되어도 기능이 막히지 않아야 하므로,
     * 이 함수는 **광고를 끝까지 시청했을 때만** 호출됩니다.
     *
     * 누적 [KEY_FREE_UNLOCK_CREDITS] 크레딧은 광고 1회와 동일한 가치로 취급하며,
     * 잠금 해제 시 1개 소비됩니다.
     *
     * 완료 횟수에 도달하면 잠금을 해제하고 진행도를 초기화합니다.
     * 초기화하지 않으면 이미 해제된 항목을 무한정 반복 시청할 수 있어
     * 보상형 광고 정책 위반이 됩니다.
     *
     * @return 적립 직후의 누적 시청 횟수 (해제 완료 시 required 값으로 보고됨)
     */
    suspend fun addRewardCredit(premiumId: String): Int {
        var watchCount = 0
        val required = PremiumCatalog.ADS_REQUIRED_PER_UNLOCK
        context.dataStore.edit { prefs ->
            if ((prefs[KEY_UNLOCKED_PREMIUM] ?: emptySet()).contains(premiumId)) return@edit
            val current = decodeProgress(prefs[KEY_REWARD_PROGRESS])
            val next = (current[premiumId] ?: 0) + 1
            watchCount = next

            val freeCredits = prefs[KEY_FREE_UNLOCK_CREDITS] ?: 0
            if (next + freeCredits >= required) {
                prefs[KEY_UNLOCKED_PREMIUM] = (prefs[KEY_UNLOCKED_PREMIUM] ?: emptySet()) + premiumId
                prefs[KEY_PREMIUM_UNLOCKED] = true
                // 광고 시청분을 소모했으므로 무료 크레딧은 그대로 두고 진행도만 초기화
                val remaining = current.toMutableMap()
                remaining.remove(premiumId)
                if (remaining.isEmpty()) prefs.remove(KEY_REWARD_PROGRESS)
                else prefs[KEY_REWARD_PROGRESS] = encodeProgress(remaining)
                watchCount = required
            } else {
                prefs[KEY_REWARD_PROGRESS] = encodeProgress(current + (premiumId to next))
            }
        }
        return watchCount
    }

    /**
     * 무료 크레딧으로 즉시 잠금을 해제합니다.
     *
     * 누적 광고 시청 횟수 + 보유 크레딧이 필요한 횟수 이상이면 광고를 보지 않고
     * 바로 해제합니다. 크레딧은 1개 소비됩니다.
     *
     * @return 해제되었으면 true, 크레딧이 부족하면 false (광고를 보여줘야 함)
     */
    suspend fun tryUnlockWithFreeCredit(premiumId: String): Boolean {
        var unlocked = false
        val required = PremiumCatalog.ADS_REQUIRED_PER_UNLOCK
        context.dataStore.edit { prefs ->
            if ((prefs[KEY_UNLOCKED_PREMIUM] ?: emptySet()).contains(premiumId)) {
                unlocked = true
                return@edit
            }
            val freeCredits = prefs[KEY_FREE_UNLOCK_CREDITS] ?: 0
            val current = decodeProgress(prefs[KEY_REWARD_PROGRESS])
            val seen = current[premiumId] ?: 0
            if (freeCredits <= 0 || seen + freeCredits < required) return@edit

            prefs[KEY_UNLOCKED_PREMIUM] = (prefs[KEY_UNLOCKED_PREMIUM] ?: emptySet()) + premiumId
            prefs[KEY_PREMIUM_UNLOCKED] = true
            prefs[KEY_FREE_UNLOCK_CREDITS] = freeCredits - 1
            val remaining = current.toMutableMap()
            remaining.remove(premiumId)
            if (remaining.isEmpty()) prefs.remove(KEY_REWARD_PROGRESS)
            else prefs[KEY_REWARD_PROGRESS] = encodeProgress(remaining)
            unlocked = true
        }
        return unlocked
    }

    /**
     * 무료 잠금 해제 크레딧을 1개 지급합니다.
     * 커스텀 오디오/영상 적용 후 보상형 광고를 시청했을 때 호출됩니다.
     */
    suspend fun grantFreeUnlockCredit() {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_FREE_UNLOCK_CREDITS] ?: 0
            // 무한 축적을 막기 위해 상한을 둡니다
            prefs[KEY_FREE_UNLOCK_CREDITS] = (current + 1)
                .coerceAtMost(PremiumCatalog.ADS_REQUIRED_PER_UNLOCK)
        }
    }

    /**
     * 광고를 N분 동안 숨깁니다 (배너/네이티브 비표시).
     */
    suspend fun startAdFreePeriod(minutes: Int) {
        val until = System.currentTimeMillis() + minutes * 60_000L
        context.dataStore.edit { it[KEY_AD_FREE_UNTIL] = until }
    }

    suspend fun saveCustomAudio(uri: Uri): Result<String> {
        return try {
            val fileName = queryFileName(uri) ?: "custom_audio.mp3"
            val extension = fileName.substringAfterLast('.', "mp3")
            val targetFile = File(context.filesDir, "custom_charge_sound.$extension")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(Exception("Cannot read audio URI"))

            context.dataStore.edit {
                it[KEY_CUSTOM_SOUND_NAME] = fileName
                it[KEY_CUSTOM_SOUND_PATH] = targetFile.absolutePath
                it[KEY_SOUND_MODE] = SoundMode.CUSTOM.name
            }
            Result.success(fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom audio", e)
            Result.failure(e)
        }
    }

    suspend fun clearCustomAudio() {
        context.dataStore.edit {
            it.remove(KEY_CUSTOM_SOUND_NAME)
            it.remove(KEY_CUSTOM_SOUND_PATH)
            it[KEY_SOUND_MODE] = SoundMode.PRESET.name
        }
    }

    suspend fun saveCustomVideo(uri: Uri): Result<String> {
        return try {
            val fileName = queryFileName(uri) ?: "custom_video.mp4"
            val extension = fileName.substringAfterLast('.', "mp4")
            val targetFile = File(context.filesDir, "custom_charge_anim.$extension")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(Exception("Cannot read video URI"))

            context.dataStore.edit {
                it[KEY_CUSTOM_VIDEO_NAME] = fileName
                it[KEY_CUSTOM_VIDEO_PATH] = targetFile.absolutePath
                it[KEY_ANIMATION_MODE] = AnimationMode.CUSTOM_VIDEO.name
            }
            Result.success(fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom video", e)
            Result.failure(e)
        }
    }

    suspend fun clearCustomVideo() {
        context.dataStore.edit {
            it.remove(KEY_CUSTOM_VIDEO_NAME)
            it.remove(KEY_CUSTOM_VIDEO_PATH)
            it[KEY_ANIMATION_MODE] = AnimationMode.BUILTIN_LIGHTNING.name
        }
    }

    private fun queryFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to query file name for URI: $uri", e)
            }
        }
        return name ?: uri.lastPathSegment
    }
}
