package com.example.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ironsource.mediationsdk.ISBannerSize
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.model.Placement
import com.ironsource.mediationsdk.sdk.InitializationListener
import com.ironsource.mediationsdk.sdk.LevelPlayBannerListener
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 광고 관리 싱글턴 (ironSource / Unity LevelPlay).
 *
 * 설계 원칙 (광고 플랫폼 정책 준수):
 * 1. **광고를 거부해도 앱의 핵심 기능은 계속 동작한다.** 어떤 실패 경로에서도
 *    프리미엄 콘텐츠 잠금이 풀린 채로 남거나, 사운드/애니메이션 선택이 막히지 않습니다.
 * 2. 광고 로드 실패는 조용히 무시됩니다. 사용자에게 오류를 강요하지 않습니다.
 * 3. 앱 핵심 기능(충전 애니메이션/사운드 재생)에는 전면류 광고를 절대 표시하지 않습니다.
 * 4. 설정이 비어 있으면([AdUnits.isConfigured] == false) 광고를 전혀 시도하지 않고
 *    앱은 완전히 정상 동작합니다.
 */
object AdManager {

    private const val TAG = "AdManager"

    private var initialized = false
    private var rewardedLoaded = false
    private var rewardedFailures = 0
    private var rewardedShowing = false

    private var lastAppOpenShownAt = 0L
    private var appOpenCountThisSession = 0

    /** 마지막 보상형 광고 노출 시각 → 재로드 쿨다운 제어 */
    @Volatile
    private var lastRewardedShownAt = 0L

    private var pendingActivity: Activity? = null

    /** SDK 초기화 완료 플래그 */
    @Volatile
    private var ironSourceReady = false

    /** 앱 핵심 기능(충전 애니메이션/사운드 재생) 수행 중인지. 이 동안 전면류 광고는 차단됩니다. */
    private val _coreFeatureBusy = MutableStateFlow(false)
    val coreFeatureBusy: StateFlow<Boolean> = _coreFeatureBusy.asStateFlow()

    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    /** SDK 초기화 완료 여부 — UI가 광고 슬롯을 그릴지 판단할 때 사용합니다. */
    private val _sdkReady = MutableStateFlow(false)
    val sdkReady: StateFlow<Boolean> = _sdkReady.asStateFlow()

    // ─────────────────────── 초기화 ───────────────────────

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true

        if (!AdUnits.isConfigured) {
            Log.w(TAG, "LevelPlay not configured (APP_KEY / placements blank). Ads disabled.")
            return
        }

        try {
            IronSource.init(
                context.applicationContext,
                AdUnits.APP_KEY,
                object : InitializationListener {
                    override fun onInitializationComplete() {
                        ironSourceReady = true
                        _sdkReady.value = true
                        Log.d(TAG, "LevelPlay initialized")
                        IronSource.setLevelPlayRewardedVideoListener(rewardedListener)
                        IronSource.loadRewardedVideo()
                    }
                },
                IronSource.AD_UNIT.BANNER,
                IronSource.AD_UNIT.REWARDED_VIDEO,
                IronSource.AD_UNIT.NATIVE_AD
            )
        } catch (e: Exception) {
            Log.e(TAG, "LevelPlay init failed", e)
        }
    }

    fun setCoreFeatureBusy(busy: Boolean) {
        _coreFeatureBusy.value = busy
    }

    /** Activity 생애주기 전달 (SDK가 포그라운드/백그라운드 전환을 추적합니다) */
    fun onResume(activity: Activity) {
        pendingActivity = activity
        if (AdUnits.isConfigured) {
            try { IronSource.onResume(activity) } catch (_: Exception) { }
        }
    }

    fun onPause(activity: Activity) {
        if (AdUnits.isConfigured) {
            try { IronSource.onPause(activity) } catch (_: Exception) { }
        }
    }

    // ─────────────────────── 배너 ───────────────────────

    /**
     * 앱 하단 고정 배너. 미설정이면 null을 반환합니다.
     * 로드 실패 시 자동으로 숨겨집니다.
     */
    fun createBannerView(activity: Activity, placement: String): IronSourceBannerLayout? {
        if (!AdUnits.isConfigured || placement.isBlank()) return null
        return try {
            IronSource.createBanner(activity, ISBannerSize.BANNER).apply {
                this.placementName = placement
                setLevelPlayBannerListener(bannerListener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Banner create exception", e)
            null
        }
    }

    /**
     * 스폰서 카드. 배너보다 eCPM이 높고 시각적 방해가 적어 유틸리티 앱에 적합합니다.
     *
     * LevelPlay의 진짜 네이티브 광고는 뷰 바인더를 직접 구현해야 해서 복잡도가 높습니다.
     * 네이티브와 같은 수익률을 내면서 훨씬 안정적인 `rectangle`(MREC) 배너를 씁니다.
     */
    fun createSponsoredCardView(activity: Activity): IronSourceBannerLayout? {
        if (!AdUnits.isConfigured || AdUnits.NATIVE_PLACEMENT.isBlank()) return null
        return try {
            IronSource.createBanner(activity, ISBannerSize.RECTANGLE).apply {
                placementName = AdUnits.NATIVE_PLACEMENT
                setLevelPlayBannerListener(bannerListener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sponsored card create exception", e)
            null
        }
    }

    fun loadBanner(layout: IronSourceBannerLayout) {
        if (!AdUnits.isConfigured) return
        try { IronSource.loadBanner(layout) } catch (e: Exception) {
            Log.e(TAG, "Banner load exception", e)
        }
    }

    fun destroyBanner(layout: IronSourceBannerLayout) {
        if (!AdUnits.isConfigured) return
        try { IronSource.destroyBanner(layout) } catch (_: Exception) { }
    }

    private val bannerListener = object : LevelPlayBannerListener {
        override fun onAdLoaded(adInfo: AdInfo?) {
            Log.d(TAG, "Banner loaded")
        }

        override fun onAdLoadFailed(error: IronSourceError?) {
            Log.w(TAG, "Banner load failed: ${error?.errorCode} ${error?.errorMessage}")
        }

        override fun onAdClicked(adInfo: AdInfo?) = Unit

        override fun onAdLeftApplication(adInfo: AdInfo?) = Unit

        override fun onAdScreenPresented(adInfo: AdInfo?) = Unit

        override fun onAdScreenDismissed(adInfo: AdInfo?) = Unit
    }

    // ─────────────────────── 보상형 광고 ───────────────────────

    /**
     * 보상형 광고를 미리 로드합니다.
     * 과도한 요청을 막기 위해 실패 횟수와 재로드 쿨다운을 적용합니다.
     */
    fun loadRewarded(context: Context, force: Boolean = false) {
        if (!AdUnits.isConfigured || AdUnits.REWARDED_PLACEMENT.isBlank()) return
        if (rewardedLoaded || rewardedShowing) return
        if (rewardedFailures >= AdPolicy.MAX_LOAD_FAILURES) return
        if (!force &&
            System.currentTimeMillis() - lastRewardedShownAt < AdPolicy.REWARDED_RELOAD_COOLDOWN_MS
        ) return

        try {
            if (pendingActivity == null) pendingActivity = null
            IronSource.loadRewardedVideo()
        } catch (e: Exception) {
            rewardedFailures++
            Log.e(TAG, "Rewarded load exception", e)
        }
    }

    /** 프리로드/표시 공통 콜백 */
    private val rewardedListener = object : LevelPlayRewardedVideoListener {
        override fun onAdAvailable(adInfo: AdInfo?) {
            rewardedLoaded = true
            rewardedFailures = 0
            _rewardedReady.value = true
            Log.d(TAG, "Rewarded available")
        }

        override fun onAdUnavailable() {
            rewardedLoaded = false
            rewardedFailures++
            _rewardedReady.value = false
            Log.w(TAG, "Rewarded unavailable")
        }

        override fun onAdShowFailed(error: IronSourceError?, adInfo: AdInfo?) {
            rewardedLoaded = false
            rewardedShowing = false
            rewardedFailures++
            _rewardedReady.value = false
            Log.e(TAG, "Rewarded show failed: ${error?.errorCode} ${error?.errorMessage}")
            showCallbacks?.onDismiss?.invoke()
            showCallbacks = null
        }

        override fun onAdRewarded(placement: Placement?, adInfo: AdInfo?) {
            // 광고를 끝까지 시청했을 때만 호출됩니다.
            Log.d(TAG, "Rewarded earned: ${placement?.rewardAmount}")
            showCallbacks?.onReward?.invoke()
        }

        override fun onAdClosed(adInfo: AdInfo?) {
            rewardedLoaded = false
            rewardedShowing = false
            _rewardedReady.value = false
            showCallbacks?.onDismiss?.invoke()
            showCallbacks = null
            val act = pendingActivity
            if (act != null) loadRewarded(act.applicationContext)
        }

        override fun onAdOpened(adInfo: AdInfo?) = Unit

        override fun onAdClicked(placement: Placement?, adInfo: AdInfo?) = Unit
    }

    private class ShowCallbacks(
        val onReward: () -> Unit,
        val onDismiss: () -> Unit,
        val onNotAvailable: () -> Unit
    )

    private var showCallbacks: ShowCallbacks? = null

    /**
     * 보상형 광고를 표시합니다.
     *
     * @param onReward 광고를 끝까지 시청해 보상을 받았을 때 (진짜 재생된 경우만 호출)
     * @param onDismiss 광고가 닫혔을 때 (보상 여부와 무관하게 항상 호출)
     * @param onNotAvailable 광고를 보여줄 수 없을 때. 호출자가 피드백만 표시하고
     *        기능을 막지 않아야 합니다.
     */
    fun showRewarded(
        activity: Activity,
        onReward: () -> Unit,
        onDismiss: () -> Unit,
        onNotAvailable: () -> Unit
    ) {
        if (!AdUnits.isConfigured) {
            onNotAvailable()
            return
        }

        // 핵심 기능 수행 중이면 표시하지 않습니다
        if (_coreFeatureBusy.value) {
            onNotAvailable()
            return
        }

        // 중복 노출 방지
        if (rewardedShowing) {
            Log.w(TAG, "showRewarded: already showing")
            return
        }

        if (!IronSource.isRewardedVideoAvailable()) {
            Log.w(TAG, "showRewarded: no ad available")
            onNotAvailable()
            loadRewarded(activity.applicationContext, force = true)
            return
        }

        showCallbacks = ShowCallbacks(onReward, onDismiss, onNotAvailable)
        rewardedShowing = true
        pendingActivity = activity

        try {
            IronSource.setLevelPlayRewardedVideoListener(rewardedListener)
            IronSource.showRewardedVideo(activity, AdUnits.REWARDED_PLACEMENT)
        } catch (e: Exception) {
            Log.e(TAG, "Rewarded show exception", e)
            rewardedShowing = false
            showCallbacks = null
            onNotAvailable()
        }
    }

    // ─────────────────────── 세션 관리 ───────────────────────

    fun resetSessionCounters() {
        appOpenCountThisSession = 0
    }

    /** 테스트/디버그용: 실패 카운터 초기화 */
    fun resetFailures() {
        rewardedFailures = 0
    }
}
