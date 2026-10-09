package com.example.data

/**
 * 보상형 광고로 해제되는 프리미엄 콘텐츠 카탈로그.
 *
 * ## 저작권 정책 (중요)
 * 이 카탈로그의 모든 항목은 **절차적으로 생성**됩니다 (오디오는 PCM 파형 합성,
 * 애니메이션은 실시간 렌더링). 외부 파일이나 라이선스 불명의 에셋을 사용하지 않습니다.
 *
 * 서버 배포를 원한다면 [PremiumSound.remoteUrl] / [PremiumAnimation.remoteUrl]에
 * **직접 제작했거나 상업적 이용이 허가된** URL만 채워야 합니다.
 * 유튜브 등 타 플랫폼에서 다운받은 콘텐츠를 재배포하면 Google Play intellectual
 * property 정책 위반으로 앱과 AdMob 계정이 함께 정지됩니다.
 *
 * ## 정책 준수
 * 보상형 광고는 사용자가 광고를 거부해도 앱의 기본 기능이 그대로 동작해야 합니다.
 * 프리미엄 콘텐츠는 "추가로 얻는 것"이지, 기본 프리셋을 잠그는 장치가 아닙니다.
 */
object PremiumCatalog {

    /** 해제에 필요한 보상형 광고 시청 횟수 */
    const val ADS_REQUIRED_PER_UNLOCK = 2

    /** 광고 없이 배너를 숨기는 시간(분) 옵션 */
    val AD_FREE_DURATION_OPTIONS = listOf(30, 60, 180, 720)

    val sounds: List<PremiumSound> = listOf(
        PremiumSound(
            id = "prm_synthwave",
            name = "신스웨이브 레이즈",
            description = "80년대 신스웨이브 아르페지오",
            iconName = "bolt",
            accentArgb = 0xFFFF6BA8,
            remoteUrl = null
        ),
        PremiumSound(
            id = "prm_chiptune",
            name = "칩튠 마블링",
            description = "레트로 8비트 게임 BGM",
            iconName = "music",
            accentArgb = 0xFF4ADE80,
            remoteUrl = null
        ),
        PremiumSound(
            id = "prm_ambient",
            name = "앰비언트 드론",
            description = "깊은 패드 화음의 잔잔한 울림",
            iconName = "waves",
            accentArgb = 0xFF60A5FA,
            remoteUrl = null
        ),
        PremiumSound(
            id = "prm_brass",
            name = "브라스 팡파르",
            description = "장엄한 금속관 팡파르 fanfare",
            iconName = "trumpet",
            accentArgb = 0xFFFBBF24,
            remoteUrl = null
        ),
        PremiumSound(
            id = "prm_rainbow",
            name = "레인보우 벨",
            description = "7음 종소리 캐스케이드",
            iconName = "bell",
            accentArgb = 0xFFC084FC,
            remoteUrl = null
        ),
        PremiumSound(
            id = "prm_vaporwave",
            name = "베이퍼웨이브 그라운드",
            description = "느린 템포의 드림펑크 비트",
            iconName = "speed",
            accentArgb = 0xFF22D3EE,
            remoteUrl = null
        )
    )

    val animations: List<PremiumAnimation> = listOf(
        PremiumAnimation(
            id = "prm_anim_nebula",
            name = "성운 (Nebula)",
            description = "회전하는 성운 입자 필드",
            accentArgb = 0xFFA78BFA,
            mode = AnimationMode.PREMIUM_NEBULA,
            remoteUrl = null
        ),
        PremiumAnimation(
            id = "prm_anim_matrix",
            name = "매트릭스 레인",
            description = "글리치처럼 떨어지는 코드 열",
            accentArgb = 0xFF4ADE80,
            mode = AnimationMode.PREMIUM_MATRIX,
            remoteUrl = null
        ),
        PremiumAnimation(
            id = "prm_anim_ocean",
            name = "심해 (Abyss)",
            description = "깊은 파도와 부유하는 산호",
            accentArgb = 0xFF38BDF8,
            mode = AnimationMode.PREMIUM_ABYSS,
            remoteUrl = null
        ),
        PremiumAnimation(
            id = "prm_anim_prism",
            name = "프리즘 (Prism)",
            description = "빛을 분해하는 다각형 굴절",
            accentArgb = 0xFFF472B6,
            mode = AnimationMode.PREMIUM_PRISM,
            remoteUrl = null
        ),
        PremiumAnimation(
            id = "prm_anim_fireworks",
            name = "불꽃 (Fireworks)",
            description = "정기적으로 터지는 불꽃",
            accentArgb = 0xFFFB923C,
            mode = AnimationMode.PREMIUM_FIREWORKS,
            remoteUrl = null
        ),
        PremiumAnimation(
            id = "prm_anim_blackhole",
            name = "블랙홀",
            description = "소용돌이 빨려 들어가는 사건의 경계",
            accentArgb = 0xFF818CF8,
            mode = AnimationMode.PREMIUM_BLACKHOLE,
            remoteUrl = null
        )
    )

    /** 사용자 데이터를 훼손하지 않으면서 안전하게 조회 */
    fun soundById(id: String): PremiumSound? = sounds.firstOrNull { it.id == id }
    fun animationById(id: String): PremiumAnimation? = animations.firstOrNull { it.id == id }

    fun isPremiumSoundId(id: String?): Boolean = id != null && sounds.any { it.id == id }
    fun isPremiumAnimationId(id: String?): Boolean = id != null && animations.any { it.id == id }

    /** 현재 잠금이 해제되었는지 판정 */
    fun isSoundUnlocked(id: String, unlocked: Set<String>): Boolean = id in unlocked
    fun isAnimationUnlocked(id: String, unlocked: Set<String>): Boolean = id in unlocked

    /**
     * 사람이 읽을 수 있는 이름 → SoundPreset 형태 변환.
     * 프리미엄 사운드는 [com.example.sound.SoundManager]가 합성 WAV를 생성합니다.
     */
    fun soundToPreset(sound: PremiumSound): SoundPreset =
        SoundPreset(sound.id, sound.name, sound.description, sound.iconName)
}

data class PremiumSound(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val accentArgb: Long,
    /** 합법적으로 라이선스한 원본 WAV URL (없으면 로컬 합성 사용) */
    val remoteUrl: String? = null
)

data class PremiumAnimation(
    val id: String,
    val name: String,
    val description: String,
    val accentArgb: Long,
    val mode: AnimationMode,
    /** 합법적으로 라이선스한 원본 MP4 URL (없으면 실시간 렌더링 사용) */
    val remoteUrl: String? = null
)