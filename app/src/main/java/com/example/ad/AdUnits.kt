package com.example.ad

import com.example.BuildConfig

/**
 * AdMob 광고 단위 ID.
 *
 * 앱 ID : ca-app-pub-4709628417690363~2909050501
 *
 * ⚠️ 실 트래픽에 Google 테스트 단위를 쓰면 계정이 정지될 수 있습니다.
 * 아래 TEST_* 상수는 로컬에서 광고 노출 흐름을 확인할 때만 사용하며,
 * [bannerId] / [nativeId] / [rewardedId] / [appOpenId] 를 통해 빌드 타입에 맞게
 * 자동으로 선택됩니다. 코드에서 테스트 ID를 직접 참조하지 마세요.
 *
 * 발급된 단위 목록
 * ------------------------------------------------------------------
 * 배너      ca-app-pub-4709628417690363/8993123458
 * 네이티브  ca-app-pub-4709628417690363/2708674292
 * 보상형    ca-app-pub-4709628417690363/8801551767
 * 앱 오픈   ca-app-pub-4709628417690363/2023470551
 *
 * 전면(Interstitial) 단위는 의도적으로 만들지 않았습니다.
 * AdMob 정책은 "명확한 시작/종료 지점이 있는 앱"에만 전면 광고를 권장하며,
 * 전 화면 애니메이션 앱(유틸리티)은 전면 대신 배너·네이티브·보상형을 쓰도록 안내합니다.
 */
object AdUnits {

    // ── 실제 광고 단위 (배포용) ──
    const val BANNER = "ca-app-pub-4709628417690363/8993123458"
    const val NATIVE = "ca-app-pub-4709628417690363/2708674292"
    const val REWARDED = "ca-app-pub-4709628417690363/8801551767"
    const val APP_OPEN = "ca-app-pub-4709628417690363/2023470551"

    // ── Google 공식 테스트 단위 (디버그 빌드 전용) ──
    const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_NATIVE = "ca-app-pub-3940256099942544/2247696110"
    const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"
    const val TEST_APP_OPEN = "ca-app-pub-3940256099942544/9257395921"

    /**
     * 빌드 타입에 맞는 광고 단위 ID.
     * 디버그 빌드 = 테스트 단위, 릴리스 빌드 = 실제 단위.
     *
     * 앱 체크에서 실 광고 단위를 실수로 띄워 계정이 정지되는 사고를 막기 위한 장치입니다.
     */
    val bannerId: String get() = if (BuildConfig.DEBUG) TEST_BANNER else BANNER
    val nativeId: String get() = if (BuildConfig.DEBUG) TEST_NATIVE else NATIVE
    val rewardedId: String get() = if (BuildConfig.DEBUG) TEST_REWARDED else REWARDED
    val appOpenId: String get() = if (BuildConfig.DEBUG) TEST_APP_OPEN else APP_OPEN
}

/**
 * 광고 표시 정책 상수.
 *
 * AdMob 정책 요약:
 * - 전면/앱오픈 광고는 앱의 핵심 기능을 방해하면 안 됩니다.
 * - 충전 애니메이션이 재생 중이거나 사운드를 미리듣는 중이면 광고를 표시하지 않습니다.
 * - 보상형 광고는 사용자가 명시적으로 선택해야 하며, 거부해도 앱 사용이 막히면 안 됩니다.
 */
object AdPolicy {
    /**
     * 앱오픈 광고 최소 간격.
     * 앱을 열 때마다 띄우되, 연속으로 빠르게 열면 피로감이 생겨 최소 간격을 둡니다.
     */
    const val APP_OPEN_MIN_INTERVAL_MS = 30L * 60L * 1000L

    /** 세션 내 앱오픈 광고 최대 노출 횟수 */
    const val APP_OPEN_MAX_PER_SESSION = 1

    /** 보상형 광고: 연속 실패 시 노출을 중단하는 최대 시도 횟수 */
    const val MAX_LOAD_FAILURES = 3

    /** "광고 없이 N분" 기본 선택값 */
    const val DEFAULT_AD_FREE_MINUTES = 60
}