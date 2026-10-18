package com.example.battery

import com.example.data.ChargeRecord

/**
 * 배터리 수명을 좌우하는 실제 요인에 대한 인사이트.
 *
 * ## 왜 SOH (%) 를 보여주지 않는가
 * 이 앱은 "배터리 건강도 92%" 같은 숫자를 **의도적으로 내지 않습니다.**
 *
 * 이유는 기술적으로 명확합니다:
 *
 * 1. Android 에는 **만충 용량**을 읽는 공개 API 가 없습니다.
 * 2. 공장 설계 용량([BatteryManager.BATTERY_PROPERTY_DESIGN_CAPACITY])은
 *    AOSP 에서 `@SystemApi` — 공개 SDK 에 노출되지 않으므로 서드파티 앱이
 *    컴파일조차 할 수 없습니다. 리플렉션으로 파고들면 Android 9+ 의 숨김 API
 *    제한에 걸리고, 스토어 정책상 문제가 됩니다.
 * 3. 그래서业界(AccuBattery 등)가 쓰는 우회법은 "만충에서 CHARGE_COUNTER 를
 *    만충 용량으로 간주"인데, 여기에도 설계 용량이 없으니 **비교 대상이** 없습니다.
 *
 * 일부 앱은 앱을 처음 켰을 때의 만충 용량을 "설계 용량"으로 학습해 SOH 를 계산합니다.
 * 여기서도 하지 않았습니다 — **배터리가 이미 닳은 상태에서 첫 실행하면 그
 * 닳은 값이 기준선이 되고, 이후 실제 노후는 영영 잡히지 않습니다.**
 * 사용자가 가장 신뢰하는 숫자가 가장 속는 숫자가 되는 셈입니다.
 *
 * ## 대신 무엇을 보여주는가
 * 아래 지표들은 전부 **앱이 이미 직접 기록한 값**에서 나옵니다.
 * 숨겨진 API 도 추정도 없습니다.
 *
 * - **온도**: 배터리가 스스로 올리는 값. 리튬 전지 수명의 1등 영향 요소.
 * - **충전 종료 잔량**: 100% 에 오래 머무는 것이 왜 수명을 갉아먹는지 알려 주는 근거.
 * - **충전 시작 잔량**: 0%까지 방전 후 다시 올리는 습관.
 * - **충전 시간대**: 밤새 꽂아두는 습관.
 */
object BatteryInsights {

    // ── 온도 추이 ──────────────────────────────────────────────────

    /** 온도 추세 판정에 필요한 최소 표본 수. */
    const val MIN_TEMP_SAMPLES = 6

    /**
     * 이 온도 이상 차이를 "오르는 중" 으로 봅니다.
     *
     * 센서 자체 오차가 ±1℃ 수준이고 계절·실내 온도가 개입합니다.
     * 0.2℃ 차이를 추세라 말하면 노이즈를 신호로 팔게 됩니다.
     */
    const val RISING_THRESHOLD_C = 1.0f

    /**
     * 리튬 이온 전지의 안전 상한.
     * 40℃ 를 넘으면 지속 시 열폭주 위험이 급격히 커집니다.
     */
    const val HOT_THRESHOLD_C = 40f

    /** 저온 하한. 이 아래로 자주 내려가면 리튬 이온이 석출되며 손상됩니다. */
    const val COOL_THRESHOLD_C = 10f

    enum class Direction { RISING, FALLING, STABLE, UNKNOWN }

    enum class Zone { COOL, WARM, HOT }

    data class TemperatureTrend(
        /** 전체 평균 최고 온도 (℃) */
        val averageC: Float,
        /** 최근 절반의 평균 (℃) */
        val recentAverageC: Float,
        /** 오래된 절반의 평균 (℃) */
        val olderAverageC: Float,
        /** 최근 − 오래됨. 양수면 최근일수록 더 뜨겁습니다. */
        val deltaC: Float,
        val direction: Direction,
        /** 최고 온도가 [HOT_THRESHOLD_C] 를 넘긴 세션 비율 (0f~1f) */
        val hotRatio: Float
    ) {
        val isRising: Boolean get() = direction == Direction.RISING
    }

    /**
     * 온도 추이를 계산합니다.
     *
     * 표본이 [MIN_TEMP_SAMPLES] 개 미만이면 방향을 판정하지 않습니다.
     * 2건으로 "오르는 중" 이라 말하면 통계가 아니라 점 하나에 코멘트를 단 것입니다.
     */
    fun temperatureTrend(records: List<ChargeRecord>): TemperatureTrend? {
        // 온도가 기록되지 않은 세션은 잡음이므로 제외합니다.
        val samples = records
            .filter { it.peakTempC > 0f && it.peakTempC < 80f }
            .sortedBy { it.startedAt }

        if (samples.isEmpty()) return null

        val average = samples.map { it.peakTempC }.average().toFloat()
        val recentCount = (samples.size / 2).coerceAtLeast(1)
        val olderCount = (samples.size - recentCount).coerceAtLeast(1)

        val recent = samples.takeLast(recentCount).map { it.peakTempC }.average().toFloat()
        val older = samples.take(olderCount).map { it.peakTempC }.average().toFloat()
        val delta = recent - older

        return TemperatureTrend(
            averageC = average,
            recentAverageC = recent,
            olderAverageC = older,
            deltaC = delta,
            direction = when {
                samples.size < MIN_TEMP_SAMPLES -> Direction.UNKNOWN
                delta >= RISING_THRESHOLD_C -> Direction.RISING
                delta <= -RISING_THRESHOLD_C -> Direction.FALLING
                else -> Direction.STABLE
            },
            hotRatio = samples.count { it.peakTempC >= HOT_THRESHOLD_C } / samples.size.toFloat()
        )
    }

    fun zoneOf(peakTempC: Float): Zone = when {
        peakTempC >= HOT_THRESHOLD_C -> Zone.HOT
        peakTempC >= 28f -> Zone.WARM
        else -> Zone.COOL
    }

    // ── 충전 습관 ───────────────────────────────────────────────────

    /**
     * 이만큼 100% 에 머물렀으면 "만충 유지" 습관으로 봅니다.
     *
     * 리튬 전지는 만충 상태를 오래 유지할수록 손상이 빨라집니다.
     * 만충 근처(95%+) 에서 끝난 세션의 비율이 가장 직접적인 지표입니다.
     */
    const val FULL_THRESHOLD = 95

    /** 이 아래까지 방전했다면 "방전 후 충전" 습관입니다. 리튬에는 부담입니다. */
    const val DEEP_DISCHARGE_THRESHOLD = 15

    data class Habits(
        val sessionCount: Int,
        /** 평균 충전 시작 잔량 (%) */
        val avgStartLevel: Int,
        /** 만충 근처에서 끝난 세션 비율 (0f~1f) */
        val fullChargeRatio: Float,
        /** 15% 이하까지 방전한 뒤 충전한 세션 비율 (0f~1f) */
        val deepDischargeRatio: Float,
        /** 밤 10시~오전 7시에 시작한 세션 비율 (0f~1f) */
        val overnightRatio: Float,
        /** 충전기가 꽂혀 있던 총 시간 (분) */
        val totalMinutes: Long
    ) {
        /** 가장 먼저 고칠 것 하나를 고릅니다. UI 는 이것만 강조합니다. */
        fun primaryConcern(): Concern? = when {
            sessionCount < MIN_HABIT_SAMPLES -> null
            fullChargeRatio >= 0.7f -> Concern.PINNED_AT_FULL
            deepDischargeRatio >= 0.3f -> Concern.DEEP_DISCHARGE
            overnightRatio >= 0.4f -> Concern.PLUGGED_OVERNIGHT
            else -> Concern.NONE
        }
    }

    enum class Concern {
        /** 100% 에 너무 오래 꽂아둡니다 */
        PINNED_AT_FULL,

        /** 0% 까지 방전하고 다시 충전합니다 */
        DEEP_DISCHARGE,

        /** 밤새 충전기에 꽂아둡니다 */
        PLUGGED_OVERNIGHT,

        NONE
    }

    /** 습관을 판정할 최소 세션 수 */
    const val MIN_HABIT_SAMPLES = 5

    fun habits(records: List<ChargeRecord>): Habits? {
        if (records.isEmpty()) return null

        return Habits(
            sessionCount = records.size,
            avgStartLevel = records.map { it.startLevel }.average().toInt(),
            fullChargeRatio = records.count { it.endLevel >= FULL_THRESHOLD } / records.size.toFloat(),
            deepDischargeRatio = records.count { it.startLevel <= DEEP_DISCHARGE_THRESHOLD } /
                    records.size.toFloat(),
            overnightRatio = records.count { isOvernight(it.startedAt) } / records.size.toFloat(),
            totalMinutes = records.sumOf { it.durationMillis } / 60_000L
        )
    }

    /** 밤 22:00 ~ 오전 7:00 사이에 시작한 충전인가. */
    private fun isOvernight(startedAt: Long): Boolean {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = startedAt }
        val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
        return hour >= 22 || hour < 7
    }
}