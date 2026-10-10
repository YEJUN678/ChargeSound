package com.example.ad

/**
 * 디버그 빌드에서 사용하는 광고 단위 ID.
 *
 * ⚠️ Google 테스트 단위는 실제 트래픽에서 쓰면 계정이 정지됩니다.
 *    이 파일은 debug 소스 세트에만 컴파일됩니다.
 *
 * 로컬에서 광고 노출 흐름을 확인할 때만 사용하세요.
 */
internal object AdUnitIds {
    const val BANNER = "ca-app-pub-3940256099942544/6300978111"
    const val NATIVE = "ca-app-pub-3940256099942544/2247696110"
    const val REWARDED = "ca-app-pub-3940256099942544/5224354917"
    const val APP_OPEN = "ca-app-pub-3940256099942544/9257395921"
}