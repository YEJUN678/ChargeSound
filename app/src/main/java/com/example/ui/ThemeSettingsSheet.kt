package com.example.ui

import android.os.Build

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ChargeSettings
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.Spacing
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800

/** 강조색 프리셋 선택 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsSheet(
    settings: ChargeSettings,
    onDismiss: () -> Unit,
    onSelectAccent: (String) -> Unit,
    onCustomAccentChange: (Long) -> Unit = {},
    fontScale: Float = 1.0f,
    onSelectFontScale: (Float) -> Unit = {},
    dynamicColor: Boolean = false,
    onToggleDynamicColor: (Boolean) -> Unit = {},
    highContrast: Boolean = false,
    onToggleHighContrast: (Boolean) -> Unit = {}
) {
    SheetScaffold(titleRes = R.string.menu_theme, onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.accent_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.accent_sub),
            style = MaterialTheme.typography.labelSmall,
            color = Slate400
        )

        Spacer(Modifier.height(16.dp))

        AccentPalette.ALL.forEach { palette ->
            val key = AccentPalette.keyOf(palette)
            val selected = settings.accentKey == key
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (selected) Slate800 else Color.Transparent)
                    .border(
                        width = 1.dp,
                        color = if (selected) palette.cyan else Slate700,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelectAccent(key) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 미리보기: 세 가지 색을 나란히 보여줍니다.
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(palette.cyan, palette.amber, palette.green).forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(c)
                        )
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = stringResource(accentLabelRes(key)),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) Color.White else Slate400,
                    modifier = Modifier.weight(1f)
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = palette.cyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.md))
        val customColor = Color(settings.customAccentArgb.toInt())
        val customSelected = settings.accentKey == "custom"
        Surface(
            color = if (customSelected) customColor.copy(alpha = 0.16f) else Slate800,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (customSelected) customColor else Slate700),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(customColor)
                            .border(2.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.accent_custom_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.accent_custom_sub),
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate400
                        )
                    }
                    Text(
                        text = String.format("#%06X", 0xFFFFFF and settings.customAccentArgb.toInt()),
                        style = MaterialTheme.typography.labelSmall,
                        color = customColor
                    )
                }
                Spacer(Modifier.height(8.dp))
                ColorChannelSlider(
                    label = stringResource(R.string.accent_red),
                    value = customColor.red,
                    color = Color(0xFFFF5A5F),
                    onValueChange = { red ->
                        onCustomAccentChange(argbOf(red, customColor.green, customColor.blue))
                    }
                )
                ColorChannelSlider(
                    label = stringResource(R.string.accent_green),
                    value = customColor.green,
                    color = Color(0xFF42D392),
                    onValueChange = { green ->
                        onCustomAccentChange(argbOf(customColor.red, green, customColor.blue))
                    }
                )
                ColorChannelSlider(
                    label = stringResource(R.string.accent_blue),
                    value = customColor.blue,
                    color = Color(0xFF5BA8FF),
                    onValueChange = { blue ->
                        onCustomAccentChange(argbOf(customColor.red, customColor.green, blue))
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.accent_apply_note),
            style = MaterialTheme.typography.labelSmall,
            color = Slate600,
            lineHeight = 16.sp
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.font_scale_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = stringResource(R.string.font_scale_sub),
            style = MaterialTheme.typography.labelSmall,
            color = Slate400
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            FONT_SCALES.forEach { scale ->
                val selected = kotlin.math.abs(fontScale - scale) < 0.01f
                Surface(
                    color = if (selected) ElectricCyan else Slate800,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (selected) ElectricCyan else Slate700),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectFontScale(scale) }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${(scale * 100).toInt()}%",
                            color = if (selected) Color.Black else Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.font_scale_note),
            style = MaterialTheme.typography.labelSmall,
            color = Slate600,
            lineHeight = 16.sp
        )

        Spacer(Modifier.height(Spacing.xl))
        HorizontalDivider(color = Slate800)
        Spacer(Modifier.height(Spacing.xl))

        Text(
            text = stringResource(R.string.appearance_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(Modifier.height(Spacing.md))

        // Material You 는 Android 12 미만에서 동작하지 않으므로 hidesDisabledAreas 로 감춥니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ToggleRow(
                title = stringResource(R.string.dynamic_color_title),
                subtitle = stringResource(R.string.dynamic_color_sub),
                checked = dynamicColor,
                onCheckedChange = onToggleDynamicColor
            )
            Spacer(Modifier.height(Spacing.md))
        }

        ToggleRow(
            title = stringResource(R.string.high_contrast_title),
            subtitle = stringResource(R.string.high_contrast_sub),
            checked = highContrast,
            onCheckedChange = onToggleHighContrast
        )
    }
}

/** 글자 크기 배율 프리셋. 시스템 폰트 설정과 무관하게 앱 안에서만 적용됩니다. */
private val FONT_SCALES = listOf(0.9f, 1.0f, 1.15f, 1.3f, 1.5f)

private fun accentLabelRes(key: String): Int = when (key) {
    "magenta" -> R.string.accent_magenta
    "violet" -> R.string.accent_violet
    "lime" -> R.string.accent_lime
    "ember" -> R.string.accent_ember
    else -> R.string.accent_cyan
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Slate400,
            modifier = Modifier.width(18.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Slate700,
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

private fun argbOf(red: Float, green: Float, blue: Float): Long {
    val r = (red.coerceIn(0f, 1f) * 255).toInt()
    val g = (green.coerceIn(0f, 1f) * 255).toInt()
    val b = (blue.coerceIn(0f, 1f) * 255).toInt()
    return (0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong())
}

/**
 * 충전 속도 실시간 배지.
 *
 * "시간당 +12%" 형태로 표시합니다. 측정된 상승률이 없을 때는
 * 아무것도 그리지 않습니다(빈 배지를 만들지 않기 위해).
 *
 * 경과 시간을 함께 보여주는 이유: 충전 시작 직후 몇 분이면 측정된 상승률이
 * "시간당 300%" 처럼 무의미한 값이 나옵니다. 경과 시간 없이는 숫자를 신뢰할 수 없습니다.
 */
@Composable
fun ChargingRateBadge(
    percentPerHour: Float?,
    elapsedMillis: Long,
    modifier: Modifier = Modifier
) {
    if (percentPerHour == null || percentPerHour <= 0.05f) return

    val accent = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Slate800)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(12.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = stringResource(R.string.session_rate, formatRate(percentPerHour)),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        // 첫 1분은 표본이 너무 작아서 숫자를 신뢰할 수 없습니다.
        if (elapsedMillis >= 60_000L) {
            Spacer(Modifier.width(7.dp))
            Text(
                text = "· " + stringResource(R.string.session_elapsed_short, formatElapsed(elapsedMillis)),
                style = MaterialTheme.typography.labelSmall,
                color = Slate600
            )
        }
    }
}

/** "2h 15m" 형태로 경과 시간을 표시 */
internal fun formatElapsed(millis: Long): String {
    val totalMinutes = millis / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

/**
 * 상승률을 숫자만 반환합니다 ("12" 또는 "1.2").
 * 단위는 문자열 리소스가 담당합니다 — languages마다 "/h" 와 "/시간" 이 다르기 때문입니다.
 */
internal fun formatRate(percentPerHour: Float): String {
    return if (percentPerHour >= 10f) {
        percentPerHour.toInt().toString()
    } else {
        // 기기 locale 을 따릅니다 — 소수점 구분자 규칙이 지역마다 다르기 때문.
        String.format(java.util.Locale.getDefault(), "%.1f", percentPerHour)
    }
}
