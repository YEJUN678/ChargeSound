package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.R
import com.example.data.ChargeSettings
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800

/**
 * 엣지 라이팅 세부 설정 시트.
 *
 * - 투명도(밝기)
 * - 그라데이션 회전 시간
 * - 시작 / 중간 / 끝 색상 (RGB 슬라이더)
 * - 잔량에 따른 자동 색상 사용 여부
 *
 * 모든 변경은 DataStore 에 즉시 저장되며, 아래 "미리보기" 버튼으로 바로 확인할 수 있다.
 */
@Composable
fun EdgeGlowSettingsSheet(
    settings: ChargeSettings,
    onDismiss: () -> Unit,
    onOpacityChange: (Float) -> Unit,
    onSweepChange: (Int) -> Unit,
    onToggleAutoColor: (Boolean) -> Unit,
    onColorStartChange: (Long) -> Unit,
    onColorMidChange: (Long) -> Unit,
    onColorEndChange: (Long) -> Unit,
    onToggleGauge: (Boolean) -> Unit,
    onToggleBreathing: (Boolean) -> Unit,
    onToggleComet: (Boolean) -> Unit,
    onToggleSparks: (Boolean) -> Unit,
    onTogglePill: (Boolean) -> Unit,
    onToggleLiquid: (Boolean) -> Unit,
    onApplyPreset: (Long, Long, Long) -> Unit,
    onPreview: () -> Unit
) {
    SheetScaffold(titleRes = R.string.edge_glow_settings_title, onDismiss = onDismiss) {
        // ── 투명도(밝기) ──
        SettingHeader(
            title = stringResource(R.string.edge_glow_opacity_label),
            value = stringResource(
                R.string.edge_glow_opacity_value,
                (settings.edgeGlowOpacity * 100).toInt()
            )
        )
        Slider(
            value = settings.edgeGlowOpacity,
            onValueChange = onOpacityChange,
            valueRange = 0.15f..1f,
            colors = SliderDefaults.colors(
                thumbColor = ElectricCyan,
                activeTrackColor = ElectricCyan,
                inactiveTrackColor = Slate700
            )
        )
        Text(
            text = stringResource(R.string.edge_glow_opacity_sub),
            style = MaterialTheme.typography.labelSmall,
            color = Slate600
        )

        Spacer(Modifier.height(18.dp))

        // ── 회전 속도 ──
        SettingHeader(
            title = stringResource(R.string.edge_glow_sweep_label),
            value = stringResource(
                R.string.edge_glow_sweep_value,
                settings.edgeGlowSweepMs / 1000f
            )
        )
        Slider(
            value = settings.edgeGlowSweepMs.toFloat(),
            onValueChange = { onSweepChange(it.toInt()) },
            valueRange = 1_500f..15_000f,
            colors = SliderDefaults.colors(
                thumbColor = ElectricCyan,
                activeTrackColor = ElectricCyan,
                inactiveTrackColor = Slate700
            )
        )
        Text(
            text = stringResource(R.string.edge_glow_sweep_sub),
            style = MaterialTheme.typography.labelSmall,
            color = Slate600
        )

        Spacer(Modifier.height(18.dp))
        HorizontalDivider(color = Slate800)
        Spacer(Modifier.height(10.dp))

        // ── 색상 프리셋 ──
        Text(
            text = stringResource(R.string.edge_glow_palette_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        GlowPresetRow(
            presets = GLOW_PRESETS.take(3),
            settings = settings,
            onApplyAuto = { onToggleAutoColor(true) },
            onApplyPreset = onApplyPreset
        )
        Spacer(Modifier.height(8.dp))
        GlowPresetRow(
            presets = GLOW_PRESETS.drop(3),
            settings = settings,
            onApplyAuto = { onToggleAutoColor(true) },
            onApplyPreset = onApplyPreset
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Slate800)
        Spacer(Modifier.height(10.dp))

        // ── 연출 ──
        Text(
            text = stringResource(R.string.edge_glow_effects_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        ToggleRow(
            title = stringResource(R.string.edge_glow_gauge_title),
            subtitle = stringResource(R.string.edge_glow_gauge_sub),
            checked = settings.edgeGlowGauge,
            onCheckedChange = onToggleGauge
        )
        ToggleRow(
            title = stringResource(R.string.edge_glow_breathing_title),
            subtitle = stringResource(R.string.edge_glow_breathing_sub),
            checked = settings.edgeGlowBreathing,
            onCheckedChange = onToggleBreathing
        )
        ToggleRow(
            title = stringResource(R.string.edge_glow_comet_title),
            subtitle = stringResource(R.string.edge_glow_comet_sub),
            checked = settings.edgeGlowComet,
            onCheckedChange = onToggleComet
        )
        ToggleRow(
            title = stringResource(R.string.edge_glow_sparks_title),
            subtitle = stringResource(R.string.edge_glow_sparks_sub),
            checked = settings.edgeGlowSparks,
            onCheckedChange = onToggleSparks
        )
        ToggleRow(
            title = stringResource(R.string.edge_glow_pill_title),
            subtitle = stringResource(R.string.edge_glow_pill_sub),
            checked = settings.edgeGlowPill,
            onCheckedChange = onTogglePill
        )
        ToggleRow(
            title = stringResource(R.string.edge_glow_liquid_title),
            subtitle = stringResource(R.string.edge_glow_liquid_sub),
            checked = settings.edgeGlowLiquid,
            onCheckedChange = onToggleLiquid
        )

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Slate800)
        Spacer(Modifier.height(6.dp))

        ToggleRow(
            title = stringResource(R.string.edge_glow_auto_color_title),
            subtitle = stringResource(R.string.edge_glow_auto_color_sub),
            checked = settings.edgeGlowAutoColor,
            onCheckedChange = onToggleAutoColor
        )

        if (settings.edgeGlowAutoColor) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.edge_glow_auto_color_note),
                style = MaterialTheme.typography.labelSmall,
                color = ElectricAmber
            )
        }

        Spacer(Modifier.height(14.dp))

        EdgeColorEditor(
            title = stringResource(R.string.edge_glow_color_start),
            argb = settings.edgeGlowColorStart,
            onChange = onColorStartChange
        )
        Spacer(Modifier.height(14.dp))
        EdgeColorEditor(
            title = stringResource(R.string.edge_glow_color_mid),
            argb = settings.edgeGlowColorMid,
            onChange = onColorMidChange
        )
        Spacer(Modifier.height(14.dp))
        EdgeColorEditor(
            title = stringResource(R.string.edge_glow_color_end),
            argb = settings.edgeGlowColorEnd,
            onChange = onColorEndChange
        )

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = onPreview,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.edge_glow_preview),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun GlowPresetRow(
    presets: List<GlowPreset>,
    settings: ChargeSettings,
    onApplyAuto: () -> Unit,
    onApplyPreset: (Long, Long, Long) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { preset ->
            val selected = if (preset.auto) {
                settings.edgeGlowAutoColor
            } else {
                !settings.edgeGlowAutoColor &&
                    settings.edgeGlowColorStart == preset.start &&
                    settings.edgeGlowColorMid == preset.mid &&
                    settings.edgeGlowColorEnd == preset.end
            }

            Surface(
                color = if (selected) ElectricCyan.copy(alpha = 0.16f) else Slate800,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (selected) ElectricCyan else Slate700),
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        if (preset.auto) {
                            onApplyAuto()
                        } else {
                            onApplyPreset(
                                preset.start ?: 0L,
                                preset.mid,
                                preset.end
                            )
                        }
                    }
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (preset.auto) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            listOf(preset.start ?: 0L, preset.mid, preset.end).forEach { argb ->
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .clip(CircleShape)
                                        .background(Color(argb.toInt()))
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = stringResource(preset.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color.White else Slate400,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * 프리셋 목록. `auto` 는 잔량 팔레트를 쓰고, 나머지는 시작/중간/끝 색을 지정한다.
 */
private data class GlowPreset(
    val labelRes: Int,
    val start: Long? = null,
    val mid: Long = 0L,
    val end: Long = 0L,
    val auto: Boolean = false
)

private val GLOW_PRESETS = listOf(
    GlowPreset(R.string.edge_glow_preset_auto, auto = true),
    GlowPreset(R.string.edge_glow_preset_aurora, 0xFF22D3EEL, 0xFF4ADE80L, 0xFFA78BFAL),
    GlowPreset(R.string.edge_glow_preset_sunset, 0xFFF97316L, 0xFFFB7185L, 0xFFFBBF24L),
    GlowPreset(R.string.edge_glow_preset_mint, 0xFF34D399L, 0xFF22D3EEL, 0xFFA7F3D0L),
    GlowPreset(R.string.edge_glow_preset_cyber, 0xFF22D3EEL, 0xFF6366F1L, 0xFFF472B6L),
    GlowPreset(R.string.edge_glow_preset_blossom, 0xFFF472B6L, 0xFFFBCFE8L, 0xFFFDE68AL)
)

@Composable
private fun SettingHeader(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = ElectricCyan
        )
    }
}

/**
 * 시작/중간/끝 색상 하나를 편집하는 블록.
 * 색상 미리보기 + HEX 코드 + R/G/B 슬라이더로 구성됩니다.
 */
@Composable
private fun EdgeColorEditor(
    title: String,
    argb: Long,
    onChange: (Long) -> Unit
) {
    val color = Color(argb.toInt())

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = String.format("#%06X", 0xFFFFFF and argb.toInt()),
                style = MaterialTheme.typography.labelSmall,
                color = Slate400
            )
        }
        Spacer(Modifier.height(4.dp))
        ColorChannelSlider(
            label = stringResource(R.string.edge_glow_channel_red),
            value = color.red,
            color = Color(0xFFFF5A5F),
            onValueChange = { red -> onChange(argbOf(red, color.green, color.blue)) }
        )
        ColorChannelSlider(
            label = stringResource(R.string.edge_glow_channel_green),
            value = color.green,
            color = Color(0xFF42D392),
            onValueChange = { green -> onChange(argbOf(color.red, green, color.blue)) }
        )
        ColorChannelSlider(
            label = stringResource(R.string.edge_glow_channel_blue),
            value = color.blue,
            color = Color(0xFF5BA8FF),
            onValueChange = { blue -> onChange(argbOf(color.red, color.green, blue)) }
        )
    }
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Float,
    color: Color,
    onValueChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Slate400,
            modifier = Modifier.width(16.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = Slate700
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
