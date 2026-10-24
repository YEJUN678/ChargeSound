package com.example.wear

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.example.battery.BatteryInfoHelper
import com.example.battery.PowerTracker
import com.example.data.SettingsRepository
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 휴대폰 -> 워치 상태 전송.
 *
 * 워치는 휴대폰 배터리를 직접 읽을 수 없으므로, 휴대폰이 주기적으로
 * DataItem 을 밀어 넣습니다. DataItem 은 저장되므로 워치가 꺼졌다 켜져도
 * 마지막 값이 남습니다.
 *
 * ## 왜 Message 가 아니라 DataItem 인가
 * Message 는 워치가 켜져 있을 때만 도달합니다. 사용자가 워치를 차고 있지
 * 않다가 나중에 차는 경우가 흔하므로 "마지막으로 알려진 상태" 를 남겨야 합니다.
 *
 * ## 전송 주기
 * 배터리 브로드캐스트는 충전 중 1%마다, 그리고 수시로 발생합니다. 매번
 * 보내면 블루투스 라디오를 계속 깨우므로 [MIN_INTERVAL_MS] 로 제한합니다.
 * 충전기 연결/해제처럼 사용자가 즉시 봐야 하는 변화는 force 로 보냅니다.
 */
object PhoneWearSync {

    private const val TAG = "PhoneWearSync"

    const val PATH_STATE = "/chargesound/battery"
    const val PATH_COMMAND = "/chargesound/command"

    private const val KEY_LEVEL = "level"
    private const val KEY_CHARGING = "charging"
    private const val KEY_WATTS = "watts"
    private const val KEY_TEMP = "temp"
    private const val KEY_SERVICE = "service"
    private const val KEY_TS = "ts"

    /** 워치로 상태를 보내는 최소 간격. */
    private const val MIN_INTERVAL_MS = 60_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var lastPushMs = 0L

    /**
     * 상태를 워치로 보냅니다.
     *
     * @param force true 면 주기 제한을 무시합니다 (충전기 연결/해제, 워치의 새로고침 요청).
     */
    fun requestPush(context: Context, force: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        if (!force && now - lastPushMs < MIN_INTERVAL_MS) return
        lastPushMs = now

        val appContext = context.applicationContext
        scope.launch {
            try {
                val info = BatteryInfoHelper.getBatteryInfo(appContext)
                val watts = PowerTracker.get(appContext).sample()
                val serviceEnabled = SettingsRepository(appContext).settingsFlow.first().isServiceEnabled

                val request = PutDataMapRequest.create(PATH_STATE).apply {
                    dataMap.putInt(KEY_LEVEL, info.level)
                    dataMap.putBoolean(KEY_CHARGING, info.isCharging)
                    // 측정 불가는 음수로 표식합니다. 0W 와 구분해야 워치가
                    // "충전기 안 물려 있음" 으로 오해하지 않습니다.
                    dataMap.putFloat(KEY_WATTS, watts ?: -1f)
                    dataMap.putInt(KEY_TEMP, info.temperatureC.roundToInt())
                    dataMap.putBoolean(KEY_SERVICE, serviceEnabled)
                    dataMap.putLong(KEY_TS, System.currentTimeMillis())
                }.asPutDataRequest().setUrgent()

                Wearable.getDataClient(appContext).putDataItem(request)
            } catch (e: Exception) {
                // 워치가 연결돼 있지 않거나 Play 서비스가 없는 기기에서는 실패합니다.
                // 휴대폰 앱의 본 기능과는 무관하므로 조용히 넘어갑니다.
                Log.d(TAG, "push skipped: ${e.message}")
            }
        }
    }
}
