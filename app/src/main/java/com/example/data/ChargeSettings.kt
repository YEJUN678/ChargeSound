package com.example.data

import android.content.Context
import androidx.annotation.StringRes
import com.example.R

enum class SoundMode {
    PRESET,
    CUSTOM
}

/**
 * 애니메이션 테마.
 *
 * 표시 이름/설명은 컴포저블이 아닌 여기서 만들어지므로 문자열이 아니라
 * 리소스 ID로 들고 있습니다. 실제 문자열은 UI에서 `stringResource()`로 꺼냅니다.
 */
enum class AnimationMode(
    @StringRes val nameRes: Int,
    @StringRes val descRes: Int
) {
    BUILTIN_LIGHTNING(R.string.mode_lightning_name, R.string.mode_lightning_desc),
    BUILTIN_REACTOR(R.string.mode_reactor_name, R.string.mode_reactor_desc),
    BUILTIN_PLASMA(R.string.mode_plasma_name, R.string.mode_plasma_desc),
    PREMIUM_NEBULA(R.string.mode_nebula_name, R.string.mode_nebula_desc),
    PREMIUM_MATRIX(R.string.mode_matrix_name, R.string.mode_matrix_desc),
    PREMIUM_ABYSS(R.string.mode_abyss_name, R.string.mode_abyss_desc),
    PREMIUM_PRISM(R.string.mode_prism_name, R.string.mode_prism_desc),
    PREMIUM_FIREWORKS(R.string.mode_fireworks_name, R.string.mode_fireworks_desc),
    PREMIUM_BLACKHOLE(R.string.mode_blackhole_name, R.string.mode_blackhole_desc),
    CUSTOM_VIDEO(R.string.mode_custom_video_name, R.string.mode_custom_video_desc);

    /** 프리미엄 테마는 광고 시청으로 해제해야 사용할 수 있습니다. */
    val isPremium: Boolean
        get() = name.startsWith("PREMIUM_")
}

/**
 * 프리미엄 애니메이션 테마 ID ↔ [AnimationMode] 매핑.
 * 잠금 해제 상태는 테마 ID로 저장하므로, 카탈로그를 확장해도 기존 사용자 데이터가 유지됩니다.
 */
object PremiumAnimationModes {
    val byMode: Map<AnimationMode, String> = mapOf(
        AnimationMode.PREMIUM_NEBULA to "prm_anim_nebula",
        AnimationMode.PREMIUM_MATRIX to "prm_anim_matrix",
        AnimationMode.PREMIUM_ABYSS to "prm_anim_ocean",
        AnimationMode.PREMIUM_PRISM to "prm_anim_prism",
        AnimationMode.PREMIUM_FIREWORKS to "prm_anim_fireworks",
        AnimationMode.PREMIUM_BLACKHOLE to "prm_anim_blackhole"
    )

    fun premiumIdOf(mode: AnimationMode): String? = byMode[mode]

    fun modeOfPremiumId(id: String): AnimationMode? =
        byMode.entries.firstOrNull { it.value == id }?.key
}

data class SoundPreset(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val iconName: String
)

data class ChargeSettings(
    val isServiceEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val volume: Float = 0.85f,
    val soundMode: SoundMode = SoundMode.PRESET,
    val presetSoundId: String = "cyber_chime",
    val customSoundName: String? = null,
    val customSoundPath: String? = null,
    val vibrationEnabled: Boolean = true,
    val animationEnabled: Boolean = true,
    val animationMode: AnimationMode = AnimationMode.BUILTIN_LIGHTNING,
    val customVideoName: String? = null,
    val customVideoPath: String? = null,
    val videoDurationSeconds: Int = 5, // 3, 5, 10, 15, 30, -1 (전체 재생), 0 (터치할 때까지)
    val videoLoop: Boolean = true,
    val playOnDisconnect: Boolean = false,
    // ── 프리미엄 / 수익화 ──
    /** 보상형 광고로 해제된 프리미엄 콘텐츠 ID 집합 (사운드 + 애니메이션 혼용) */
    val unlockedPremiumIds: Set<String> = emptySet(),
    /** 광고 시청 진행도. key = 프리미엄 ID, value = 지금까지 본 광고 횟수 */
    val rewardProgress: Map<String, Int> = emptyMap(),
    /** 광고가 숨겨질 때까지의 epoch millis. 0이면 비활성 */
    val adFreeUntil: Long = 0L,
    /** 사용자가 "다른 앱 위에 표시" 권한을 부여했는지 (캐시, 실제값은 설정 화면에서 재확인) */
    val overlayPermissionGranted: Boolean = false,
    /** 사용자가 권한 안내를 이미 확인했는지 */
    val overlayPromptSeen: Boolean = false,
    /** 사용자 맞춤 고빈도 설정 (프리미엄 콘텐츠 용량 해제 후 활성화) */
    val premiumUnlocked: Boolean = false,
    /**
     * 광고 시청으로 지급된 무료 잠금 해제 크레딧.
     * 커스텀 오디오/영상 적용 후 보상형 광고를 시청하면 1개씩 쌓입니다.
     * [PremiumCatalog.ADS_REQUIRED_PER_UNLOCK] 회 대신 이 크레딧으로 잠금을 열 수 있습니다.
     */
    val freeUnlockCredits: Int = 0,

    // ── 충전 알람 ──
    /** 설정된 충전율에 도달하면 알람을 울립니다 */
    val alarmEnabled: Boolean = false,
    /** 알람을 울릴 충전율 (10~100) */
    val alarmTargetPercent: Int = 80,
    /**
     * 현재 충전 세션에서 이미 울린 알람인지.
     * 한 번 울리면 전원을 뽑을 때까지 다시 울리지 않습니다.
     */
    val alarmFiredThisSession: Boolean = false,
    /** 알람에 사운드를 함께 재생할지 (off면 진동 + 알림만) */
    val alarmWithSound: Boolean = true,

    // ── 배터리 경고 ──
    /** 배터리가 설정 온도를 넘으면 경고 알림을 보냅니다 */
    val tempAlertEnabled: Boolean = false,
    /** 경고 온도 (℃) */
    val tempAlertThresholdC: Float = 45f,
    /** 배터리 건강이 좋지 않을 때 경고합니다 */
    val healthAlertEnabled: Boolean = true,

    // ── 잠금 화면 ──
    /** 잠금 화면에서도 충전 애니메이션을 재생합니다 */
    val showOnLockScreen: Boolean = true,

    // ── 충전 방식별 사운드 ──
    /** AC / USB / 무선에 각각 다른 프리셋 사운드를 지정합니다 */
    val plugSpecificSounds: Boolean = false,
    val acPresetId: String = "cyber_chime",
    val usbPresetId: String = "lightning_spark",
    val wirelessPresetId: String = "gentle_ding",

    // ── 저전력 · 급방전 경고 ──
    /** 낮은 배터리(기본 20% 이하) 경고 */
    val lowBatteryAlertEnabled: Boolean = true,
    val lowBatteryThreshold: Int = 20,
    /** 비충전 중 급격한 배터리 감소 감지 */
    val rapidDischargeAlertEnabled: Boolean = true,
    /** 급방전으로 판단하는 시간 창(분) */
    val rapidDischargeWindowMinutes: Int = 30,
    /** 해당 시간 동안 이 % 이상 떨어지면 급방전으로 판단 */
    val rapidDischargeDropPercent: Int = 10,

    // ── 조용한 시간 ──
    /** 지정한 시간대에는 충전 사운드를 재생하지 않습니다 (알림·진동은 유지) */
    val quietHoursEnabled: Boolean = false,
    /** 시작 시각 (0~1439 분, 0=00:00) */
    val quietStartMinute: Int = 1320,
    /** 종료 시각 (0~1439 분, 1440=24:00) */
    val quietEndMinute: Int = 420,
    /** 강조색 프리셋 키 (com.example.ui.theme.AccentPalette.keyOf 참조) */
    val accentKey: String = "cyan"
) {

    /**
     * 영상 재생 시간 라벨.
     * 컴포저블 밖에서 호출될 수 있어 Context를 직접 받습니다.
     */
    fun durationLabel(context: Context): String = when (videoDurationSeconds) {
        -1 -> context.getString(R.string.duration_label_full)
        0 -> context.getString(R.string.duration_label_touch)
        else -> context.getString(R.string.duration_label_seconds, videoDurationSeconds)
    }

    fun isUnlocked(premiumId: String): Boolean = premiumId in unlockedPremiumIds

    /** 해당 프리미엄 항목을 해제하려면 앞으로 몇 번 더 광고를 봐야 하는지. 0이면 이미 해제됨. */
    fun remainingAdsToUnlock(premiumId: String): Int {
        if (premiumId in unlockedPremiumIds) return 0
        val seen = rewardProgress[premiumId] ?: 0
        val required = com.example.data.PremiumCatalog.ADS_REQUIRED_PER_UNLOCK
        return (required - seen - freeUnlockCredits).coerceAtLeast(0)
    }

    fun adFreeRemainingMinutes(nowMillis: Long): Int {
        if (adFreeUntil <= nowMillis) return 0
        return ((adFreeUntil - nowMillis) / 60_000L).toInt()
    }

    /** 현재 시각이 조용한 시간대에 해당하는지 */
    fun isQuietHourNow(nowMinuteOfDay: Int): Boolean {
        if (!quietHoursEnabled) return false
        return if (quietStartMinute <= quietEndMinute) {
            nowMinuteOfDay in quietStartMinute until quietEndMinute
        } else {
            // 자정을 넘어가는 범위 (예: 22:00 ~ 07:00)
            nowMinuteOfDay >= quietStartMinute || nowMinuteOfDay < quietEndMinute
        }
    }
}
