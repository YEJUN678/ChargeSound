package com.example.ad

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 광고 관리 싱글턴 (Google AdMob / Google Mobile Ads SDK).
 *
 * 설계 원칙 (광고 플랫폼 정책 준수):
 * 1. **광고를 거부해도 앱의 핵심 기능은 계속 동작한다.** 어떤 실패 경로에서도
 *    프리미엄 콘텐츠가 잠긴 채로 남거나, 사운드/애니메이션 선택이 막히지 않습니다.
 * 2. 광고 로드 실패는 조용히 무시됩니다. 사용자에게 오류를 강요하지 않습니다.
 * 3. 앱 핵심 기능(충전 애니메이션/사운드 재생)에는 전면류 광고를 절대 표시하지 않습니다.
 * 4. 설정이 비어 있으면([AdUnits.isConfigured] == false) SDK 를 초기화하지 않고
 *    앱은 완전히 정상 동작합니다.
 */
object AdManager {

    private const val TAG = "AdManager"

    private var initialized = false

    /** 보상형 광고 캐시. null 이면 아직 로드되지 않은 상태입니다. */
    private var rewardedAd: RewardedAd? = null
    private var rewardedLoading = false
    private var rewardedFailures = 0
    private var rewardedShowing = false

    /** 마지막 보상형 광고 노출 시각 → 재로드 쿨다운 제어 */
    @Volatile
    private var lastRewardedShownAt = 0L

    /** 스폰서 카드에 표시 중인 네이티브 광고. 새 광고를 받을 때 destroy 합니다. */
    private var currentNativeAd: NativeAd? = null

    /** 앱 핵심 기능(충전 애니메이션/사운드 재생) 수행 중인지. 이 동안 전면류 광고는 차단됩니다. */
    private val _coreFeatureBusy = MutableStateFlow(false)
    val coreFeatureBusy: StateFlow<Boolean> = _coreFeatureBusy.asStateFlow()

    private val _rewardedReady = MutableStateFlow(false)
    val rewardedReady: StateFlow<Boolean> = _rewardedReady.asStateFlow()

    /** SDK 초기화 완료 여부 — UI 가 광고 슬롯을 그릴지 판단할 때 사용합니다. */
    private val _sdkReady = MutableStateFlow(false)
    val sdkReady: StateFlow<Boolean> = _sdkReady.asStateFlow()

    // ─────────────────────── 초기화 ───────────────────────

    fun initialize(context: Context) {
        if (initialized) return
        initialized = true

        if (!AdUnits.isConfigured) {
            Log.w(TAG, "AdMob not configured (app id / units blank). Ads disabled.")
            return
        }

        try {
            MobileAds.initialize(context.applicationContext) {
                Log.d(TAG, "AdMob initialized (test=${AdUnits.USE_TEST_ADS})")
                _sdkReady.value = true
                loadRewarded(context.applicationContext)
            }
        } catch (e: Exception) {
            Log.e(TAG, "AdMob init failed", e)
        }
    }

    fun setCoreFeatureBusy(busy: Boolean) {
        _coreFeatureBusy.value = busy
    }

    /**
     * AdMob 은 ironSource 와 달리 Activity 생애주기 전달이 필요 없습니다.
     * 호출부를 그대로 두기 위해 시그니처만 유지합니다.
     */
    fun onResume(activity: Activity) = Unit

    fun onPause(activity: Activity) = Unit

    // ─────────────────────── 배너 ───────────────────────

    /**
     * 앱 하단 고정 배너. 미설정이면 null 을 반환합니다.
     * 로드 실패 시 AdMob 이 스스로 뷰를 숨깁니다(빈 공간이 남지 않도록 높이를 0 으로).
     */
    fun createBanner(context: Context): AdView? {
        if (!AdUnits.isConfigured || AdUnits.bannerUnit.isBlank()) return null
        return try {
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdUnits.bannerUnit
                loadAd(AdRequest.Builder().build())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Banner create exception", e)
            null
        }
    }

    fun destroyBanner(view: AdView) {
        try {
            view.destroy()
        } catch (_: Exception) {
        }
    }

    // ─────────────────────── 스폰서 카드 (네이티브) ───────────────────────

    /**
     * 스폰서 카드. 네이티브 광고를 받아 80~96dp 높이의 카드로 그립니다.
     *
     * 반환값은 빈 컨테이너입니다. 광고가 도착하면 그 안에 뷰가 추가되고,
     * 네이티브 로드가 실패하면 같은 자리에 일반 배너(320x50)로 대체합니다.
     * 그래서 광고가 없을 때도 레이아웃이 깨지지 않습니다.
     */
    fun createSponsoredCard(context: Context): FrameLayout? {
        if (!AdUnits.isConfigured || AdUnits.nativeUnit.isBlank()) return null

        val container = FrameLayout(context)
        try {
            AdLoader.Builder(context, AdUnits.nativeUnit)
                .forNativeAd { ad ->
                    currentNativeAd?.destroy()
                    currentNativeAd = ad
                    try {
                        container.removeAllViews()
                        container.addView(
                            buildNativeCard(context, ad),
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.WRAP_CONTENT
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "Native render failed", e)
                    }
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Native load failed: ${error.code} ${error.message}")
                        if (container.childCount > 0) return
                        // 폴백: 네이티브가 안 되면 일반 배너로 채웁니다.
                        val fallback = createBanner(context)
                        if (fallback != null) {
                            container.addView(
                                fallback,
                                FrameLayout.LayoutParams(
                                    FrameLayout.LayoutParams.WRAP_CONTENT,
                                    FrameLayout.LayoutParams.WRAP_CONTENT,
                                    Gravity.CENTER_HORIZONTAL
                                )
                            )
                        }
                    }
                })
                .build()
                .loadAd(AdRequest.Builder().build())
        } catch (e: Exception) {
            Log.e(TAG, "Sponsored card create exception", e)
            return null
        }
        return container
    }

    /** 네이티브 광고를 카드 레이아웃으로 조립합니다. */
    private fun buildNativeCard(context: Context, ad: NativeAd): View {
        val density = context.resources.displayMetrics.density
        fun px(value: Int): Int = (value * density).toInt()

        val nativeAdView = NativeAdView(context)

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(px(12), px(8), px(12), px(8))
        }

        val icon = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(px(40), px(40))
            scaleType = ImageView.ScaleType.FIT_CENTER
        }
        val iconDrawable = ad.icon?.drawable
        if (iconDrawable != null) {
            icon.setImageDrawable(iconDrawable)
        } else {
            icon.visibility = View.GONE
        }

        val headline = TextView(context).apply {
            text = ad.headline
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 13f
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
        }

        val body = TextView(context).apply {
            val bodyText = ad.body
            if (bodyText.isNullOrBlank()) {
                visibility = View.GONE
            } else {
                text = bodyText
                setTextColor(0xFF94A3B8.toInt())
                textSize = 10f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            }
        }

        val callToAction = TextView(context).apply {
            text = ad.callToAction ?: "자세히 보기"
            setTextColor(0xFF00E5FF.toInt())
            textSize = 11f
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
        }

        val column = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(px(10), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        column.addView(headline)
        column.addView(body)
        column.addView(callToAction)

        row.addView(icon)
        row.addView(column)
        nativeAdView.addView(row)

        // AdMob 은 이 네 개의 자산 등록을 요구합니다. 빠지면 광고가 무효 처리되어
        // 클릭이 동작하지 않습니다.
        nativeAdView.headlineView = headline
        nativeAdView.bodyView = body
        nativeAdView.iconView = icon
        nativeAdView.callToActionView = callToAction

        nativeAdView.setNativeAd(ad)
        return nativeAdView
    }

    /** 스폰서 카드가 화면에서 사라질 때 네이티브 광고 자원을 해제합니다. */
    fun destroySponsoredCard() {
        try {
            currentNativeAd?.destroy()
        } catch (_: Exception) {
        }
        currentNativeAd = null
    }

    // ─────────────────────── 보상형 광고 ───────────────────────

    /**
     * 보상형 광고를 미리 로드합니다.
     * 과도한 요청을 막기 위해 실패 횟수와 재로드 쿨다운을 적용합니다.
     */
    fun loadRewarded(context: Context, force: Boolean = false) {
        if (!AdUnits.isConfigured || AdUnits.rewardedUnit.isBlank()) return
        if (rewardedAd != null || rewardedLoading || rewardedShowing) return
        if (rewardedFailures >= AdPolicy.MAX_LOAD_FAILURES) return
        if (!force &&
            System.currentTimeMillis() - lastRewardedShownAt < AdPolicy.REWARDED_RELOAD_COOLDOWN_MS
        ) return

        rewardedLoading = true
        try {
            RewardedAd.load(
                context.applicationContext,
                AdUnits.rewardedUnit,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedLoading = false
                        rewardedAd = ad
                        rewardedFailures = 0
                        _rewardedReady.value = true
                        Log.d(TAG, "Rewarded loaded")
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedLoading = false
                        rewardedAd = null
                        rewardedFailures++
                        _rewardedReady.value = false
                        Log.w(TAG, "Rewarded load failed: ${error.code} ${error.message}")
                    }
                }
            )
        } catch (e: Exception) {
            rewardedLoading = false
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

        val ad = rewardedAd
        if (ad == null) {
            Log.w(TAG, "showRewarded: no ad available")
            onNotAvailable()
            loadRewarded(activity.applicationContext, force = true)
            return
        }

        rewardedShowing = true
        var earned = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded shown")
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Rewarded show failed: ${error.code} ${error.message}")
                rewardedShowing = false
                rewardedAd = null
                _rewardedReady.value = false
                onNotAvailable()
            }

            override fun onAdDismissedFullScreenContent() {
                rewardedShowing = false
                rewardedAd = null
                _rewardedReady.value = false
                lastRewardedShownAt = System.currentTimeMillis()
                // 보상을 먼저 지급하고, 닫힘 콜백을 마지막에 호출합니다.
                if (earned) onReward()
                onDismiss()
                loadRewarded(activity.applicationContext)
            }
        }

        try {
            ad.show(activity) { earned = true }
        } catch (e: Exception) {
            Log.e(TAG, "Rewarded show exception", e)
            rewardedShowing = false
            rewardedAd = null
            _rewardedReady.value = false
            onNotAvailable()
        }
    }

    // ─────────────────────── 세션 관리 ───────────────────────

    /** 테스트/디버그용: 실패 카운터 초기화. */
    fun resetFailures() {
        rewardedFailures = 0
    }
}
