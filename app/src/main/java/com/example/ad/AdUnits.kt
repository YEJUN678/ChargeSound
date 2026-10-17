package com.example.ad

/**
 * 광고 단위 설정 (ironSource / Unity LevelPlay).
 *
 * ## 왜 AdMob을 떠났는가
 * AdMob은 **자사가 지원하는 스토어에 앱이 게시되어 있어야** 광고를 게재합니다.
 * 지원 스토어는 Google Play / Amazon Appstore / OPPO App Market / Samsung Galaxy Store /
 * VIVO App Store / Xiaomi GetApps 뿐이며, OneStore는 포함되지 않습니다.
 * OneStore에 게시된 앱은 "게재 제한(limited ad serving)" 상태에 머물러 실 단위가 채워지지 않습니다.
 *
 * AppLovin도 계정 가입 단계에서 Application store URL을 Play / App Store로만 검증해
 * OneStore URL을 거부했습니다. LevelPlay는 스토어 URL을 요구하지 않아 그대로 진행 가능합니다.
 *
 * ## 채워야 할 값
 * 1. APP_KEY — LevelPlay 대시보드 Account > My Account > Keys
 * 2. 아래 placement ID 3개 — 대시보드 광고 단위 목록에서 복사
 *
 * ⚠️ 빈 상태로 빌드해도 앱은 100% 정상 동작합니다 (광고만 표시되지 않음).
 */
object AdUnits {

    /** LevelPlay App Key (SDK Key) */
    const val APP_KEY: String = ""

    /** 배너 (앱 하단 고정) */
    const val BANNER_PLACEMENT: String = "9mcw3kw9sziwg1jz"

    /** 스폰서 카드 — 네이티브 단위를.rectangle 배너로 사용 (아래 주석 참고) */
    const val NATIVE_PLACEMENT: String = "ucwxlw42iknaw9ra"

    /** 보상형 — 프리미엄 잠금 해제 / 광고 없는 시간 / 크레딧 지급 */
    const val REWARDED_PLACEMENT: String = "25usihf6uxpszb0u"

    val isConfigured: Boolean
        get() = APP_KEY.isNotBlank() &&
            BANNER_PLACEMENT.isNotBlank() &&
            REWARDED_PLACEMENT.isNotBlank()
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
     *
     * 전면 형식을 다시 도입하려면 아래 상수를 되살리고 AdManager 에
     * 노출 로직을 추가해야 합니다. 현재는 정책적으로 차단되어 있습니다.
     */

    /** 보상형 광고: 연속 실패 시 노출을 중단하는 최대 시도 횟수 */
    const val MAX_LOAD_FAILURES = 3

    /** "광고 없는 N분" 기본 선택값 */
    const val DEFAULT_AD_FREE_MINUTES = 60

    /** 보상형 광고 재로드 쿨다운 (마지막 노출 직후 연속 요청 방지) */
    const val REWARDED_RELOAD_COOLDOWN_MS = 60_000L
}