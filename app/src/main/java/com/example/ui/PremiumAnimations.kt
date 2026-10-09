package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.ui.theme.Slate950
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 프리미엄 애니메이션 테마.
 *
 * 보상형 광고로 잠금을 해제하면 사용할 수 있는 실시간 렌더링 애니메이션입니다.
 * 전부 코드로 그리기 때문에 배포·저작자 표시·파일 크기 문제가 없습니다.
 *
 * 각 함수에는 @OptIn이 필요하지 않으며, 공통으로 무한 반복 트랜지션을 사용합니다.
 */

private val NebulaA = Color(0xFFA78BFA)
private val NebulaB = Color(0xFF38BDF8)
private val NebulaC = Color(0xFFF472B6)

private val MatrixA = Color(0xFF4ADE80)
private val MatrixB = Color(0xFF16A34A)
private val MatrixC = Color(0xFFBBF7D0)

private val AbyssA = Color(0xFF0EA5E9)
private val AbyssB = Color(0xFF1E3A8A)
private val AbyssC = Color(0xFF7DD3FC)

private val PrismA = Color(0xFFF472B6)
private val PrismB = Color(0xFF818CF8)
private val PrismC = Color(0xFFFBBF24)

private val FireA = Color(0xFFFB923C)
private val FireB = Color(0xFFFDE047)
private val FireC = Color(0xFFEF4444)

private val HoleA = Color(0xFF818CF8)
private val HoleB = Color(0xFFC084FC)
private val HoleC = Color(0xFFFFFFFF)

/** 별도 파일로 분리한 프리미엄 애니메이션 모음 */

/** 성운: 회전하는 입자 + 방사형 광휘 */
@Composable
fun NebulaAnimation() {
    val infinite = rememberInfiniteTransition(label = "nebula")
    val angle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "nebula_angle"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(3200, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse),
        label = "nebula_pulse"
    )
    val random = remember { Random(2024) }
    val stars = remember { List(180) { Triple(random.nextFloat(), random.nextFloat(), random.nextFloat()) } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Slate950)
            // 중심에서 방사되는 두 개의 부드러운 광휘
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NebulaA.copy(alpha = 0.45f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = size.minDimension * 0.8f * pulse
                ),
                radius = size.minDimension * 0.8f * pulse,
                center = Offset(cx, cy)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NebulaB.copy(alpha = 0.35f), Color.Transparent),
                    center = Offset(cx + size.width * 0.2f, cy - size.height * 0.1f),
                    radius = size.minDimension * 0.6f * pulse
                ),
                radius = size.minDimension * 0.6f * pulse,
                center = Offset(cx + size.width * 0.2f, cy - size.height * 0.1f)
            )
            // 회전하는 별 입자
            rotate(angle, Offset(cx, cy)) {
                stars.forEachIndexed { index, (fx, fy, sz) ->
                    val px = fx * size.width
                    val py = fy * size.height
                    val radius = (sz * 2.2f + 0.8f) * density
                    val color = when (index % 3) {
                        0 -> NebulaC
                        1 -> NebulaB
                        else -> Color.White
                    }
                    drawCircle(color.copy(alpha = 0.25f + sz * 0.5f), radius, Offset(px, py))
                }
            }
            // 나선 회전 궤도
            for (arm in 0 until 3) {
                val path = Path()
                for (step in 0..140) {
                    val t = step / 140f
                    val theta = t * 5.5f + arm * (2f * PI.toFloat() / 3f) + angle * 0.017453f
                    val r = size.minDimension * 0.48f * t
                    val x = cx + r * cos(theta)
                    val y = cy + r * sin(theta) * 0.6f
                    if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path,
                    brush = Brush.horizontalGradient(listOf(NebulaC.copy(alpha = 0f), NebulaC.copy(alpha = 0.5f), NebulaC.copy(alpha = 0f))),
                    style = Stroke(width = 2.2f * density)
                )
            }
        }
    }
}

/** 매트릭스 레인: 위에서 떨어지는 글리치 문자열 */
@Composable
fun MatrixRainAnimation() {
    val infinite = rememberInfiniteTransition(label = "matrix")
    val scroll by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = androidx.compose.animation.core.LinearEasing)),
        label = "matrix_scroll"
    )
    val glyphs = remember {
        listOf(
            "0", "1", "ｦ", "ｧ", "ｨ", "ﾊ", "ﾋ", "ﾑ", "ﾒ", "ｱ", "ｲ", "ｳ", "ｴ", "ｵ", "ｶ", "ｷ",
            "ﾊ", "ﾋ", "ﾎ", "!", "#", "$", "%", "&", "*", "+", "-", "=", "~", "^", "?", "@"
        )
    }
    val random = remember { Random(7) }
    val columns = remember {
        val count = 34
        List(count) {
            val speed = 0.4f + random.nextFloat() * 1.1f
            val offset = random.nextFloat()
            Triple(random.nextFloat(), speed, offset)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Slate950)
            val colWidth = size.width / columns.size
            // 텍스트는 Canvas 텍스트 드로잉 대신 간단한 블록으로 표현하여 성능을 유지
            columns.forEachIndexed { colIndex, (_, speed, offset) ->
                val x = colIndex * colWidth
                val charsPerCol = 16
                for (row in 0 until charsPerCol) {
                    val baseY = row / charsPerCol.toFloat()
                    val phase = (baseY + scroll * speed + offset) % 1.0f
                    val y = phase * size.height
                    val alpha = ((1.0f - baseY).coerceIn(0.05f, 1f))
                    val glyph = glyphs[(colIndex + row * 7) % glyphs.size]
                    val blockSize = colWidth * 0.42f
                    val selected = (colIndex + row) % 5 == 0
                    val color = if (selected) MatrixC else if (row % 3 == 0) MatrixB else MatrixA
                    // 블록 형태의 글리치 텍스트 근사
                    drawRect(
                        color = color.copy(alpha = alpha * (if (selected) 0.95f else 0.55f)),
                        topLeft = Offset(x + colWidth * 0.25f, y),
                        size = Size(blockSize, blockSize * 0.62f)
                    )
                }
                // 선두의 밝은 헤드
                val headY = (((scroll * speed + offset) % 1.0f) * size.height).coerceIn(0f, size.height - 8f)
                drawRect(
                    color = MatrixC.copy(alpha = 0.9f),
                    topLeft = Offset(x + colWidth * 0.2f, headY),
                    size = Size(colWidth * 0.6f, 5f * density)
                )
            }
            // 중앙 페이드 오버레이
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(MatrixB.copy(alpha = 0.30f), Color.Transparent, MatrixB.copy(alpha = 0.30f))
                )
            )
        }
    }
}

/** 심해: 사인파 물결 + 부유 입자 */
@Composable
fun AbyssAnimation() {
    val infinite = rememberInfiniteTransition(label = "abyss")
    val wave by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(5200, easing = androidx.compose.animation.core.LinearEasing)),
        label = "abyss_wave"
    )
    val glow by infinite.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(4200, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse),
        label = "abyss_glow"
    )
    val random = remember { Random(101) }
    val bubbles = remember { List(40) { Triple(random.nextFloat(), random.nextFloat(), 0.3f + random.nextFloat()) } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(AbyssB.copy(alpha = 0.6f), AbyssA.copy(alpha = 0.35f), Slate950)
                )
            )
            // 수중 광선
            for (i in 0 until 5) {
                val fx = 0.1f + i * 0.2f
                val sway = sin(wave + i).dp
                drawLine(
                    color = AbyssC.copy(alpha = 0.12f * glow),
                    start = Offset(size.width * fx, 0f),
                    end = Offset(size.width * (fx + 0.08f) + sway.toPx(), size.height * 0.8f),
                    strokeWidth = (14 + i * 6).dp.toPx()
                )
            }
            // 물결 3겹
            for (layer in 0 until 3) {
                val amplitude = (18 + layer * 12).dp.toPx()
                val yBase = size.height * (0.45f + layer * 0.16f)
                val path = Path()
                for (x in 0..size.width.toInt() step 8) {
                    val y = yBase + amplitude * sin(x / size.width * (6 + layer * 2).toFloat() + wave + layer * 1.3f)
                    if (x == 0) path.moveTo(x.toFloat(), y) else path.lineTo(x.toFloat(), y)
                }
                drawPath(
                    path,
                    color = if (layer % 2 == 0) AbyssC.copy(alpha = 0.30f - layer * 0.08f) else AbyssA.copy(alpha = 0.28f - layer * 0.07f),
                    style = Stroke(width = 2.4f.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            // 부유 기포
            bubbles.forEach { (fx, fy, scale) ->
                val bx = fx * size.width + sin(wave + fx * 10f) * 12f
                val by = ((fy + wave * 0.05f) % 1f) * size.height
                drawCircle(
                    color = AbyssC.copy(alpha = 0.28f),
                    radius = scale * 7f.dp.toPx(),
                    center = Offset(bx, by)
                )
            }
        }
    }
}

/** 프리즘: 회전 다각형 + 분산 스펙트럼 */
@Composable
fun PrismAnimation() {
    val infinite = rememberInfiniteTransition(label = "prism")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(18000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "prism_rot"
    )
    val spread by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse),
        label = "prism_spread"
    )
    val spectrum = listOf(
        Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFFACC15),
        Color(0xFF22C55E), Color(0xFF3B82F6), Color(0xFF8B5CF6)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Slate950)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val radius = size.minDimension * 0.32f
            // 입사 광선 (흰색) → 다각형 → 분산 스펙트럼
            drawLine(
                color = Color.White.copy(alpha = 0.5f),
                start = Offset(0f, cy - radius * 1.4f),
                end = Offset(cx, cy),
                strokeWidth = 5f.dp.toPx(),
                cap = StrokeCap.Round
            )
            rotate(rotation, Offset(cx, cy)) {
                drawPolygonOutlines(
                    cx = cx, cy = cy, radius = radius,
                    sides = 3,
                    colors = listOf(PrismB),
                    width = 3f.dp.toPx()
                )
                drawPolygonOutlines(
                    cx = cx, cy = cy, radius = radius * 0.72f,
                    sides = 6,
                    colors = listOf(PrismA),
                    width = 2.4f.dp.toPx()
                )
            }
            // 분산된 무지개 선들
            spectrum.forEachIndexed { index, color ->
                val offset = (index - spectrum.size / 2f) * spread * 18f.dp.toPx()
                drawLine(
                    color = color.copy(alpha = 0.65f),
                    start = Offset(cx + radius * 0.5f, cy + offset * 0.2f),
                    end = Offset(size.width, cy + offset * 2.2f),
                    strokeWidth = 4f.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/** 불꽃: 주기적으로 터지는 폭죽 */
@Composable
fun FireworksAnimation() {
    // 결정론적 시드 → 매 프레임 동일
    var frame by remember { mutableStateOf(0) }
    val random = remember { Random(88) }
    val bursts = remember {
        List(5) {
            val bx = 0.2f + random.nextFloat() * 0.6f
            val by = 0.18f + random.nextFloat() * 0.4f
            val phase = random.nextInt(60)
            val hue = random.nextInt(360).toFloat()
            Triple(bx, by, phase) to hue
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            frame = (frame + 1) % 100
            delay(40L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Slate950)
            val cycle = 60
            bursts.forEach { (info, hue) ->
                val (bx, by, phase) = info
                val localFrame = (frame - phase).mod(cycle)
                val progress = localFrame / cycle.toFloat()
                val cx = size.width * bx
                val cy = size.height * by
                // 폭죽 궤적
                if (progress < 0.32f) {
                    val t = progress / 0.32f
                    drawLine(
                        color = FireB.copy(alpha = 0.8f * (1f - t)),
                        start = Offset(cx, size.height * 0.95f),
                        end = Offset(cx, cy - (1f - t) * size.height * 0.3f),
                        strokeWidth = 2.6f * density,
                        cap = StrokeCap.Round
                    )
                } else {
                    val spreadT = ((progress - 0.32f) / 0.68f).coerceIn(0f, 1f)
                    val maxR = size.minDimension * 0.38f * spreadT
                    val fade = (1f - spreadT).coerceIn(0f, 1f)
                    val rayCount = 14
                    for (ray in 0 until rayCount) {
                        val ang = (ray / rayCount.toFloat()) * 2f * PI.toFloat() + hue
                        val x = cx + maxR * cos(ang)
                        val y = cy + maxR * sin(ang)
                        val color = Color.hsl(
                            (hue + ray * 4f).mod(360f),
                            0.85f,
                            0.62f
                        )
                        drawLine(
                            color = color.copy(alpha = fade * 0.9f),
                            start = Offset(cx, cy),
                            end = Offset(x, y),
                            strokeWidth = 2.6f * density,
                            cap = StrokeCap.Round
                        )
                    }
                    // 폭발 중심 섬광
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(FireB.copy(alpha = fade * 0.8f), Color.Transparent),
                            center = Offset(cx, cy),
                            radius = maxR * 0.6f
                        ),
                        radius = maxR * 0.6f,
                        center = Offset(cx, cy)
                    )
                }
            }
            // 화면 하단 잔광
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, FireA.copy(alpha = 0.14f))
                )
            )
        }
    }
}

/** 블랙홀: 소용돌이 빨림 + 렌즈링 */
@Composable
fun BlackHoleAnimation() {
    val infinite = rememberInfiniteTransition(label = "blackhole")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(11000, easing = androidx.compose.animation.core.LinearEasing)),
        label = "bh_rot"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2600, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bh_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color.Black)
            val cx = size.width / 2f
            val cy = size.height / 2f
            val horizon = size.minDimension * 0.55f * pulse

            // 중력렌즈 왜곡 느낌의 밝은 링
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.Transparent, HoleA.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = horizon * 1.4f
                ),
                radius = horizon * 1.4f,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = HoleC.copy(alpha = 0.9f),
                radius = horizon,
                center = Offset(cx, cy),
                style = Stroke(width = 3.4f.dp.toPx())
            )
            // 빨려 들어가는 나선 물질
            for (arm in 0 until 3) {
                rotate(rotation + arm * 120f, Offset(cx, cy)) {
                    val path = Path()
                    for (step in 0..90) {
                        val t = step / 90f
                        val theta = t * 4.2f
                        val r = horizon * (1f - t * 0.92f)
                        val x = cx + r * cos(theta)
                        val y = cy + r * sin(theta)
                        if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(
                        path,
                        color = if (arm % 2 == 0) HoleB.copy(alpha = 0.65f) else HoleA.copy(alpha = 0.65f),
                        style = Stroke(width = 2.4f.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
            // 어두운 코어
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.Black, Color.Transparent),
                    center = Offset(cx, cy),
                    radius = horizon * 0.85f
                ),
                radius = horizon * 0.85f,
                center = Offset(cx, cy)
            )
            // 흡수 accretion disc
            drawCircle(
                color = HoleC.copy(alpha = 0.30f),
                radius = horizon * 1.22f,
                center = Offset(cx, cy),
                style = Stroke(width = 14f.dp.toPx())
            )
        }
    }
}

/** 정다각형 외곽선 여러 개를 한 번에 그립니다 */
private fun DrawScope.drawPolygonOutlines(
    cx: Float,
    cy: Float,
    radius: Float,
    sides: Int,
    colors: List<Color>,
    width: Float
) {
    val path = Path()
    for (i in 0..sides) {
        val ang = (i / sides.toFloat()) * 2f * PI.toFloat() - PI.toFloat() / 2f
        val x = cx + radius * cos(ang)
        val y = cy + radius * sin(ang)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    colors.forEachIndexed { idx, color ->
        drawPath(
            path,
            color = color.copy(alpha = 0.9f - idx * 0.1f),
            style = Stroke(width = width, join = StrokeJoin.Round)
        )
    }
}