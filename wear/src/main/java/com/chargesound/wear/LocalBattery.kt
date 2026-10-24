package com.chargesound.wear

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 이 기기(워치 또는 휴대폰)의 배터리를 직접 읽습니다.
 *
 * ## 왜 필요한가
 * 이 앱은 원래 "휴대폰 배터리를 워치에서 본다" 는 동반 앱입니다. 그런데
 * Galaxy Store 는 워치 앱을 중국에서만 판매하도록 막고 있어서, 국내 판매를
 * 하려면 일반 Android 앱으로 등록해야 합니다. 그러면 휴대폰에도 설치되므로
 * 휴대폰에서도 의미 있는 화면을 보여줘야 합니다.
 *
 * 그래서 휴대폰 앱과 연결되지 않았을 때는 **이 기기의 배터리**를 보여줍니다.
 * 워치에서 실행하면 워치 배터리, 휴대폰에서 실행하면 휴대폰 배터리입니다.
 *
 * ## 와트 계산
 * 휴대폰 앱의 PowerTracker 와 같은 방식(에너지 카운터 미분)을 씁니다.
 * 첫 호출에는 비교 대상이 없어 null 이고, 30초 주기 갱신에서 값이 나옵니다.
 */
object LocalBattery {

    private const val TAG = "LocalBattery"

    /** 이보다 짧은 간격은 계측 오차만 커집니다. */
    private const val MIN_SAMPLE_INTERVAL_MS = 5_000L

    /** 배터리 안으로 들어가는 전력이 이보다 크면 계측 오류로 봅니다. */
    private const val MAX_PLAUSIBLE_WATTS = 100f

    private var lastEnergyNwh = -1L
    private var lastAtMs = 0L

    fun read(context: Context): PhoneSnapshot? {
        return try {
            val batteryIntent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val percent = if (level >= 0 && scale > 0) level * 100 / scale else 0

            val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

            // EXTRA_TEMPERATURE 는 섭씨 x10 입니다.
            val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val tempC = (tempTenths / 10f).roundToInt()

            PhoneSnapshot(
                level = percent,
                charging = charging,
                watts = sampleWatts(context),
                tempC = tempC,
                serviceEnabled = false,
                updatedAt = System.currentTimeMillis(),
                isLocal = true
            )
        } catch (e: Exception) {
            Log.w(TAG, "read failed", e)
            null
        }
    }

    private fun sampleWatts(context: Context): Float? {
        return try {
            val battery = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                ?: return null
            val energy = battery.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
            if (energy <= 0L) return null

            val now = System.currentTimeMillis()
            val prevEnergy = lastEnergyNwh
            val prevAt = lastAtMs
            lastEnergyNwh = energy
            lastAtMs = now

            if (prevEnergy <= 0L || prevAt <= 0L) return null
            val dt = now - prevAt
            if (dt < MIN_SAMPLE_INTERVAL_MS) return null

            val watts = ((energy - prevEnergy).toDouble() / dt * 3.6e-3).toFloat()
            if (!watts.isFinite() || abs(watts) > MAX_PLAUSIBLE_WATTS || watts < 0.5f) null
            else watts
        } catch (e: Exception) {
            null
        }
    }
}
