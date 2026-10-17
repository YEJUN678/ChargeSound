package com.example.data

import androidx.annotation.StringRes
import com.example.R

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
 * property 정책 위반으로 앱과 광고 계정이 함께 정지됩니다.
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
            nameRes = R.string.prm_synthwave_name,
            descriptionRes = R.string.prm_synthwave_desc,
            iconName = "bolt",
            accentArgb = 0xFFFF6BA8,
            remoteUrl = null
        ),
        PremiumSound(
            id = "prm_chiptune",
            nameRes = R.string.prm_chiptune_name,
            descriptionRes = R.string.prm_chiptune_desc,
            iconName = "music",
            accentArgb = 0xFF4ADE80,
            remoteUrl = null
        ),
        PremiumSound(
            id = "prm_ambient",
            nameRes = R.string.prm_ambient_name,
            descriptionRes = R.string.prm_ambient_desc,
            iconName = "waves",
            accentArgb = 0xFF60A5FA,
            remoteUrl = null
        ),
        PremiumSound(
            id = "prm_brass",
            nameRes = R.string.prm_brass_name,
            descriptionRes = R.string.prm_brass_desc,
            iconName = "trumpet",
            accentArgb = 0xFFFBBF24,
            remoteUrl = null
        ),
        PremiumSound(
            id = "prm_rainbow",
            nameRes = R.string.prm_rainbow_name,
            descriptionRes = R.string.prm_rainbow_desc,
            iconName = "bell",
            accentArgb = 0xFFC084FC,
            remoteUrl = null
        ),
        PremiumSound(
            id = "prm_vaporwave",
            nameRes = R.string.prm_vaporwave_name,
            descriptionRes = R.string.prm_vaporwave_desc,
            iconName = "speed",
            accentArgb = 0xFF22D3EE,
            remoteUrl = null
        )
    )

    val animations: List<PremiumAnimation> = listOf(
        PremiumAnimation(
            id = "prm_anim_nebula",
            nameRes = R.string.mode_nebula_name,
            descriptionRes = R.string.mode_nebula_desc,
            accentArgb = 0xFFA78BFA,
            mode = AnimationMode.PREMIUM_NEBULA,
            remoteUrl = null
        ),
        PremiumAnimation(
            id = "prm_anim_matrix",
            nameRes = R.string.mode_matrix_name,
            descriptionRes = R.string.mode_matrix_desc,
            accentArgb = 0xFF4ADE80,
            mode = AnimationMode.PREMIUM_MATRIX,
            remoteUrl = null
        ),
        PremiumAnimation(
            id = "prm_anim_ocean",
            nameRes = R.string.mode_abyss_name,
            descriptionRes = R.string.mode_abyss_desc,
            accentArgb = 0xFF38BDF8,
            mode = AnimationMode.PREMIUM_ABYSS,
            remoteUrl = null
        ),
        PremiumAnimation(
            id = "prm_anim_prism",
            nameRes = R.string.mode_prism_name,
            descriptionRes = R.string.mode_prism_desc,
            accentArgb = 0xFFF472B6,
            mode = AnimationMode.PREMIUM_PRISM,
            remoteUrl = null
        ),
        PremiumAnimation(
            id = "prm_anim_fireworks",
            nameRes = R.string.mode_fireworks_name,
            descriptionRes = R.string.mode_fireworks_desc,
            accentArgb = 0xFFFB923C,
            mode = AnimationMode.PREMIUM_FIREWORKS,
            remoteUrl = null
        ),
        PremiumAnimation(
            id = "prm_anim_blackhole",
            nameRes = R.string.mode_blackhole_name,
            descriptionRes = R.string.mode_blackhole_desc,
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
        SoundPreset(sound.id, sound.nameRes, sound.descriptionRes, sound.iconName)
}

data class PremiumSound(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val iconName: String,
    val accentArgb: Long,
    /** 합법적으로 라이선스한 원본 WAV URL (없으면 로컬 합성 사용) */
    val remoteUrl: String? = null
)

data class PremiumAnimation(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val accentArgb: Long,
    val mode: AnimationMode,
    /** 합법적으로 라이선스한 원본 MP4 URL (없으면 실시간 렌더링 사용) */
    val remoteUrl: String? = null
)