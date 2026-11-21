package com.example.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

/**
 * 충전 중에만 흐르는 은은한 앰비언트 사운드.
 *
 * 별도 음원을 번들하지 않고, 처음 한 번 10초짜리 무봉제 루프 WAV 를
 * 기기 안에서 만들어 재생합니다. (APK 용량 증가 없음)
 *
 * - 충전기를 꽂으면 1.4초 동안 페이드인
 * - 뽑으면 0.6초 동안 페이드아웃 후 해제
 * - 조용한 시간대에는 호출되지 않습니다. (호출부에서 판단)
 */
object ChargingAmbience {

    private const val TAG = "ChargingAmbience"
    private const val FILE_NAME = "charging_space.wav"
    private const val SAMPLE_RATE = 22_050
    private const val LOOP_SECONDS = 10
    private const val TARGET_VOLUME = 0.32f
    private const val MIN_VOLUME = 0.05f

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var player: MediaPlayer? = null
    private var prepareJob: Job? = null
    private var fadeJob: Job? = null

    /** 현재 목표 볼륨. 재생 중 슬라이더 변경도 여기에 반영됩니다. */
    @Volatile
    private var targetVolume: Float = TARGET_VOLUME

    /** 충전 시작 시 호출. 이미 재생 중이면 아무 것도 하지 않습니다. */
    fun start(context: Context, volume: Float = TARGET_VOLUME) {
        if (player != null || prepareJob?.isActive == true) return
        val app = context.applicationContext
        val target = volume.coerceIn(MIN_VOLUME, 1f)
        targetVolume = target

        prepareJob = scope.launch {
            val file = withContext(Dispatchers.IO) { ensureLoopFile(app) }
            if (file == null) {
                Log.w(TAG, "Could not prepare ambient loop file")
                return@launch
            }

            val created = withContext(Dispatchers.IO) {
                runCatching {
                    MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .build()
                        )
                        setDataSource(app, Uri.fromFile(file))
                        isLooping = true
                        setVolume(0f, 0f)
                        prepare()
                    }
                }.getOrNull()
            }

            if (created == null) {
                Log.w(TAG, "Could not create ambient player")
                return@launch
            }

            player = created
            runCatching { created.start() }
            fadeTo(created, 0f, target, 1_400L)
        }
    }

    /** 재생 중 볼륨을 부드럽게 바꿉니다. (설정 슬라이더) */
    fun setVolume(volume: Float) {
        val target = volume.coerceIn(MIN_VOLUME, 1f)
        val previous = targetVolume
        targetVolume = target
        val current = player ?: return
        fadeTo(current, previous, target, 350L)
    }

    /** 충전 해제 · 설정 끄기 · 모니터링 종료 시 호출. */
    fun stop() {
        prepareJob?.cancel()
        prepareJob = null

        val current = player ?: return
        player = null
        val from = targetVolume
        fadeJob?.cancel()
        fadeJob = scope.launch {
            var volume = from
            val steps = 12
            repeat(steps) { index ->
                volume = from * (1f - (index + 1) / steps.toFloat())
                runCatching { current.setVolume(volume, volume) }
                delay(50)
            }
            runCatching {
                if (current.isPlaying) current.stop()
            }
            runCatching { current.release() }
        }
    }

    private fun fadeTo(target: MediaPlayer, from: Float, to: Float, durationMs: Long) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val steps = 24
            repeat(steps) { index ->
                val value = from + (to - from) * (index + 1) / steps.toFloat()
                runCatching { target.setVolume(value, value) }
                delay(durationMs / steps)
            }
        }
    }

    private fun ensureLoopFile(context: Context): File? {
        return try {
            val dir = File(context.filesDir, "ambient")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, FILE_NAME)
            if (file.exists() && file.length() > 44L) {
                file
            } else {
                writeLoop(file)
                file
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate ambient loop", e)
            null
        }
    }

    /**
     * 10초 무봉제 루프를 만든다.
     *
     * 모든 주파수를 루프 길이의 정수배(0.1Hz 단위)로 맞춰서
     * 이음새에서 위상이 튀지 않게 했습니다.
     */
    private fun writeLoop(file: File) {
        val total = SAMPLE_RATE * LOOP_SECONDS
        val samples = ShortArray(total)
        val twoPi = 2.0 * Math.PI

        for (i in 0 until total) {
            val t = i.toDouble() / SAMPLE_RATE
            // 10초 주기로 천천히 부풀었다 잦아드는 패드
            val swell = 0.62 + 0.38 * sin(twoPi * 0.1 * t)
            val pad = 0.50 * sin(twoPi * 110.0 * t) +
                0.28 * sin(twoPi * 165.0 * t) +
                0.18 * sin(twoPi * 220.0 * t)
            // 5초 주기로 반짝이는 고음
            val shimmer = 0.09 * sin(twoPi * 330.0 * t) *
                (0.5 + 0.5 * sin(twoPi * 0.2 * t))
            val value = (pad * swell * 0.55 + shimmer) * 0.5
            samples[i] = (value.coerceIn(-1.0, 1.0) * Short.MAX_VALUE).toInt().toShort()
        }

        val dataSize = samples.size * 2
        val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + dataSize)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)                       // PCM chunk size
        buffer.putShort(1)                      // PCM
        buffer.putShort(1)                      // mono
        buffer.putInt(SAMPLE_RATE)
        buffer.putInt(SAMPLE_RATE * 2)          // byte rate
        buffer.putShort(2)                      // block align
        buffer.putShort(16)                     // bits per sample
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataSize)
        for (sample in samples) buffer.putShort(sample)

        file.writeBytes(buffer.array())
    }
}
