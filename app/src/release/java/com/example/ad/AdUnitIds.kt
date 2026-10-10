package com.example.ad

/**
 * 릴리스 빌드에서 사용하는 광고 단위 ID.
 *
 * 실제 AdMob 단위 ID가 release APK에 들어가는 유일한 경로입니다.
 * 테스트 ID 문자열이 release 빌드에 포함되지 않도록 debug/release를 소스 세트로 분리했습니다.
 */
internal object AdUnitIds {
    const val BANNER = AdUnits.BANNER
    const val NATIVE = AdUnits.NATIVE
    const val REWARDED = AdUnits.REWARDED
    const val APP_OPEN = AdUnits.APP_OPEN
}