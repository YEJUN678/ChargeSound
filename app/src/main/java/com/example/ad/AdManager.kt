package com.example.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AdMob 광고 관리 싱글턴.
 *
 * 설계 원칙 (AdMob 정책 준수):
 * 1. **광고를 거부해도 앱의 핵심 기능은 계속 동작한다.** 어떤 실패 경로에서도
 *    프리미엄 콘텐츠 잠금이 풀린 채로 남거나, 사운드/애니메이션 선택이 막히지 않습니다.
 * 2. 광고 로드 실패는 조용히 무시됩니다. 사용자에게 오류를 강요하지 않습니다.
 * 3. 앱 핵심 기능(충전 애니메이션 재생 중)에는 전면/앱오픈 광고를 절대 표시하지 않습니다.
 */
object AdManager {

    private const val TAG = "AdManager"

    private var initialized = false

    private var rewardedAd: RewardedAd? = null
    private var appOpenAd: AppOpenAd? = null
    private var rewardedFailures = 0

    private var lastAppOpenShownAt = 0L
    private var appOpenCountThisSession = 0

    /** 앱 핵심 기능(충전 애니메이션/사운드 재생) 수행 중인지. 이 동안 전면류 광고는 차단됩니다. */
    private val _coreFeatureBusy = MutableStateFlow(false)
    val coreFeatureBusy: StateFlow<Boolean> = _coreFeatureBusy.asStateFlow()

    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    /** 마지막 광고 노출 성공 시각 → "방금 광고를 봤어요" 노출 빈도 제어에 사용 */
    @Volatile
    private var lastRewardedShownAt = 0L

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        try {
            com.google.android.gms.ads.MobileAds.initialize(context) {
                Log.d(TAG, "MobileAds initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "MobileAds init failed", e)
        }
    }

    fun setCoreFeatureBusy(busy: Boolean) {
        _coreFeatureBusy.value = busy
    }

    private fun buildRequest(): AdRequest = AdRequest.Builder().build()

    // ─────────────────────── 보상형 광고 ───────────────────────

    /**
     * 보상형 광고를 미리 로드합니다.
     * 재시도 간격을 두어 과도한 요청을 막습니다.
     */
    fun loadRewarded(context: Context, force: Boolean = false) {
        if (rewardedAd != null) return
        if (rewardedFailures >= AdPolicy.MAX_LOAD_FAILURES) return
        if (!force && System.currentTimeMillis() - lastRewardedShownAt < 60_000L) return

        try {
            RewardedAd.load(
                context,
                AdUnits.rewardedId,
                buildRequest(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        rewardedFailures = 0
                        _rewardedReady.value = true
                        Log.d(TAG, "Rewarded ad loaded")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedFailures++
                        rewardedAd = null
                        _rewardedReady.value = false
                        Log.w(TAG, "Rewarded load failed: ${error.code} ${error.message}")
                    }
                }
            )
        } catch (e: Exception) {
            rewardedFailures++
            Log.e(TAG, "Rewarded load exception", e)
        }
    }

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
        val ad = rewardedAd
        if (ad == null) {
            Log.w(TAG, "showRewarded called with no cached ad")
            onNotAvailable()
            // 재시도 준비
            loadRewarded(activity.applicationContext, force = true)
            return
        }

        // 핵심 기능 수행 중이면 표시하지 않습니다
        if (_coreFeatureBusy.value) {
            onNotAvailable()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewarded(activity.applicationContext)
                onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Rewarded show failed: ${error.code} ${error.message}")
                rewardedAd = null
                loadRewarded(activity.applicationContext, force = true)
                // 표시 실패는 보상 지급이 아니므로 onReward를 호출하지 않습니다.
                onDismiss()
            }
        }

        try {
            lastRewardedShownAt = System.currentTimeMillis()
            // onUserEarnedReward는 광고를 끝까지 시청했을 때만 호출됩니다.
            ad.show(activity) {
                onReward()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Rewarded show exception", e)
            rewardedAd = null
            onNotAvailable()
        }
    }

    // ─────────────────────── 앱 오픈 광고 ───────────────────────

    fun loadAppOpen(context: Context) {
        try {
            AppOpenAd.load(
                context,
                AdUnits.appOpenId,
                buildRequest(),
                object : AppOpenAd.AppOpenAdLoadCallback() {
                    override fun onAdLoaded(ad: AppOpenAd) {
                        appOpenAd = ad
                        Log.d(TAG, "AppOpen ad loaded")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        appOpenAd = null
                        Log.w(TAG, "AppOpen load failed: ${error.code}")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "AppOpen load exception", e)
        }
    }

    /**
     * 앱 오픈 광고 표시 여부를 판단하고, 조건을 만족할 때만 표시합니다.
     *
     * 표시 조건:
     * - 광고가 로드되어 있고
     * - 마지막 표시로부터 [AdPolicy.APP_OPEN_MIN_INTERVAL_MS] 이상 지났고
     * - 이번 세션에서 아직 표시하지 않았고
     * - 앱 핵심 기능(충전 애니메이션 등) 수행 중이 아니고
     * - 광고 없는 시간이 종료되지 않았어야 함
     */
    fun maybeShowAppOpen(
        activity: Activity,
        adFreeUntil: Long,
        onDismissed: () -> Unit
    ) {
        val ad = appOpenAd
        if (ad == null) {
            onDismissed()
            return
        }
        val now = System.currentTimeMillis()
        if (now < adFreeUntil) {
            onDismissed()
            return
        }
        if (_coreFeatureBusy.value) {
            onDismissed()
            return
        }
        if (appOpenCountThisSession >= AdPolicy.APP_OPEN_MAX_PER_SESSION) {
            onDismissed()
            return
        }
        if (now - lastAppOpenShownAt < AdPolicy.APP_OPEN_MIN_INTERVAL_MS) {
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                appOpenCountThisSession++
                lastAppOpenShownAt = System.currentTimeMillis()
                onDismissed()
                loadAppOpen(activity.applicationContext)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "AppOpen show failed: ${error.code}")
                appOpenAd = null
                onDismissed()
            }
        }

        try {
            ad.show(activity)
        } catch (e: Exception) {
            Log.e(TAG, "AppOpen show exception", e)
            appOpenAd = null
            onDismissed()
        }
    }

    fun resetSessionCounters() {
        appOpenCountThisSession = 0
    }

    /** 테스트/디버그용: 실패 카운터 초기화 */
    fun resetFailures() {
        rewardedFailures = 0
    }
}