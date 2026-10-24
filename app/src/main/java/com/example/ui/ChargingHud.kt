package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AppShapes
import com.example.ui.theme.AppText
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.Motion
import com.example.ui.theme.Spacing
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 충전 애니메이션 화면 전용 HUD 컴포넌트.
 *
 * ## 왜 별도 파일인가
 * 이 화면은 의도적으로 "몰입형"입니다. 애니메이션이 배경을 다 채우고,
 * 텍스트는 그 위에 얹히는 얇은 유리 배지 형태입니다. 메인 화면의 M3 표면
 * 규칙(레벨, 컨테이너 색)을 그대로 쓰면 애니메이션 위에서 층이 생겨
 * 화면이 더 복잡해집니다.
 *
 * 하지만 **규칙 자체는 동일**합니다 — 하나의 모양, 하나의 불투명도, 하나의
 * 테두리 두께. 이 파일이 그 규칙의 한 벌이고, 화면 안의 모든 배지는 이것만
 * 씁니다. 그래서 모서리 4종·알파 3종이던 것이 각각 1종으로 줄었습니다.
 */

/** 유리 배지의 배경. 애니메이션 위에서도 글자가 읽히는 최소 대비를 확보합니다. */
private val HudScrim = Color.Black.copy(alpha = 0.55f)

/** 배지 테두리 불투명도. 색은 강조색에서 뽑지만 농도는 항상 이 값 하나로. */
private const val HudBorderAlpha = 0.32f

/** HUD 가 화면 좌우 끝에서 물러나는 거리. 상단/하단 HUD 가 같은 선에 서게 합니다. */
internal val HudPad = Spacing.xl

/**
 * HUD 배지 하나.
 *
 * [tint] 로 강조색을 다르게 — 예: 충전기는 시안, 무한 반복은 그린.
 */
@Composable
fun ChargingHudChip(
    text: String,
    icon: ImageVector? = null,
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = HudScrim,
        shape = AppShapes.pill,
        border = BorderStroke(1.dp, tint.copy(alpha = HudBorderAlpha)),
        contentColor = tint
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Spacing.md,
                vertical = Spacing.sm
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(Spacing.sm))
            }
            Text(
                text = text,
                style = AppText.Badge,
                color = tint
            )
        }
    }
}

/**
 * 닫기 버튼.
 *
 * 손으로 누르는 영역은 최소 48dp를 확보합니다 — 시각적으로는 조금 작아도
 * 실제 터치 영역은 접근성 기준에 맞게 넓게 둡니다.
 *
 * 눌렀을 때 작아지는 반응이 없습니다. 기본 물결(`indication`) 대신 **스케일로**
 * 표시하는 이유: 이 화면은 어두운 애니메이션 위에 떠 있는 반투명 원이라,
 * 원형 물결이 배경과 섞이면 눌렸는지 알기가 어렵습니다. 도장(印章)이 눌리는
 * 느낌으로 처리합니다.
 */
@Composable
fun HudCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = 700f
        ),
        label = "close_press"
    )

    Box(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(AppShapes.pill)
            .background(HudScrim)
            .border(1.dp, ElectricCyan.copy(alpha = HudBorderAlpha), AppShapes.pill)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(R.string.a11y_close),
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** 충전 오버레이에서도 바로 앱의 설정·상태 화면으로 들어가는 버튼. */
@Composable
fun HudOpenAppButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(AppShapes.pill)
            .background(HudScrim)
            .border(1.dp, ElectricCyan.copy(alpha = HudBorderAlpha), AppShapes.pill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = stringResource(R.string.a11y_open_app),
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** 상단 — 충전기 종류. */
@Composable
fun ChargingPlugChip(plugName: String, modifier: Modifier = Modifier) {
    ChargingHudChip(
        text = plugName,
        icon = Icons.Default.Bolt,
        tint = ElectricCyan,
        modifier = modifier
    )
}

/**
 * 하단 — 자동 종료까지 남은 시간.
 *
 * 마지막 5초부터 앰버로 바꿉니다. 종료 전환을 넣은 뒤로 "언제 닫히는지"가
 * 예전보다 덜 확실해졌는데, 숫자만이 아니라 **색**으로도 남은 시간을 알 수 있게 합니다.
 */
@Composable
fun ChargingCountdownChip(
    text: String,
    seconds: Int,
    modifier: Modifier = Modifier
) {
    val urgent = seconds in 1..5
    ChargingHudChip(
        text = text,
        icon = Icons.Default.Timer,
        tint = if (urgent) ElectricAmber else ElectricCyan,
        modifier = modifier
    )
}

/** 하단 — 무한 반복 안내. */
@Composable
fun ChargingLoopChip(text: String, modifier: Modifier = Modifier) {
    ChargingHudChip(
        text = text,
        icon = Icons.Default.Loop,
        tint = ElectricGreen,
        modifier = modifier
    )
}

/**
 * 중앙 표시 아래에 오는 충전 상태 줄.
 *
 * ## 왜 "FAST CHARGING" 같은 문구를 넣지 않는가
 * 예전에는 충전 속도와 무관하게 "FAST CHARGING"을 고정 표시했습니다.
 * 5W 충전기로 꽂아도 같은 문구가 나오는, 그 자체로 거짓인 안내였고
 * 번역 파일에도 없어서 영문으로만 나왔습니다.
 *
 * 이제는 **실제로 측정된 값만** 보여줍니다.
 * - 상승률을 잴 수 있으면 → 시간당 상승률
 * - 잴 수 없으면 → 문구 없이 침묵. 측정 중이면 아주 흐리게 힌트만.
 *
 * 아무것도 모르는 순간에 틀린 정보를 보여주는 것보다 아무것도 안 보여주는 게 낫습니다.
 *
 * ## 왜 전환을 걸었는가
 * 상태 줄은 화면이 뜨고 30초~1분 뒤에야 첫 숫자가 나옵니다. 그 전까지는
 * "측정 중" 이었다가 자릿수가 사라집니다. 매번 `if` 로 갈아 끼우면
 * 눈이 깜빡이고, 특히 `%` 가 사라지는 순간이 밋밋합니다.
 * 그래서 **위로 접히며 사라지고 아래에서 새로 올라오도록** 바꿨습니다.
 */
@Composable
fun ChargingStatusLine(
    percentPerHour: Float?,
    measuring: Boolean,
    isFull: Boolean = false,
    modifier: Modifier = Modifier
) {
    val rate = percentPerHour?.takeIf { it > 0.05f }

    AnimatedContent(
        targetState = StatusLineState.of(rate, measuring, isFull),
        transitionSpec = {
            // 아래에서 올라오며 나타남 / 위로 접히며 사라짐 — 내용은 아래에서 온다
            (slideInVertically(animationSpec = tween(Motion.MEDIUM_MILLIS)) { height -> height } +
                fadeIn(tween(Motion.MEDIUM_MILLIS)))
                .togetherWith(
                    slideOutVertically(animationSpec = tween(Motion.FAST_MILLIS)) { height -> -height } +
                        fadeOut(tween(Motion.FAST_MILLIS))
                )
        },
        modifier = modifier,
        label = "status_line"
    ) { state ->
        when (state) {
            is StatusLineState.Rate -> StatusChip(
                text = stringResource(R.string.session_rate, formatRate(state.value)),
                tint = ElectricAmber
            )

            is StatusLineState.Full -> StatusChip(
                text = stringResource(R.string.anim_full_label),
                icon = Icons.Default.CheckCircle,
                tint = ElectricGreen
            )

            StatusLineState.Measuring -> Text(
                text = stringResource(R.string.anim_rate_measuring),
                style = AppText.Axis,
                color = Color.White.copy(alpha = 0.38f)
            )

            StatusLineState.Silent -> Unit
        }
    }
}

/**
 * 상태 줄의 세 상태를 하나의 값으로.
 *
 * `AnimatedContent` 는 대상 값이 `equals` 로 비교되므로, 부동소수점을 그대로
 * 넣으면 애니메이션 트리거가 10초마다 새 값으로 새지워지며 두 값이 겹쳐 보입니다.
 * 여기서 의미 있는 상태로만 뭉개서 비교합니다.
 */
private sealed interface StatusLineState {
    data class Rate(val value: Float) : StatusLineState
    data object Full : StatusLineState
    data object Measuring : StatusLineState
    data object Silent : StatusLineState

    companion object {
        fun of(rate: Float?, measuring: Boolean, isFull: Boolean): StatusLineState = when {
            isFull -> Full
            rate != null -> Rate(rate)
            measuring -> Measuring
            else -> Silent
        }
    }
}

/** 상태 줄의 강조 배지. [ChargingHudChip] 보다 옅은 톤이라 중앙 표시를 압도하지 않습니다. */
@Composable
private fun StatusChip(
    text: String,
    tint: Color,
    icon: ImageVector? = null
) {
    Surface(
        color = tint.copy(alpha = 0.16f),
        shape = AppShapes.pill,
        contentColor = tint
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = Spacing.md,
                vertical = Spacing.xs
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(Spacing.xs))
            }
            Text(text = text, style = AppText.Badge)
        }
    }
}

/**
 * HUD 진입 전환.
 *
 * 배경이 먼저 떠오른 뒤, HUD 요소들이 위·가운데·아래 순으로 아주 짧은 간격을 두고
 * 올라옵니다. 전부 동시에 뜨면 화면이 "떠오르는" 게 아니라 "이미 정지해 있던" 것처럼
 * 보입니다 — 사용자가 눈을 떴을 때 이미 전부 진행된 경우가 그렇기 때문입니다.
 *
 * [fromTop] 이 true 면 위에서, false 면 아래에서 내려옵니다.
 */
@Composable
fun Modifier.hudEntrance(
    delayMillis: Int,
    fromTop: Boolean = false
): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = Motion.SLOW_MILLIS,
                easing = FastOutSlowInEasing
            )
        )
    }
    return this.graphicsLayer {
        alpha = progress.value
        // 남은 거리 24dp — 크지 않아야 "떠오르는" 것이 아니라 "미끄러져 올라온" 느낌
        val travel = 24.dp.toPx()
        translationY = if (fromTop) -(1f - progress.value) * travel
        else (1f - progress.value) * travel
    }
}

/**
 * 종료 전환.
 *
 * 예전에는 누르는 즉시 `finish()` 라 화면이 그냥 사라졌습니다. 이제는
 * 살짝 줄며 사라진 다음 실제로 종료됩니다 — 되감기(undo) 직후처럼 화면이
 * 갑자기 증발하지 않으려면 이 0.2초가 필요합니다.
 *
 * 소리도 같은 시점에 페이드 아웃됩니다 (호출 측에서 처리).
 */
class ExitTransition {
    internal val alpha = Animatable(1f)
    internal val scale = Animatable(1f)

    /**
     * 두 값을 **동시에** 움직입니다.
     *
     * 순차로 await 하면 전체 시간이 두 배가 됩니다. 알파가 이미 0 인 0.2초를
     * 아무도 보지 못한 채 기다리는 셈이고, 그동안 화면은 이미 사라진 채로
     * `setCoreFeatureBusy(true)` 가 풀리지 않아 광고가 계속 막힙니다.
     */
    suspend fun run() {
        val spec = tween<Float>(
            durationMillis = Motion.MEDIUM_MILLIS,
            easing = FastOutSlowInEasing
        )
        coroutineScope {
            launch { alpha.animateTo(0f, spec) }
            launch {
                scale.animateTo(
                    0.94f,
                    tween(Motion.MEDIUM_MILLIS, easing = FastOutSlowInEasing)
                )
            }
        }
    }
}

/** HUD 가 차지하는 세 구간의 진입 지연값 (ms). */
internal object HudStagger {
    /** 상단 — 충전기 / 닫기 */
    const val TOP = 70

    /** 중앙 — 배터리 링 */
    const val CENTER = 120

    /** 하단 — 안내 문구 */
    const val BOTTOM = 180
}
