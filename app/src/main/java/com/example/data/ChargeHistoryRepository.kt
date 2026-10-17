package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * 완료된 충전 세션 1건.
 *
 * 통계 화면과 "배터리 수명" 판단의 재료입니다. 저장하지 않고 읽기만 하는 기록이므로
 * 서버로 보낼 수도 없습니다.
 */
data class ChargeRecord(
    /** 충전이 시작된 시각 (epoch millis) */
    val startedAt: Long,
    /** 충전이 끝난 시각 (epoch millis) */
    val endedAt: Long,
    /** 충전 시작 시 배터리 잔량 */
    val startLevel: Int,
    /** 충전 종료 시 배터리 잔량 */
    val endLevel: Int,
    /** 충전 중 최고 온도 (℃) */
    val peakTempC: Float,
    /** 충전기 유형 (ac / usb / wireless / none) */
    val plugType: String,
    /** 설정된 알람 임계값 중 하나에 도달했는지 */
    val reachedAlarmTarget: Boolean
) {
    /** 충전에 걸린 시간(밀리초) */
    val durationMillis: Long get() = (endedAt - startedAt).coerceAtLeast(0L)

    /** 실제로 오른 잔량 (% points) */
    val gainedPercent: Int get() = (endLevel - startLevel).coerceAtLeast(0)

    /** 시간당 상승률. 측정이 안 될 만큼 짧으면 null */
    val percentPerHour: Float?
        get() {
            val hours = durationMillis / 3_600_000f
            if (hours < 1f / 60f) return null   // 1분 미만은 노이즈
            return gainedPercent / hours
        }
}

/**
 * 충전 기록 저장소.
 *
 * 최대 [MAX_RECORDS] 건까지만 유지합니다. 오래된 기록은 지워지므로
 * "이 기기를 2년간 썼을 때의 통계"는 되지 않습니다. 최근 습관을 보려는 기능입니다.
 */
object ChargeHistoryRepository {

    private const val TAG = "ChargeHistory"
    private const val MAX_RECORDS = 100

    fun flow(context: Context): Flow<List<ChargeRecord>> =
        SettingsRepository(context.applicationContext).historyFlow

    suspend fun read(context: Context): List<ChargeRecord> = flow(context).first()

    /**
     * 기록 1건을 추가합니다.
     *
     * 최대치를 넘으면 가장 오래된 것부터 버립니다.
     */
    suspend fun append(context: Context, record: ChargeRecord) {
        try {
            val repo = SettingsRepository(context.applicationContext)
            val current = repo.readHistory()
            val next = (listOf(record) + current)
                .sortedByDescending { it.startedAt }
                .take(MAX_RECORDS)
            repo.writeHistory(next)
        } catch (e: Exception) {
            Log.w(TAG, "append failed", e)
        }
    }

    suspend fun clear(context: Context) {
        try {
            SettingsRepository(context.applicationContext).writeHistory(emptyList())
        } catch (e: Exception) {
            Log.w(TAG, "clear failed", e)
        }
    }

    /**
     * 직렬화.
     *
     * JSONObject 배열 형태입니다. 필드가 하나라도 깨져도 앱 전체가 죽지 않도록
     * 파싱 실패 시 빈 리스트를 돌려줍니다 — 통계는 부가 기능이기 때문입니다.
     */
    fun encode(records: List<ChargeRecord>): String {
        val array = JSONArray()
        records.forEach { r ->
            array.put(
                JSONObject().apply {
                    put("startedAt", r.startedAt)
                    put("endedAt", r.endedAt)
                    put("startLevel", r.startLevel)
                    put("endLevel", r.endLevel)
                    put("peakTempC", r.peakTempC.toDouble())
                    put("plugType", r.plugType)
                    put("reached", r.reachedAlarmTarget)
                }
            )
        }
        return array.toString()
    }

    fun decode(raw: String?): List<ChargeRecord> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val started = o.optLong("startedAt", 0L)
                if (started <= 0L) return@mapNotNull null
                ChargeRecord(
                    startedAt = started,
                    endedAt = o.optLong("endedAt", started),
                    startLevel = o.optInt("startLevel", 0),
                    endLevel = o.optInt("endLevel", 0),
                    peakTempC = o.optDouble("peakTempC", 0.0).toFloat(),
                    plugType = o.optString("plugType", "none"),
                    reachedAlarmTarget = o.optBoolean("reached", false)
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "decode failed", e)
            emptyList()
        }
    }
}