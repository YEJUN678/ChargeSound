package com.example.service

import android.util.Log
import com.example.battery.BatteryInfo
import com.example.data.ChargingSession
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 충전 세션 추적 — 경과 시간과 목표 도달 예상(ETA)을 계산합니다.
 *
 * 충전이 시작되면 시작 시각과 시작 잔량을 기록하고, 잔량이 변할 때마다 갱신합니다.
 * 충전이 끊기면 세션은 종료됩니다(ETA 계산에 오래된 세션을 쓰지 않기 위함).
 */
object ChargingSessionTracker {

    private const val TAG = "ChargingSession"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    data class Snapshot(
        /** 충전 시작 후 경과 시간(밀리초). 충전 중이 아니면 0 */
        val elapsedMillis: Long,
        /** 목표 충전율까지 예상 남은 시간(밀리초). 산출 불가하면 null */
        val etaMillis: Long?,
        /** 충전 중 1시간당 상승하는 % (측정 불가하면 null) */
        val percentPerHour: Float?,
        val isActive: Boolean
    )

    /**
     * 배터리 상태가 바뀔 때마다 호출합니다.
     */
    fun record(context: android.content.Context, info: BatteryInfo, alarmTarget: Int) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                val repo = SettingsRepository(appContext)
                val session = repo.readSession()
                val now = System.currentTimeMillis()

                if (!info.isCharging) {
                    // 충전 종료 — 세션을 비웁니다.
                    if (session.startedAt > 0L) repo.writeSession(ChargingSession())
                    return@launch
                }

                if (session.startedAt <= 0L) {
                    // 충전 시작
                    repo.writeSession(
                        ChargingSession(
                            startedAt = now,
                            startLevel = info.level,
                            lastLevel = info.level,
                            lastChangedAt = now
                        )
                    )
                    Log.d(TAG, "charge session started at $info.level%")
                    return@launch
                }

                // 잔량이 실제로 바뀌었을 때만 시각을 갱신합니다.
                // (같은 % 로 여러 번 이벤트가 오면 분모가 어긋납니다)
                if (info.level != session.lastLevel) {
                    repo.writeSession(
                        session.copy(lastLevel = info.level, lastChangedAt = now)
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "record failed", e)
            }
        }
    }

    /** 충전 종료 시 호출합니다. */
    fun reset(context: android.content.Context) {
        val appContext = context.applicationContext
        scope.launch {
            try {
                val repo = SettingsRepository(appContext)
                repo.writeSession(ChargingSession())
            } catch (e: Exception) {
                Log.w(TAG, "reset failed", e)
            }
        }
    }

    /**
     * 현재 경과 시간과 ETA를 계산합니다.
     * 이 함수는 코루틴 밖에서 호출 가능하도록 동기적입니다.
     */
    suspend fun snapshot(context: android.content.Context, info: BatteryInfo): Snapshot {
        val appContext = context.applicationContext
        val now = System.currentTimeMillis()

        return try {
            val repo = SettingsRepository(appContext)
            val session = repo.readSession()

            if (!info.isCharging || session.startedAt <= 0L) {
                return Snapshot(0L, null, null, false)
            }

            val elapsed = now - session.startedAt
            if (elapsed <= 0L) return Snapshot(0L, null, null, true)

            val gained = info.level - session.startLevel
            if (gained <= 0) {
                // 아직 충전량이 오르지 않았으면 예측할 수 없습니다.
                return Snapshot(elapsed, null, null, true)
            }

            val perHour = gained.toFloat() / (elapsed / 3_600_000f)
            val settings = repo.settingsFlow.first()
            val target = settings.alarmTargetPercent
            val remainingPercent = target - info.level

            val eta = if (perHour <= 0.01f || remainingPercent <= 0) {
                null
            } else {
                (remainingPercent.toFloat() / perHour * 3_600_000f).toLong()
            }

            Snapshot(elapsed, eta, perHour, true)
        } catch (e: Exception) {
            Log.w(TAG, "snapshot failed", e)
            Snapshot(0L, null, null, false)
        }
    }
}