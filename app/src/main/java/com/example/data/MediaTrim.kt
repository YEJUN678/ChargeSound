package com.example.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * 사용자 지정 미디어의 재생 구간.
 *
 * ## 왜 파일을 실제로 자르지 않는가
 * 파일을 다시 인코딩하면 경우에 따라 포맷이 깨지거나 화질이 떨어지고,
 * 100MB짜리 영상을 자르려면 시간이 걸리며 임시 파일 공간도 필요합니다.
 * 반면 **재생할 때 시작점으로 건너뛰고 끝점에서 멈추기**는
 * - 어떤 포맷에서도 그대로 동작하고
 * - 재인코딩이 없어 화질 손실이 없고
 * - 몇 KB의 숫자 2개로 저장됩니다.
 *
 * 충전 사운드는 몇 초짜리 한 마디가 대부분이므로, 이 방법이体感상 더 정확합니다.
 */
data class TrimRange(
    /** 전체 길이(밀리초). 알 수 없으면 0 */
    val totalMs: Long,
    /** 선택한 시작 지점(밀리초) */
    val startMs: Long,
    /** 선택한 끝 지점(밀리초). 0 이면 전체 끝 */
    val endMs: Long
) {
    /** 실제 재생 길이 */
    val lengthMs: Long
        get() = when {
            totalMs <= 0L -> 0L
            endMs <= 0L -> (totalMs - startMs).coerceAtLeast(0L)
            else -> (endMs - startMs).coerceAtLeast(0L)
        }

    /** 전체를 재생하는 상태면 자르기를 해제할 필요가 없습니다. */
    val isFull: Boolean
        get() = startMs <= 0L && (endMs <= 0L || (totalMs > 0L && endMs >= totalMs))

    /** 시작점으로부터 재생 완료까지 남은 시간 */
    fun remainingAfter(start: Long): Long = (lengthMs - (start - startMs)).coerceAtLeast(0L)

    /** 범위를 유효한 값으로 정규화합니다 (길이 초과·역전 방지). */
    fun normalized(): TrimRange {
        if (totalMs <= 0L) return TrimRange(0L, 0L, 0L)
        val s = startMs.coerceIn(0L, totalMs)
        val e = when {
            endMs <= 0L -> totalMs
            else -> endMs.coerceIn(0L, totalMs)
        }
        return TrimRange(totalMs, s, maxOf(s, e))
    }
}

/**
 * 미디어 길이/구간 관련 도우미.
 */
object MediaTrim {

    private const val TAG = "MediaTrim"

    /** 최소 선택 길이. 이보다 짧으면 클릭음으로 들립니다. */
    const val MIN_LENGTH_MS = 200L

    /**
     * 파일 또는 content:// URI 의 길이(밀리초)를 읽습니다.
     *
     * 읽을 수 없으면 0 을 돌려줍니다. UI 는 "길이를 알 수 없습니다" 로 안내합니다.
     */
    suspend fun readTotalMs(context: Context, path: String): Long = withContext(Dispatchers.IO) {
        if (path.isBlank()) return@withContext 0L
        val retriever = MediaMetadataRetriever()
        try {
            if (path.startsWith("content://")) {
                retriever.setDataSource(context, android.net.Uri.parse(path))
            } else {
                val f = File(path)
                if (!f.exists()) return@withContext 0L
                retriever.setDataSource(path)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            Log.w(TAG, "failed to read duration: $path", e)
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    /** 안정적인 형태로 경로를 정규화합니다 (content URI 는 그대로 둡니다). */
    fun normalizePath(path: String?): String? = path?.takeIf { it.isNotBlank() }

    /**
     * 범위를 검증합니다. 전체 길이를 넘어가거나 역전된 값을 고쳐줍니다.
     */
    fun sanitize(range: TrimRange): TrimRange {
        val n = range.normalized()
        if (n.totalMs <= 0L) return TrimRange(0L, 0L, 0L)
        if (n.lengthMs < MIN_LENGTH_MS) {
            // 너무 짧으면 끝점을 최소 길이만큼 뒤로 밀어줍니다.
            val newEnd = (n.startMs + MIN_LENGTH_MS).coerceAtMost(n.totalMs)
            return TrimRange(n.totalMs, n.startMs, newEnd)
        }
        return n
    }

    /** 경과 시간을 "m:ss" 로 표시 (예: 1:04) */
    fun formatMs(ms: Long): String {
        if (ms <= 0L) return "0:00"
        val totalSec = ms / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        return if (m > 0) "$m:${s.toString().padStart(2, '0')}" else "0:${s.toString().padStart(2, '0')}"
    }

    /** 입력 스트림 존재 확인 — Uri 권한이 사라졌는지 점검할 때 씁니다. */
    fun canOpen(context: Context, path: String): Boolean = try {
        if (path.startsWith("content://")) {
            context.contentResolver.openInputStream(android.net.Uri.parse(path))?.use { it: InputStream -> true } ?: false
        } else {
            File(path).let { it.exists() && it.canRead() }
        }
    } catch (e: Exception) {
        false
    }
}