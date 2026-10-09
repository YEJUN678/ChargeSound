package com.example.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import com.example.data.SoundPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.max
import kotlin.math.sin

object SoundManager {
    private const val TAG = "SoundManager"
    private var activeMediaPlayer: MediaPlayer? = null
    private var fadeJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    val PRESETS = listOf(
        SoundPreset("cyber_chime", "사이버 차임 (Cyber Chime)", "미래지향적인 3단 상승 차임음", "bolt"),
        SoundPreset("lightning_spark", "일렉트릭 스파크 (Spark)", "짜릿한 전기 스파크 및 펄스 사운드", "flash_on"),
        SoundPreset("super_charge", "슈퍼 차저 (Super Level Up)", "경쾌한 게임 레벨업 스타일 상승 멜로디", "upgrade"),
        SoundPreset("gentle_ding", "젠틀 딩 (Gentle Bell)", "은은하고 부드러운 맑은 크리스탈 종소리", "notifications"),
        SoundPreset("turbo_surge", "터보 엔진 서지 (Turbo Surge)", "우주선 부스터 가동 사운드", "speed")
    )

    /**
     * 프리미엄 사운드. [com.example.data.PremiumCatalog]와 동일한 ID를 사용합니다.
     * 광고로 해제되기 전에는 생성하지 않습니다(정책: 미해제 콘텐츠를 임의 재생하지 않음).
     */
    val PREMIUM_PRESETS: List<SoundPreset> by lazy {
        com.example.data.PremiumCatalog.sounds.map { com.example.data.PremiumCatalog.soundToPreset(it) }
    }

    /** 기본(무료) 프리셋 목록 — 광고와 무관하게 항상 사용 가능 */
    val FREE_PRESETS: List<SoundPreset> get() = PRESETS

    /**
     * 광고로 해제된 프리미엄 사운드의 WAV 파일을 생성합니다.
     * 파일이 이미 있으면 재생성하지 않습니다.
     */
    fun ensurePremiumSound(context: Context, presetId: String): File? {
        if (!com.example.data.PremiumCatalog.isPremiumSoundId(presetId)) return null
        val targetFile = getPresetFile(context, presetId)
        if (targetFile.exists() && targetFile.length() > 0L) return targetFile
        return try {
            targetFile.parentFile?.mkdirs()
            writeWavFile(targetFile, generatePresetSamples(presetId), sampleRate = 44100)
            targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create premium sound: $presetId", e)
            null
        }
    }

    fun initializePresets(context: Context) {
        val presetDir = File(context.filesDir, "presets").apply { if (!exists()) mkdirs() }
        // 무료 프리셋만 생성합니다. 프리미엄 사운드는 광고로 잠금을 해제할 때
        // [ensurePremiumSound]에서 필요할 때만 생성합니다.
        PRESETS.forEach { preset ->
            val targetFile = File(presetDir, "${preset.id}.wav")
            if (!targetFile.exists() || targetFile.length() == 0L) {
                try {
                    val pcmData = generatePresetSamples(preset.id)
                    writeWavFile(targetFile, pcmData, sampleRate = 44100)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create preset sound: ${preset.id}", e)
                }
            }
        }
    }

    fun getPresetFile(context: Context, presetId: String): File {
        val presetDir = File(context.filesDir, "presets")
        return File(presetDir, "$presetId.wav")
    }

    @Synchronized
    fun playSound(
        context: Context,
        filePath: String?,
        volume: Float,
        durationSeconds: Int = -1, // >0: sync fade-out with animation duration
        onCompletion: (() -> Unit)? = null
    ) {
        stopSound()
        if (filePath.isNullOrBlank()) {
            onCompletion?.invoke()
            return
        }

        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            Log.w(TAG, "Sound file does not exist: $filePath")
            onCompletion?.invoke()
            return
        }

        try {
            val clampedVolume = volume.coerceIn(0f, 1f)
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .build()
                )
                setDataSource(context, Uri.fromFile(file))
                setVolume(clampedVolume, clampedVolume)
                setOnCompletionListener { mp ->
                    mp.release()
                    if (activeMediaPlayer == mp) {
                        activeMediaPlayer = null
                    }
                    onCompletion?.invoke()
                }
                setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    mp.release()
                    if (activeMediaPlayer == mp) {
                        activeMediaPlayer = null
                    }
                    onCompletion?.invoke()
                    true
                }
                prepare()
                start()
            }
            activeMediaPlayer = player

            // Schedule audio fade-out according to animation playback duration
            if (durationSeconds > 0) {
                scheduleFadeOut(player, clampedVolume, durationSeconds, onCompletion)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound: $filePath", e)
            onCompletion?.invoke()
        }
    }

    private fun scheduleFadeOut(
        player: MediaPlayer,
        startVolume: Float,
        durationSeconds: Int,
        onCompletion: (() -> Unit)?
    ) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val totalDurationMs = durationSeconds * 1000L
            val fadeDurationMs = 1500L.coerceAtMost(totalDurationMs)
            val delayBeforeFade = max(0L, totalDurationMs - fadeDurationMs)

            delay(delayBeforeFade)

            // Gradually decrease volume to 0
            val steps = 15
            val stepDelay = fadeDurationMs / steps
            for (i in steps downTo 0) {
                if (activeMediaPlayer != player) break
                val currentVol = (startVolume * i / steps).coerceIn(0f, 1f)
                try {
                    player.setVolume(currentVol, currentVol)
                } catch (e: Exception) {
                    break
                }
                delay(stepDelay)
            }

            // Stop and release once faded out
            if (activeMediaPlayer == player) {
                stopSound()
                onCompletion?.invoke()
            }
        }
    }

    fun fadeOutAndStop(fadeDurationMs: Long = 600L) {
        fadeJob?.cancel()
        val player = activeMediaPlayer ?: return
        fadeJob = scope.launch {
            val steps = 10
            val stepDelay = fadeDurationMs / steps
            for (i in steps downTo 0) {
                if (activeMediaPlayer != player) break
                val factor = i.toFloat() / steps
                try {
                    player.setVolume(factor, factor)
                } catch (e: Exception) {
                    break
                }
                delay(stepDelay)
            }
            stopSound()
        }
    }

    @Synchronized
    fun stopSound() {
        fadeJob?.cancel()
        fadeJob = null
        try {
            activeMediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping sound", e)
        } finally {
            activeMediaPlayer = null
        }
    }

    private fun generatePresetSamples(presetId: String): ShortArray {
        val sampleRate = 44100
        val durationSec: Double

        when (presetId) {
            // ── 프리미엄 사운드 (보상형 광고로 해제) ──
            // 모두 실시간 PCM 합성이라 배포/ 저작권 문제가 없습니다.
            "prm_synthwave" -> {
                val semitone = 2.0 * (2.0.pow(1.0 / 12.0))
                durationSec = 3.2
                val totalSamples = (durationSec * sampleRate).toInt()
                val samples = ShortArray(totalSamples)
                // Aminor9 기반 4음 아르페지오 + 고조화 배음
                val notes = doubleArrayOf(220.00, 261.63, 329.63, 493.88)
                val noteSamples = (0.28 * sampleRate).toInt()
                for (i in 0 until totalSamples) {
                    val idx = (i / noteSamples).coerceAtMost(notes.size - 1)
                    val local = i - idx * noteSamples
                    val t = local.toDouble() / sampleRate
                    val base = notes[idx] * semitone.pow((idx % 3).toDouble())
                    val env = kotlin.math.exp(-local.toDouble() / (0.5 * sampleRate))
                    val wave = sin(2.0 * PI * base * t) +
                            0.5 * sin(2.0 * PI * base * 2.0 * t) +
                            0.25 * sin(2.0 * PI * base * 3.0 * t)
                    val sustain = 1.0 - (i.toDouble() / totalSamples) * 0.45
                    samples[i] = (wave * env * sustain * 20000)
                        .toInt().coerceIn(-32768, 32767).toShort()
                }
                return samples
            }
            "prm_chiptune" -> {
                durationSec = 2.8
                val totalSamples = (durationSec * sampleRate).toInt()
                val samples = ShortArray(totalSamples)
                val step = (0.18 * sampleRate).toInt()
                // C-Am-F-G, 16비트 제곱파 + 피루스 노이즈 → 8비트 느낌
                val melody = doubleArrayOf(
                    523.25, 659.25, 783.99, 880.00, 783.99, 659.25, 523.25, 392.00,
                    440.00, 523.25, 659.25, 523.25, 440.00, 392.00, 329.63, 261.63
                )
                for (i in 0 until totalSamples) {
                    val idx = (i / step).coerceAtMost(melody.size - 1)
                    val local = i - idx * step
                    val t = local.toDouble() / sampleRate
                    val freq = melody[idx]
                    val env = kotlin.math.exp(-local.toDouble() / (0.22 * sampleRate))
                    val square = if (sin(2.0 * PI * freq * t) >= 0) 1.0 else -1.0
                    val noise = (Math.random() * 2.0 - 1.0) * 0.18
                    val duty = 0.25 + 0.5 * (0.5 + 0.5 * sin(2.0 * PI * 6.0 * t))
                    val pulse = if (((t * freq) % 1.0) < duty) 1.0 else -1.0
                    val wave = (pulse * 0.6 + square * 0.3) * (1.0 - noise) + noise
                    samples[i] = (wave * env * 17000)
                        .toInt().coerceIn(-32768, 32767).toShort()
                }
                return samples
            }
            "prm_ambient" -> {
                durationSec = 4.0
                val totalSamples = (durationSec * sampleRate).toInt()
                val samples = ShortArray(totalSamples)
                // 완전하게 어긋난 3음 패드 + 아주 느린 LFO
                val freqs = doubleArrayOf(110.0, 164.81, 220.0)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val fadeIn = (t / 0.8).coerceAtMost(1.0)
                    val fadeOut = ((durationSec - t) / 1.2).coerceAtMost(1.0)
                    val env = (fadeIn * fadeOut).coerceIn(0.0, 1.0)
                    var wave = 0.0
                    for (j in freqs.indices) {
                        val detune = 1.0 + 0.0016 * j
                        wave += sin(2.0 * PI * freqs[j] * detune * t) / (j + 1.5)
                    }
                    val lfo = 0.82 + 0.18 * sin(2.0 * PI * 0.35 * t)
                    samples[i] = (wave * env * lfo * 23000)
                        .toInt().coerceIn(-32768, 32767).toShort()
                }
                return samples
            }
            "prm_brass" -> {
                durationSec = 2.6
                val totalSamples = (durationSec * sampleRate).toInt()
                val samples = ShortArray(totalSamples)
                // 3화음 팡파르, sawtooth 근사 → 필터링된 금속음
                val freqs = doubleArrayOf(146.83, 185.00, 220.00)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val attack = (t / 0.09).coerceAtMost(1.0)
                    val release = ((durationSec - t) / 0.6).coerceAtMost(1.0)
                    val env = (attack * release).coerceIn(0.0, 1.0)
                    var wave = 0.0
                    for (freq in freqs) {
                        var partial = 0.0
                        for (h in 1..7) {
                            partial += sin(2.0 * PI * freq * h * t) / h
                        }
                        wave += partial / 3.0
                    }
                    // 미세한 비브라토
                    val vib = 1.0 + 0.004 * sin(2.0 * PI * 5.5 * t)
                    samples[i] = (wave * env * vib * 21000)
                        .toInt().coerceIn(-32768, 32767).toShort()
                }
                return samples
            }
            "prm_rainbow" -> {
                durationSec = 3.4
                val totalSamples = (durationSec * sampleRate).toInt()
                val samples = ShortArray(totalSamples)
                // 도maj 7음 캐스케이드 (C-B)
                val freqs = doubleArrayOf(
                    523.25, 587.33, 659.25, 783.99, 880.00, 1046.50, 1174.66, 1567.98
                )
                val stepSamples = (totalSamples / freqs.size)
                for (i in 0 until totalSamples) {
                    val idx = (i / stepSamples).coerceAtMost(freqs.size - 1)
                    val local = i - idx * stepSamples
                    val t = local.toDouble() / sampleRate
                    val env = kotlin.math.exp(-local.toDouble() / (0.55 * sampleRate))
                    val wave = sin(2.0 * PI * freqs[idx] * t) +
                            0.45 * sin(2.0 * PI * freqs[idx] * 2.76 * t)
                    samples[i] = (wave * env * 21500)
                        .toInt().coerceIn(-32768, 32767).toShort()
                }
                return samples
            }
            "prm_vaporwave" -> {
                durationSec = 3.6
                val totalSamples = (durationSec * sampleRate).toInt()
                val samples = ShortArray(totalSamples)
                val bpm = 84.0
                val beatSamples = (60.0 / bpm * sampleRate).toInt()
                val bassPattern = doubleArrayOf(110.0, 0.0, 146.83, 0.0, 130.81, 0.0, 110.0, 0.0)
                for (i in 0 until totalSamples) {
                    val step = (i / beatSamples).coerceAtMost(bassPattern.size - 1)
                    val local = i - step * beatSamples
                    val t = local.toDouble() / sampleRate
                    val freq = bassPattern[step]
                    val env = kotlin.math.exp(-local.toDouble() / (0.35 * sampleRate))
                    var wave = 0.0
                    if (freq > 0.0) {
                        wave = sin(2.0 * PI * freq * t) + 0.3 * sin(2.0 * PI * freq * 2.0 * t)
                    }
                    // 느린 빈 페이드
                    val slow = 0.7 + 0.3 * sin(2.0 * PI * 0.2 * i.toDouble() / sampleRate)
                    samples[i] = (wave * env * slow * 23000)
                        .toInt().coerceIn(-32768, 32767).toShort()
                }
                return samples
            }
            "cyber_chime" -> {
                durationSec = 2.5
                val totalSamples = (durationSec * sampleRate).toInt()
                val samples = ShortArray(totalSamples)
                val noteDuration = (0.35 * sampleRate).toInt()
                val freqs = doubleArrayOf(659.25, 830.61, 987.77)
                for (i in 0 until totalSamples) {
                    val noteIndex = (i / noteDuration).coerceAtMost(2)
                    val noteLocalI = i - noteIndex * noteDuration
                    val t = noteLocalI.toDouble() / sampleRate
                    val freq = freqs[noteIndex]
                    val envelope = kotlin.math.exp(-noteLocalI.toDouble() / (0.4 * sampleRate))
                    val wave = sin(2.0 * PI * freq * t) + 0.4 * sin(4.0 * PI * freq * t)
                    samples[i] = (wave * envelope * 24000).toInt().coerceIn(-32768, 32767).toShort()
                }
                return samples
            }
            "lightning_spark" -> {
                durationSec = 2.0
                val totalSamples = (durationSec * sampleRate).toInt()
                val samples = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = (1.0 - t / durationSec).coerceAtLeast(0.0)
                    val sweepFreq = 1800.0 * (1.0 - t * 0.7)
                    val noise = (Math.random() * 2.0 - 1.0) * 0.25
                    val wave = sin(2.0 * PI * sweepFreq * t + sin(2.0 * PI * 120.0 * t) * 2.0) + noise
                    samples[i] = (wave * envelope * 26000).toInt().coerceIn(-32768, 32767).toShort()
                }
                return samples
            }
            "super_charge" -> {
                durationSec = 2.6
                val totalSamples = (durationSec * sampleRate).toInt()
                val samples = ShortArray(totalSamples)
                val step = (0.3 * sampleRate).toInt()
                val freqs = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
                for (i in 0 until totalSamples) {
                    val idx = (i / step).coerceAtMost(3)
                    val local = i - idx * step
                    val t = local.toDouble() / sampleRate
                    val freq = freqs[idx]
                    val env = kotlin.math.exp(-local.toDouble() / (0.45 * sampleRate))
                    val wave = sin(2.0 * PI * freq * t) + 0.3 * sin(2.0 * PI * freq * 2 * t)
                    samples[i] = (wave * env * 25000).toInt().coerceIn(-32768, 32767).toShort()
                }
                return samples
            }
            "gentle_ding" -> {
                durationSec = 3.0
                val totalSamples = (durationSec * sampleRate).toInt()
                val samples = ShortArray(totalSamples)
                val freq = 880.0
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = kotlin.math.exp(-t * 1.5)
                    val wave = sin(2.0 * PI * freq * t) + 0.5 * sin(2.0 * PI * freq * 2.04 * t) + 0.2 * sin(2.0 * PI * freq * 3.0 * t)
                    samples[i] = (wave * envelope * 22000).toInt().coerceIn(-32768, 32767).toShort()
                }
                return samples
            }
            "turbo_surge" -> {
                durationSec = 2.8
                val totalSamples = (durationSec * sampleRate).toInt()
                val samples = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val progress = t / durationSec
                    val freq = 150.0 + 800.0 * (progress * progress)
                    val envelope = if (progress < 0.7) progress / 0.7 else (1.0 - progress) / 0.3
                    val wave = sin(2.0 * PI * freq * t) + 0.35 * sin(4.0 * PI * freq * t)
                    samples[i] = (wave * envelope * 27000).toInt().coerceIn(-32768, 32767).toShort()
                }
                return samples
            }
            else -> {
                durationSec = 2.0
                val totalSamples = (durationSec * sampleRate).toInt()
                val samples = ShortArray(totalSamples)
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / sampleRate
                    val wave = sin(2.0 * PI * 440.0 * t) * (1.0 - t)
                    samples[i] = (wave * 20000).toInt().coerceIn(-32768, 32767).toShort()
                }
                return samples
            }
        }
    }

    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
        val byteDataLength = pcmData.size * 2
        val totalDataLength = byteDataLength + 36

        FileOutputStream(file).use { out ->
            val header = ByteBuffer.allocate(44).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                put("RIFF".toByteArray())
                putInt(totalDataLength)
                put("WAVE".toByteArray())
                put("fmt ".toByteArray())
                putInt(16)
                putShort(1)
                putShort(1)
                putInt(sampleRate)
                putInt(sampleRate * 2)
                putShort(2)
                putShort(16)
                put("data".toByteArray())
                putInt(byteDataLength)
            }
            out.write(header.array())

            val audioBuffer = ByteBuffer.allocate(pcmData.size * 2).apply {
                order(ByteOrder.LITTLE_ENDIAN)
                pcmData.forEach { putShort(it) }
            }
            out.write(audioBuffer.array())
            out.flush()
        }
    }
}
