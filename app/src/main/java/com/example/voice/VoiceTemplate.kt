package com.example.voice

import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** 사용자 음성 문구 안의 토큰을 현재 충전 정보로 바꿉니다. */
object VoiceTemplate {
    fun expand(template: String, level: Int, etaMillis: Long?): String {
        val eta = etaMillis?.takeIf { it >= 0 }?.let { millis ->
            val totalMinutes = (millis / 60_000L).coerceAtLeast(1L)
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            if (hours > 0) "${hours}시간 ${minutes}분" else "${minutes}분"
        } ?: "예측할 수 없습니다"
        val date = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(Date())
        return template
            .replace("(%)", level.coerceIn(0, 100).toString())
            .replace("(t)", eta)
            .replace("(d)", date)
            .trim()
    }
}
