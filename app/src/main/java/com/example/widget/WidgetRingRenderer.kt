package com.example.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient

/**
 * 위젯의 원형 배터리 게이지를 비트맵으로 그립니다.
 *
 * ## 왜 비트맵인가
 * RemoteViews 는 커스텀 뷰를 쓸 수 없습니다. ProgressBar 는 **가로 막대**만 되고
 * 회전이나 원형 아크는 지원하지 않습니다. 그래서 매 업데이트마다 Canvas 로 한 장을
 * 그려 ImageView 에 밀어넣습니다.
 *
 * 크기가 크지 않고(최대 92dp) 갱신도 분 단위라 비용은 무시할 수 있습니다.
 * 같은 조합이 반복되는 경우(홈 화면 갱신)를 위해 최근 결과는 캐시합니다.
 */
internal object WidgetRingRenderer {

    private const val MAX_CACHED = 12
    private val cache = LinkedHashMap<String, Bitmap>()

    /**
     * @param sizePx   한 변의 픽셀 크기 (정사각형)
     * @param level    배터리 잔량 0~100
     * @param accent   진행 아크 색 (ARGB)
     * @param track    바탕 링 색 (ARGB)
     * @param charging 충전 중이면 안쪽에 옅은 발광을 넣습니다
     */
    fun render(sizePx: Int, level: Int, accent: Int, track: Int, charging: Boolean): Bitmap {
        val key = "$sizePx|$level|$accent|$track|$charging"
        cache[key]?.let { return it }

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val center = sizePx / 2f

        // 링 두께는 크기에 비례 — 24dp 짜리 미니 위젯에서도 뭉개지지 않게.
        val stroke = (sizePx * 0.115f).coerceAtLeast(3f)
        val inset = stroke / 2f + sizePx * 0.015f
        val arcRect = RectF(inset, inset, sizePx - inset, sizePx - inset)

        // 1) 바탕 링
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            color = track
        }
        canvas.drawArc(arcRect, 0f, 360f, false, trackPaint)

        // 2) 충전 중일 때 안쪽 발광 — 화면이 꺼져 있어도 "살아 있다"는 느낌을 줍니다.
        if (charging) {
            val glowRadius = center - inset
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    center, center, glowRadius,
                    intArrayOf(withAlpha(accent, 0.22f), withAlpha(accent, 0f)),
                    floatArrayOf(0f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(center, center, glowRadius, glow)
        }

        // 3) 진행 아크
        val sweep = (level.coerceIn(0, 100) / 100f) * 360f
        if (sweep > 0f) {
            val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
                shader = SweepGradient(
                    center, center,
                    intArrayOf(accent, lighten(accent), accent),
                    floatArrayOf(0f, 0.55f, 1f)
                )
            }
            canvas.save()
            // SweepGradient 는 3시 방향에서 시작합니다. 12시로 돌립니다.
            canvas.rotate(-90f, center, center)
            canvas.drawArc(arcRect, 0f, sweep, false, arcPaint)
            canvas.restore()
        }

        if (cache.size >= MAX_CACHED) cache.clear()
        cache[key] = bitmap
        return bitmap
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        (color and 0x00FFFFFF) or (((alpha.coerceIn(0f, 1f) * 255).toInt()) shl 24)

    /** 아크 중간 톤 — 단색보다 입체적으로 보입니다. */
    private fun lighten(color: Int): Int {
        val r = ((color shr 16 and 0xFF) * 0.55f + 0xFF * 0.45f).toInt().coerceIn(0, 255)
        val g = ((color shr 8 and 0xFF) * 0.55f + 0xFF * 0.45f).toInt().coerceIn(0, 255)
        val b = ((color and 0xFF) * 0.55f + 0xFF * 0.45f).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
