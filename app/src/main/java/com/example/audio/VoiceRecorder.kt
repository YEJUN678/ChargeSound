package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File

/**
 * 앱 안에서 짧은 충전음을 직접 녹음한다.
 *
 * MP3 파일을 구해 오거나 자를 필요 없이, 마이크로 3~10초를 녹음해
 * 바로 커스텀 충전음으로 쓸 수 있게 한다.
 *
 * ## 수명
 * [start] 로 녹음을 시작하고 [stop] 으로 파일을 돌려받는다.
 * 화면을 벗어나면 반드시 [cancel] 을 호출해 마이크를 놓아야 한다.
 */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    /** 녹음 중인지 여부. */
    val isRecording: Boolean get() = recorder != null

    /** 녹음을 시작한다. 마이크 권한이 없거나 기기 문제가 있으면 false. */
    fun start(): Boolean {
        if (recorder != null) return true
        val file = File(context.filesDir, RECORDED_FILE_NAME)
        var created: MediaRecorder? = null
        return try {
            val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            created = mediaRecorder
            mediaRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = mediaRecorder
            outputFile = file
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            runCatching { created?.release() }
            recorder = null
            outputFile = null
            false
        }
    }

    /**
     * 녹음을 끝내고 파일을 돌려준다.
     * 너무 짧거나 실패하면 null 을 돌려주고 파일을 지운다.
     */
    fun stop(): File? {
        val mediaRecorder = recorder ?: return null
        recorder = null
        val file = outputFile
        outputFile = null
        return try {
            mediaRecorder.stop()
            mediaRecorder.release()
            file?.takeIf { it.exists() && it.length() > MIN_FILE_BYTES }
        } catch (e: Exception) {
            // stop() 은 1초 미만 녹음에서 자주 실패한다 — 정상적인 사용 흐름이다.
            Log.w(TAG, "Failed to stop recording", e)
            runCatching { mediaRecorder.release() }
            file?.delete()
            null
        }
    }

    /** 녹음을 버린다. (화면 종료, 취소) */
    fun cancel() {
        val mediaRecorder = recorder
        recorder = null
        val file = outputFile
        outputFile = null
        runCatching { mediaRecorder?.stop() }
        runCatching { mediaRecorder?.release() }
        file?.delete()
    }

    companion object {
        private const val TAG = "VoiceRecorder"

        /** 녹음 파일 이름. 같은 이름으로 다시 녹음하면 덮어쓴다. */
        const val RECORDED_FILE_NAME = "recorded_charge_sound.m4a"

        /** 최소 파일 크기. 0바이트/깨진 파일을 걸러낸다. */
        private const val MIN_FILE_BYTES = 1_024L

        /** 최대 녹음 길이(초). 10초면 충전음으로 충분하다. */
        const val MAX_RECORDING_SECONDS = 10
    }
}
