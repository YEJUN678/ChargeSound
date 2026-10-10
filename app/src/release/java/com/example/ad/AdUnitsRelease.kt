package com.example.ad

/**
 * 릴리스 빌드 전용 광고 단위.
 *
 * 실제 AdMob 단위 ID가 release APK에 들어가는 유일한 경로입니다.
 * 테스트 ID 문자열이 release 빌드에 포함되지 않도록 소스 세트를 분리했습니다.
 */
internal object AdUnitsRelease {
    const val BANNER = AdUnits.BANNER
    const val NATIVE = AdUnits.NATIVE
    const val REWARDED = AdUnits.REWARDED
    const val APP_OPEN = AdUnits.APP_OPEN
}