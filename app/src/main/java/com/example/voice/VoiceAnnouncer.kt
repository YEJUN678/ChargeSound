package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * 충전 상태를 음성으로 알려줍니다 ("충전을 시작합니다", "80%입니다").
 *
 * ## 초기화가 비동기인 이유
 * [TextToSpeech] 엔진은 생성 직후 바로 말할 수 없습니다. 콜백이 SUCCESS 를
 * 돌려줄 때까지 기다려야 하므로, 그 전에 들어온 요청은 [pending] 에 담아뒀다가
 * 초기화가 끝나면 말합니다.
 *
 * ## 언어
 * 화면에 보이는 문자열과 같은 언어로 읽어야 하므로 기기 로케일을 씁니다.
 * 해당 언어 음성이 없으면 한국어로 떨어집니다(앱 기본 언어).
 *
 * ## 수명
 * TTS 엔진은 무겁습니다. 한 번 만들고 재사용하며, 발화가 끝나면 큐만 비웁니다.
 * (프로세스가 종료되면 자연스럽게 정리됩니다)
 */
object VoiceAnnouncer {

    private const val TAG = "VoiceAnnouncer"
    private const val UTTERANCE_ID = "chargesound_voice"

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingText: String? = null

    @Synchronized
    fun announce(context: Context, text: String) {
        if (text.isBlank()) return
        val app = context.applicationContext

        if (tts == null) {
            ready = false
            tts = TextToSpeech(app) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts
                    val langResult = engine?.setLanguage(Locale.getDefault())
                    if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                        langResult == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        engine?.setLanguage(Locale.KOREAN)
                    }
                    engine?.setSpeechRate(0.95f)
                    ready = true
                    pendingText?.let { queued ->
                        pendingText = null
                        speakNow(queued)
                    }
                } else {
                    Log.w(TAG, "TTS init failed: $status")
                }
            }
        }

        if (ready) {
            speakNow(text)
        } else {
            // 엔진 준비 전 요청은 마지막 것만 말합니다 (연속 알림에서 줄 세우지 않음).
            pendingText = text
        }
    }

    private fun speakNow(text: String) {
        try {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
        } catch (e: Exception) {
            Log.w(TAG, "speak failed", e)
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            // 무시
        }
    }
}
