package com.example.ad

/**
 * 디버그 빌드 전용 광고 단위.
 *
 * ⚠️ Google 테스트 단위는 실제 트래픽에서 쓰면 계정이 정지됩니다.
 *    이 파일은 debug 소스 세트에만 포함되며, release 빌드에는 컴파일되지 않습니다.
 *
 * 로컬에서 광고 노출 흐름을 확인할 때만 사용하세요.
 * 실 단위 ID는 app/src/main/.../AdUnits.kt 에 있습니다.
 */
internal object AdUnitsDebug {
    const val BANNER = "ca-app-pub-3940256099942544/6300978111"
    const val NATIVE = "ca-app-pub-3940256099942544/2247696110"
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
    const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
}