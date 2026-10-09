package com.example.data

enum class SoundMode {
    PRESET,
    CUSTOM
}

enum class AnimationMode(val displayName: String, val description: String) {
    BUILTIN_LIGHTNING("일렉트릭 라이트닝", "전기 스파크와 네온 전류 효과"),
    BUILTIN_REACTOR("사이버 리액터", "아크 원자로 충전 회전 코어"),
    BUILTIN_PLASMA("퀀텀 플라즈마", "네온 에너지 방울 및 입자 효과"),
    PREMIUM_NEBULA("성운 (Nebula)", "회전하는 성운 입자 필드"),
    PREMIUM_MATRIX("매트릭스 레인", "글리치처럼 떨어지는 코드 열"),
    PREMIUM_ABYSS("심해 (Abyss)", "깊은 파도와 부유하는 산호"),
    PREMIUM_PRISM("프리즘 (Prism)", "빛을 분해하는 다각형 굴절"),
    PREMIUM_FIREWORKS("불꽃 (Fireworks)", "정기적으로 터지는 불꽃"),
    PREMIUM_BLACKHOLE("블랙홀", "소용돌이 빨려 들어가는 사건의 경계"),
    CUSTOM_VIDEO("커스텀 영상 (MP4/AVI)", "사용자가 직접 선택한 비디오 파일");

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
    val name: String,
    val description: String,
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
    val freeUnlockCredits: Int = 0
) {
    val durationLabel: String
        get() = when (videoDurationSeconds) {
            -1 -> "영상 전체 재생"
            0 -> "터치할 때까지 계속 재생"
            else -> "${videoDurationSeconds}초"
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
}
