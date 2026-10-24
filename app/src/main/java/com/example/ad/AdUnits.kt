package com.example.ad

/**
 * 광고 단위 설정 (Google AdMob / Google Mobile Ads SDK).
 *
 * ## 현재 상태
 * `USE_TEST_ADS = true` 입니다. Google 공식 테스트 광고 단위를 쓰므로 **수익이 없고**,
 * 광고가 실제로 뜨는지만 확인할 수 있습니다. 실 광고로 바꾸려면 아래 두 곳을 함께
 * 바꿔야 합니다.
 *
 * 1. 이 파일의 `USE_TEST_ADS` → false
 * 2. `app/build.gradle.kts` 의 `admobAppId` → 실제 앱 ID
 *    (AdMob 콘솔 → 앱 선택 → 앱 설정에 `ca-app-pub-XXXX~YYYY` 형태로 있습니다)
 *
 * 앱 ID 와 광고 단위 ID 는 같은 AdMob 앱의 것이어야 합니다. 서로 다르면
 * 광고 요청이 실패하고 앱에는 광고가 표시되지 않습니다(앱은 정상 동작).
 *
 * ## 광고 정책
 * - 광고를 거부하거나 로드에 실패해도 앱의 핵심 기능은 그대로 동작합니다.
 * - 전면(Interstitial) · 앱오픈 광고는 사용하지 않습니다 ([AdPolicy] 참고).
 * - 충전 애니메이션/사운드 재생 중에는 어떤 광고도 띄우지 않습니다.
 */
object AdUnits {

    /**
     * 테스트 광고 사용 여부.
     * 배포용 빌드에서는 반드시 false 로 바꾸세요. true 인 채로 배포하면
     * 수익이 발생하지 않습니다.
     */
    const val USE_TEST_ADS: Boolean = false

    // ─────────────────── 실제 광고 단위 (AdMob 콘솔) ───────────────────

    /** 앱 ID — 물결표(~) 형태입니다. 광고 단위 ID 와 다릅니다. */
    const val PROD_APP_ID: String = "ca-app-pub-4709628417690363~4678252154"

    /** 배너 (앱 하단 고정) */
    const val PROD_BANNER_UNIT: String = "ca-app-pub-4709628417690363/2642367320"

    /** 네이티브 (스폰서 카드) */
    const val PROD_NATIVE_UNIT: String = "ca-app-pub-4709628417690363/3616451924"

    /** 보상형 (프리미엄 잠금 해제 / 광고 없는 시간) */
    const val PROD_REWARDED_UNIT: String = "ca-app-pub-4709628417690363/7208212575"

    /**
     * 전면 광고 단위 — **사용하지 않습니다.**
     * 충전 감지 유틸리티는 시작/종료 지점이 모호해 전면 광고를 넣으면
     * 스토어 심사와 사용자 경험 양쪽에서 문제가 됩니다. ([AdPolicy] 참고)
     */
    const val PROD_INTERSTITIAL_UNIT: String = "ca-app-pub-4709628417690363/4929533596"

    // ─────────────────── Google 공식 테스트 단위 ───────────────────

    private const val TEST_APP_ID: String = "ca-app-pub-3940256099942544~3347511713"
    private const val TEST_BANNER_UNIT: String = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_NATIVE_UNIT: String = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_REWARDED_UNIT: String = "ca-app-pub-3940256099942544/5224354917"

    val appId: String get() = if (USE_TEST_ADS) TEST_APP_ID else PROD_APP_ID
    val bannerUnit: String get() = if (USE_TEST_ADS) TEST_BANNER_UNIT else PROD_BANNER_UNIT
    val nativeUnit: String get() = if (USE_TEST_ADS) TEST_NATIVE_UNIT else PROD_NATIVE_UNIT
    val rewardedUnit: String get() = if (USE_TEST_ADS) TEST_REWARDED_UNIT else PROD_REWARDED_UNIT

    /**
     * 광고를 시도해도 되는 상태인지.
     *
     * 실제 단위로 전환했는데 앱 ID 를 아직 채우지 않은 경우(placeholder 가 남아 있으면)
     * false 가 되어 SDK 초기화를 건너뜁니다. 잘못된 ID 로 초기화하면 앱이 죽을 수
     * 있으므로 이 가드가 필요합니다.
     */
    val isConfigured: Boolean
        get() = if (USE_TEST_ADS) {
            true
        } else {
            appId.startsWith("ca-app-pub-") &&
                !appId.contains("PUT_APP_ID_HERE") &&
                bannerUnit.isNotBlank() &&
                rewardedUnit.isNotBlank()
        }
}

/**
 * 광고 표시 정책 상수.
 *
 * 플랫폼 정책 요약:
 * - 광고를 거부하거나 로드에 실패해도 앱의 핵심 기능이 막히면 안 됩니다.
 * - 충전 애니메이션/사운드 재생을 방해하는 시점에 전면류 광고를 띄우지 않습니다.
 * - 보상형 광고는 사용자가 명시적으로 선택해야 하며, 거부해도 모든 기능이 그대로 동작합니다.
 */
object AdPolicy {

    /**
     * 전면(Interstitial) · 앱오픈 광고는 사용하지 않습니다.
     *
     * 충전 감지 유틸리티는 명확한 시작/종료 지점이 없어 전면 광고를 쓰기 어렵고,
     * 강제 노출 시 스토어 심사에서 걸릴 수 있습니다.
     * 배너 · 스폰서 카드 · 보상형만 사용합니다.
     */

    /** 보상형 광고: 연속 실패 시 노출을 중단하는 최대 시도 횟수 */
    const val MAX_LOAD_FAILURES = 3

    /** "광고 없는 N분" 기본 선택값 */
    const val DEFAULT_AD_FREE_MINUTES = 60

    /** 보상형 광고 재로드 쿨다운 (마지막 노출 직후 연속 요청 방지) */
    const val REWARDED_RELOAD_COOLDOWN_MS = 60_000L
}
