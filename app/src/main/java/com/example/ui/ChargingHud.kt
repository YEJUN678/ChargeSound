package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AppShapes
import com.example.ui.theme.AppText
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.Spacing

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
 */
@Composable
fun HudCloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(AppShapes.pill)
            .background(HudScrim)
            .border(1.dp, ElectricCyan.copy(alpha = HudBorderAlpha), AppShapes.pill)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
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

/** 하단 — 자동 종료까지 남은 시간. */
@Composable
fun ChargingCountdownChip(text: String, modifier: Modifier = Modifier) {
    ChargingHudChip(
        text = text,
        icon = Icons.Default.Timer,
        tint = ElectricCyan,
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
 */
@Composable
fun ChargingStatusLine(
    percentPerHour: Float?,
    measuring: Boolean,
    modifier: Modifier = Modifier
) {
    val label = if (percentPerHour != null && percentPerHour > 0.05f) {
        stringResource(R.string.session_rate, formatRate(percentPerHour))
    } else {
        null
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            label != null -> {
                // 실제 숫자가 있으면 조금 더 눈에 띄게 (강조색 + 옅은 배경)
                Surface(
                    color = ElectricAmber.copy(alpha = 0.16f),
                    shape = AppShapes.pill,
                    contentColor = ElectricAmber
                ) {
                    Text(
                        text = label,
                        style = AppText.Badge,
                        modifier = Modifier.padding(
                            horizontal = Spacing.md,
                            vertical = Spacing.xs
                        )
                    )
                }
            }
            // 측정 중인데 아직 숫자가 없음 — "왜 안 뜨지" 하는 혼란만 막고,
            // 시선을 끌지 않도록 가장 흐리게 둡니다.
            measuring -> {
                Text(
                    text = stringResource(R.string.anim_rate_measuring),
                    style = AppText.Axis,
                    color = Color.White.copy(alpha = 0.38f)
                )
            }
        }
    }
}