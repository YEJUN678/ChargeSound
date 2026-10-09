package com.example.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class BatteryInfo(
    val level: Int = 0,
    val isCharging: Boolean = false,
    val plugType: PlugType = PlugType.NONE,
    val statusText: String = "알 수 없음",
    val temperatureC: Float = 0f,
    val voltageV: Float = 0f,
    val healthText: String = "양호",
    val technology: String = "Li-ion"
)

enum class PlugType(val displayName: String) {
    AC("고속 유선 충전 (AC)"),
    USB("USB 포트 연결"),
    WIRELESS("무선 충전 (Wireless)"),
    NONE("배터리 방전 중 (연결 안 됨)")
}

object BatteryInfoHelper {

    fun getBatteryInfo(context: Context): BatteryInfo {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter) ?: return BatteryInfo()
        return parseBatteryIntent(intent)
    }

    fun batteryFlow(context: Context): Flow<BatteryInfo> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent?.let {
                    trySend(parseBatteryIntent(it))
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

    private fun parseBatteryIntent(intent: Intent): BatteryInfo {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val statusText = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "충전 진행 중"
            BatteryManager.BATTERY_STATUS_FULL -> "충전 완료 (100%)"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "배터리 사용 중"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "충전 중 아님"
            else -> "대기 중"
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
        val healthText = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "양호 (정상)"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "과열 주의"
            BatteryManager.BATTERY_HEALTH_DEAD -> "수명 종료"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "과전압 주의"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "점검 필요"
            BatteryManager.BATTERY_HEALTH_COLD -> "저온 주의"
            else -> "정상"
        }

        val technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Li-ion"

        return BatteryInfo(
            level = batteryPct,
            isCharging = isCharging,
            plugType = plugType,
            statusText = statusText,
            temperatureC = temperatureC,
            voltageV = voltageV,
            healthText = healthText,
            technology = technology
        )
    }
}
