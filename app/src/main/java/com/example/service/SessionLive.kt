package com.example.service

import android.content.Context

/**
 * 충전 중 실시간 값(완충 예상 시각)을 잠깐 공유하는 곳.
 *
 * ## 왜 DataStore 가 아닌가
 * DataStore 는 읽기가 suspend 입니다. 위젯([com.example.widget.ChargingWidgetProvider])은
 * RemoteViews 를 **동기적으로** 만들어야 해서 suspend 를 부를 수 없습니다.
 * 값 하나(epoch ms)만 공유하면 되므로 SharedPreferences 가 맞습니다.
 *
 * 쓰는 쪽은 [ChargingSessionTracker], 읽는 쪽은 위젯입니다.
 */
object SessionLive {

    private const val PREFS = "session_live"
    private const val KEY_ETA_AT = "eta_at"

    fun writeEta(context: Context, etaAtMillis: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_ETA_AT, etaAtMillis)
            .apply()
    }

    fun clearEta(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_ETA_AT)
            .apply()
    }

    /** 완충 예상 시각(epoch ms). 없거나 이미 지났으면 0 */
    fun etaAt(context: Context): Long {
        val at = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_ETA_AT, 0L)
        return if (at > System.currentTimeMillis()) at else 0L
    }
}
