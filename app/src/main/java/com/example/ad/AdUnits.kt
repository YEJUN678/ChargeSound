package com.example.ad

/**
 * 빌드 변형별로 실제 사용할 광고 단위를 결정합니다.
 *
 * - debug 소스 세트: Google 테스트 ID (실 트래픽에 노출되면 계정 정지)
 * - release 소스 세트: 실제 AdMob ID
 *
 * `AdUnitIds`를 debug/release 양쪽에 같은 이름으로 정의해,
 * release APK에 테스트 ID 문자열이 아예 포함되지 않게 합니다.
 *
 * ※ Kotlin `const val`은 컴파일 시 인라인되므로, 조건 분기(getter)로는
 *    release 빌드에서 테스트 ID를 제거할 수 없습니다. 소스 세트 분리가 유일한 방법입니다.
 */
internal object ActiveAdUnits {
    val BANNER: String get() = AdUnitIds.BANNER
    val NATIVE: String get() = AdUnitIds.NATIVE
    val REWARDED: String get() = AdUnitIds.REWARDED
    val APP_OPEN: String get() = AdUnitIds.APP_OPEN
}

/**
 * AdMob 광고 단위 ID (배포용).
 *
 * 앱 ID : ca-app-pub-4709628417690363~2909050501
 *
 * ⚠️ Google 테스트 단위(ca-app-pub-3940256099942544/...)는 이 파일에 두지 않습니다.
 *    테스트 단위는 app/src/debug/.../AdUnitIds.kt 에만 존재합니다.
 *
 * 발급된 단위 목록
 * ------------------------------------------------------------------
 * 앱 ID      ca-app-pub-4709628417690363~2426649445
 * 배너      ca-app-pub-4709628417690363/1500580589
 * 네이티브  ca-app-pub-4709628417690363/5503319056
 * 보상형    ca-app-pub-4709628417690363/4427050261
 * 앱 오픈   ca-app-pub-4709628417690363/7447965278
 *
 * 패키지 네임 com.chargesound.app 로 AdMob에 앱을 추가하면서 발급된 단위입니다.
 * (기존 단위 8993123458/2708674292/8801551767/2023470551 은 구 패키지
 *  com.aistudio.chargesound.vkyrt 기준이라 폐기되었습니다)
 *
 * 전면(Interstitial) 단위는 의도적으로 만들지 않았습니다.
 * AdMob 정책은 "명확한 시작/종료 지점이 있는 앱"에만 전면 광고를 권장하며,
 * 전 화면 애니메이션 앱(유틸리티)은 전면 대신 배너·네이티브·보상형을 쓰도록 안내합니다.
 */
object AdUnits {
    const val APP_ID = "ca-app-pub-4709628417690363~2426649445"
    const val BANNER = "ca-app-pub-4709628417690363/1500580589"
    const val NATIVE = "ca-app-pub-4709628417690363/5503319056"
    const val REWARDED = "ca-app-pub-4709628417690363/4427050261"
    const val APP_OPEN = "ca-app-pub-4709628417690363/7447965278"
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
     * 앱오펜 광고 최소 간격.
     * 앱을 열 때마다 띄우되, 연속으로 빠르게 열면 피로감이 생겨 최소 간격을 둡니다.
     */
    const val APP_OPEN_MIN_INTERVAL_MS = 30L * 60L * 1000L

    /** 세션 내 앱오펜 광고 최대 노출 횟수 */
    const val APP_OPEN_MAX_PER_SESSION = 1

    /** 보상형 광고: 연속 실패 시 노출을 중단하는 최대 시도 횟수 */
    const val MAX_LOAD_FAILURES = 3

    /** "광고 없이 N분" 기본 선택값 */
    const val DEFAULT_AD_FREE_MINUTES = 60
}