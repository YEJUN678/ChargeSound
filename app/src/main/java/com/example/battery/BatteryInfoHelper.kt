package com.example.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.annotation.StringRes
import com.example.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * 배터리 스냅샷.
 *
 * 표시용 텍스트는 컴포저블 밖에서 만들어지므로 문자열이 아니라 리소스 ID로
 * 들고 있습니다. UI는 `stringResource(info.statusRes)` 로 해석합니다.
 */
data class BatteryInfo(
    val level: Int = 0,
    val isCharging: Boolean = false,
    val plugType: PlugType = PlugType.NONE,
    @StringRes val statusRes: Int = R.string.battery_status_unknown,
    val temperatureC: Float = 0f,
    val voltageV: Float = 0f,
    /** 시스템이 제공하는 순간 충전 전류(mA). 기기 미지원 시 0. */
    val chargeCurrentMa: Float = 0f,
    /** 현재 charge counter에서 계산한 추정 전체 용량(mAh). 기기 미지원 시 0. */
    val estimatedCapacityMah: Float = 0f,
    @StringRes val healthRes: Int = R.string.battery_health_good,
    val technology: String = "Li-ion"
)

enum class PlugType(@StringRes val displayNameRes: Int) {
    AC(R.string.plug_ac),
    USB(R.string.plug_usb),
    WIRELESS(R.string.plug_wireless),
    NONE(R.string.plug_none)
}

object BatteryInfoHelper {

    fun getBatteryInfo(context: Context): BatteryInfo {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter) ?: return BatteryInfo()
        return parseBatteryIntent(context, intent)
    }

    fun batteryFlow(context: Context): Flow<BatteryInfo> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let {
                    trySend(parseBatteryIntent(context, it))
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)

        // Emit initial value
        trySend(getBatteryInfo(context))

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore if already unregistered
            }
        }
    }

    private fun parseBatteryIntent(context: Context, intent: Intent): BatteryInfo {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val statusRes = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> R.string.battery_status_charging
            BatteryManager.BATTERY_STATUS_FULL -> R.string.battery_status_full
            BatteryManager.BATTERY_STATUS_DISCHARGING -> R.string.battery_status_discharging
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> R.string.battery_status_not_charging
            else -> R.string.battery_status_standby
        }

        val chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val plugType = when (chargePlug) {
            BatteryManager.BATTERY_PLUGGED_AC -> PlugType.AC
            BatteryManager.BATTERY_PLUGGED_USB -> PlugType.USB
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> PlugType.WIRELESS
            else -> PlugType.NONE
        }

        val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val temperatureC = tempRaw / 10.0f

        val voltRaw = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val voltageV = voltRaw / 1000.0f

        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val healthRes = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> R.string.battery_health_good
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> R.string.battery_health_overheat
            BatteryManager.BATTERY_HEALTH_DEAD -> R.string.battery_health_dead
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> R.string.battery_health_overvoltage
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> R.string.battery_health_fail
            BatteryManager.BATTERY_HEALTH_COLD -> R.string.battery_health_cold
            else -> R.string.battery_health_ok
        }

        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"
        // CURRENT_NOW/CHARGE_COUNTER는 모든 제조사가 제공하진 않습니다. 값이 없으면
        // 0으로 두고, ETA는 세션·충전 기록 방식으로 자연스럽게 넘어갑니다.
        val manager = context.getSystemService(BatteryManager::class.java)
        val currentRaw = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)?.toLong() ?: 0L
        val currentMa = kotlin.math.abs(currentRaw).toFloat() / 1_000f
        val chargeMah = (manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: 0) / 1_000f
        val capacityMah = if (batteryPct > 0 && chargeMah > 0f) chargeMah * 100f / batteryPct else 0f

        return BatteryInfo(
            level = batteryPct,
            isCharging = isCharging,
            plugType = plugType,
            statusRes = statusRes,
            temperatureC = temperatureC,
            voltageV = voltageV,
            chargeCurrentMa = currentMa,
            estimatedCapacityMah = capacityMah,
            healthRes = healthRes,
            technology = technology
        )
    }
}
