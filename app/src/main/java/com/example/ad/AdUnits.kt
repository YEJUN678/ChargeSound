package com.example.ad

/**
 * 빌드 변형별로 실제 사용할 광고 단위를 결정합니다.
 *
 * - debug 소스 세트: 테스트 ID (실 트래픽에 노출되면 계정 정지이므로 절대 배포 금지)
 * - release 소스 세트: 실 ID
 *
 * 출처별로 갈라둠으로써 release 빌드에 테스트 ID 문자열이 아예 포함되지 않게 합니다.
 * Kotlin `const val`은 컴파일 시 인라인되므로, 조건 분기(getter)로는 제거되지 않습니다.
 */
internal object ActiveAdUnits {
    val BANNER: String get() = AdUnitsRelease.BANNER
    val NATIVE: String get() = AdUnitsRelease.NATIVE
    val REWARDED: String get() = AdUnitsRelease.REWARDED
    val APP_OPEN: String get() = AdUnitsRelease.APP_OPEN
}

/**
 * AdMob 광고 단위 ID (배포용).
 *
 * 앱 ID : ca-app-pub-4709628417690363~2909050501
 *
 * ⚠️ Google 테스트 단위(ca-app-pub-3940256099942544/...)는 이 파일에 두지 않습니다.
 *    실 트래픽에 테스트 단위를 쓰면 계정이 정지되며, 상수 인lining 때문에
 *    조건 분기만으로도 release APK에서 제거되지 않기 때문입니다.
 *    테스트 단위는 app/src/debug 소스 세트의 AdUnitsDebug.kt에만 존재합니다.
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
 *
 * 패키지 네임이 com.chargesound.app 로 변경되었으므로, AdMob 콘솔에서
 * 패키지 네임을 일치시키고 이 단위들을 재발급해야 합니다.
 */
object AdUnits {
    const val BANNER = "ca-app-pub-4709628417690363/8993123458"
    const val NATIVE = "ca-app-pub-4709628417690363/2708674292"
    const val REWARDED = "ca-app-pub-4709628417690363/8801551767"
    const val APP_OPEN = "ca-app-pub-4709628417690363/2023470551"
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