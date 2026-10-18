package com.example.battery

import android.content.Context
import android.os.BatteryManager
import android.util.Log
import kotlin.math.abs

/**
 * 배터리의 "지금 몇 W 들어오고 있나" 를 잰다.
 *
 * ## 무엇을 재는가
 * [BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER] 는 배터리 안으로 들어간
 * 에너지를 **나노와트시(nWh)** 로 돌려줍니다. 이 값을 시간차 두 번 읽고 미분하면
 * 그 사이의 평균 소비/충전 전력(와트)이 됩니다.
 *
 * ```
 * W = ΔnWh / Δms × 3.6e-3
 * ```
 *
 * (1 nWh = 3.6×10⁻⁶ J 이므로, 밀리초당 nWh 를 3.6e-3 으로 곱하면 W 가 됩니다.
 *  18W 로 1초 동안 들어온 양 = 5,000,000 nWh → 5000 × 3.6e-3 = 18 ✓)
 *
 * ## 왜 전류(A) 대신 에너지(Wh)를 쓰는가
 * [BatteryManager.BATTERY_PROPERTY_CURRENT_NOW] 도 읽을 수 있지만 값이
 * 지나치게 요동칩니다. 충전 중인데 음수가 나오는 기기도 있고, 화면 밝기에 따라
 * 수십 mA 가 흔들립니다. 사용자가 "몇 W 인가" 를 물었는데 매 순간 답이 달라지면
 * 그건 답이 아니라 잡음입니다.
 *
 * ## 대부분의 기기는 값을 안 줍니다
 * 일부 기기(삼성 일부 모델)가 에너지원을 노출하고, 나머지는 0 을 돌려줍니다.
 * 그래서 **측정 못 하면 null 을 돌려줍니다.** "0W" 라고 표시하면 사용자가
 * "충전기가 안 물고 있구나" 라는 잘못된 결론을 내리게 됩니다.
 *
 * 샘플 간격을 기억해야 하므로 프로세스 단위로 하나만 두어야 합니다. 그래서 [get] 으로
 * 인스턴스를 나눕니다.
 */
class PowerTracker private constructor(appContext: Context) {

    companion object {
        private const val TAG = "PowerTracker"

        /** BatteryManager 가 지원하지 않는 값을 돌려줄 때 쓰는 표식 */
        private const val UNKNOWN = -1L

        /**
         * 이보다 짧은 간격은 측정 신뢰할 수 없습니다.
         *
         * 배터리 펌프(전원 관리 IC)가 수백 ms ~ 초 단위로 값을 갱신합니다.
         * 그보다 자주 읽으면 두 값이 같은 펌프 값이라 차이가 0 이거나,
         * 계측 오차만 증폭됩니다.
         */
        const val MIN_SAMPLE_INTERVAL_MS = 5_000L

        /**
         * 이 위쪽은 계측 오류로 봅니다.
         *
         * 대체 충전기는 100W 급이 나오지만 배터리 *안으로 들어가는* 값이
         * 그 정도는 되지 않습니다. 나왔으면 계측 이상입니다.
         */
        const val PLAUSIBLE_MAX_WATTS = 100f

        /** 이 이하는 사실상 "측정 불가"입니다. 충전 중인데 이 값이면 배터리를 안 타고 있습니다. */
        private const val PLAUSIBLE_MIN_WATTS = 0.5f

        /** 지수 이동 평균 계수. 1 에 가까울수록 현재 값에 빠르게 반응합니다. */
        private const val EMA_ALPHA = 0.35f

        @Volatile
        private var instance: PowerTracker? = null

        fun get(context: Context): PowerTracker =
            instance ?: synchronized(this) {
                instance ?: PowerTracker(context.applicationContext).also { instance = it }
            }

        /**
         * 이 기기가 에너지원을 노출하는지 **한 번만** 확인합니다.
         *
         * 안 되면 몇 번을 재도 0 이므로, 호출한 쪽은 폴링을 아예 멈춰야 합니다.
         * 배터리를 쓸데없이 낭비할 이유가 없기 때문입니다.
         */
        fun isSupported(context: Context): Boolean {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                ?: return false
            return try {
                bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER) > 0L
            } catch (e: Exception) {
                false
            }
        }
    }

    private val battery: BatteryManager? =
        appContext.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager

    private var lastEnergyNwh: Long = UNKNOWN
    private var lastAtMillis: Long = 0L

    /** 직전 측정값을 이평한 값. 화면에 최종적으로 보여줄 숫자입니다. */
    @Volatile
    private var smoothed: Float = Float.NaN

    /**
     * 현재 배터리 전력을 읽습니다.
     *
     * @param nowMillis 측정 시각.
     * @return 와트. 측정할 수 없으면 **null** (0 이 아닙니다).
     */
    @Synchronized
    fun sample(nowMillis: Long = System.currentTimeMillis()): Float? {
        val bm = battery ?: return null
        return try {
            val energy = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            if (energy <= 0L) {
                // 이 기기는 아예 지원 안 함. 이전 샘플을 비워야 프로세스가 살아남는
                // 동안 다른 기기로 전환됐을 때 옛 값이 새 기기에 붙지 않습니다.
                reset()
                return null
            }

            val prevEnergy = lastEnergyNwh
            val prevAt = lastAtMillis
            lastEnergyNwh = energy
            lastAtMillis = nowMillis

            // 첫 샘플 — 비교 대상이 없습니다.
            if (prevEnergy <= 0L || prevAt <= 0L) return null

            val dtMs = nowMillis - prevAt
            if (dtMs < MIN_SAMPLE_INTERVAL_MS) return null

            // 에너지가 늘었으면 충전, 줄었으면 방전. 부호를 그대로 씁니다.
            val watts = ((energy - prevEnergy).toDouble() / dtMs * 3.6e-3).toFloat()
            if (!isPlausible(watts)) return null

            val next = if (smoothed.isNaN()) watts else smoothed + (watts - smoothed) * EMA_ALPHA
            smoothed = next
            next
        } catch (e: Exception) {
            Log.w(TAG, "sample failed", e)
            reset()
            null
        }
    }

    /** 물리적으로 말이 되는 값인지. 방전 중 음수여도 통과합니다. */
    fun isPlausible(watts: Float): Boolean =
        watts.isFinite() && abs(watts) <= PLAUSIBLE_MAX_WATTS

    /** 충전 중인데 사실상 0 인 값은 방전으로 오인되는 노이즈입니다. */
    fun isMeaningful(watts: Float): Boolean =
        isPlausible(watts) && abs(watts) >= PLAUSIBLE_MIN_WATTS

    /** 한 번이라도 유효한 값을 얻었는지. */
    val hasEverMeasured: Boolean get() = !smoothed.isNaN()

    /** 상태를 비웁니다. 충전기 분리 시, 측정 초기화 시. */
    @Synchronized
    fun reset() {
        lastEnergyNwh = UNKNOWN
        lastAtMillis = 0L
        smoothed = Float.NaN
    }
}