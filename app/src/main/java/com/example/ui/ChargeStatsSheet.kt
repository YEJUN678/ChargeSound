package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.battery.BatteryInsights
import com.example.data.ChargeRecord
import com.example.ui.theme.AppShapes
import com.example.ui.theme.AppText
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Spacing
import java.util.Calendar
import java.util.Locale

/**
 * 충전 기록 통계.
 *
 * 저장된 [ChargeRecord] 에서 요약 지표와 최근 충전량을 막대 그래프로 보여줍니다.
 *
 * ## 왜 "최근" 통계인가
 * 기록은 최대 100건까지만 유지됩니다([com.example.data.ChargeHistoryRepository]).
 * 따라서 이 화면은 "이 기기를 2년간 썼을 때의 통계"가 아니라
 * "요즘 충전 습관"을 보여주는 용도입니다. 그렇게 해석해야 숫자가 거짓말하지 않습니다.
 */
@Composable
fun ChargeStatsSheet(
    records: List<ChargeRecord>,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    SheetScaffold(titleRes = R.string.menu_stats, onDismiss = onDismiss) {
        if (records.isEmpty()) {
            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(R.string.stats_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = Slate600,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.stats_empty_sub),
                style = MaterialTheme.typography.labelSmall,
                color = Slate700,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))
            return@SheetScaffold
        }

        val recent = records.take(MAX_BARS)
        val avgGain = recent.sumOf { it.gainedPercent } / recent.size
        val avgMinutes = recent.sumOf { it.durationMillis } / recent.size / 60_000L
        val avgRate = recent.mapNotNull { it.percentPerHour }.takeIf { it.isNotEmpty() }?.average()?.toFloat()
        val avgPeakTemp = recent.map { it.peakTempC }.average().toFloat()

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                modifier = Modifier.weight(1f),
                value = stringResource(R.string.stats_total, records.size),
                label = stringResource(R.string.stats_total_label),
                tint = ElectricCyan
            )
            StatTile(
                modifier = Modifier.weight(1f),
                value = stringResource(R.string.stats_avg_gain, avgGain),
                label = stringResource(R.string.stats_avg_gain_label),
                tint = ElectricGreen
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                modifier = Modifier.weight(1f),
                value = formatMinutesShort(avgMinutes),
                label = stringResource(R.string.stats_avg_time_label),
                tint = ElectricAmber
            )
            StatTile(
                modifier = Modifier.weight(1f),
                value = if (avgRate != null) stringResource(R.string.stats_avg_rate, formatRate(avgRate)) else "-",
                label = stringResource(R.string.stats_avg_rate_label),
                tint = ElectricCyan
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                modifier = Modifier.weight(1f),
                value = stringResource(R.string.stats_peak_temp, avgPeakTemp),
                label = stringResource(R.string.stats_peak_temp_label),
                tint = if (avgPeakTemp >= BatteryInsights.HOT_THRESHOLD_C) ElectricAmber else ElectricGreen
            )
            StatTile(
                modifier = Modifier.weight(1f),
                value = stringResource(R.string.stats_alarm_hit, recent.count { it.reachedAlarmTarget }),
                label = stringResource(R.string.stats_alarm_hit_label),
                tint = ElectricCyan
            )
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.stats_chart_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.stats_chart_sub, MAX_BARS),
            style = MaterialTheme.typography.labelSmall,
            color = Slate600
        )
        Spacer(Modifier.height(12.dp))

        ChargeBarChart(records = recent.reversed(), modifier = Modifier.fillMaxWidth())

        // ── 온도 추이 ──────────────────────────────────────────────
        // 데이터는 이미 기록되고 있었는데 여기서 처음 보여줍니다.
        // 충전량이 평균 60% 인 그래프와 달리, 온도는 배터리가 스스로 말하는
        // 수명 지표라서 "요즘 더 뜨거워졌다" 는 눈에 띄는 변화가 있습니다.
        Spacer(Modifier.height(24.dp))
        BatteryInsightsSection(records = recent.reversed())

        Spacer(Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.stats_recent_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))

        recent.take(5).forEach { record ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate900)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = if (record.reachedAlarmTarget) ElectricGreen else Slate600,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(
                            R.string.stats_row_title,
                            record.startLevel,
                            record.endLevel
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = formatRecordDate(record.startedAt) + " · " +
                            formatMinutesShort(record.durationMillis / 60_000L),
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate600
                    )
                }
                record.percentPerHour?.let { rate ->
                    Text(
                        text = formatRate(rate),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCyan
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Slate900)
                .clickable { onClear() }
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.stats_clear),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate400
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.stats_local_note),
            style = MaterialTheme.typography.labelSmall,
            color = Slate700,
            lineHeight = 14.sp
        )
    }
}

private const val MAX_BARS = 14

@Composable
private fun StatTile(
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Slate900)
            .padding(vertical = 13.dp, horizontal = 12.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = tint,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Slate600,
            maxLines = 2,
            lineHeight = 13.sp
        )
    }
}

/**
 * 최근 충전량을 막대 그래프로 그립니다.
 *
 * 높이는 "올라간 잔량(%)"입니다. 시간당 상승률이 아니라 절대량인 이유는
 * 짧은 세션과 긴 세션을 같은 축에 놓으면 시간 정보가 사라지기 때문입니다.
 *
 * 호출 시점에 [records]는 **오래된 순**이어야 합니다 (오른쪽으로 시간이 흐릅니다).
 */
@Composable
private fun ChargeBarChart(
    records: List<ChargeRecord>,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val maxGain = records.maxOfOrNull { it.gainedPercent }?.coerceAtLeast(1) ?: 1

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            records.forEach { record ->
                val fraction = record.gainedPercent.toFloat() / maxGain
                // 최소 높이 4dp — 0% 세션이 완전히 보이지 않는 것을 막습니다.
                val barHeight = (4 + (108 * fraction)).coerceAtMost(108f).dp
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                        .background(if (record.reachedAlarmTarget) ElectricGreen else accent)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.stats_axis_old),
                style = MaterialTheme.typography.labelSmall,
                color = Slate700
            )
            Text(
                text = stringResource(R.string.stats_axis_max, maxGain),
                style = MaterialTheme.typography.labelSmall,
                color = Slate700
            )
            Text(
                text = stringResource(R.string.stats_axis_new),
                style = MaterialTheme.typography.labelSmall,
                color = Slate700
            )
        }
    }
}

/**
 * 온도 추이 + 충전 습관 인사이트.
 *
 * ## 왜 "% 건강도" 대신 이걸 보여주는가
 * Android 는 만충 용량을 공개하지 않고, 공장 설계 용량은 공개 SDK 에 없는
 * `@SystemApi` 입니다. 서드파티 앱이 "SOH 92%" 를 띄우려면 반드시 지어내야 합니다.
 *
 * 대신 이 앱이 **직접 기록한 값**으로만 판단합니다.
 * 온도는 배터리가 스스로 올리는 값이고, 만충 유지 시간과 방전 깊이는
 * 사용자의 습관을 그대로 드러냅니다. 모두 오차 없는 사실입니다.
 *
 * [records] 는 오래된 순이어야 합니다.
 */
@Composable
private fun BatteryInsightsSection(records: List<ChargeRecord>) {
    val trend = remember(records) { BatteryInsights.temperatureTrend(records) }
    val habits = remember(records) { BatteryInsights.habits(records) }

    // ── 온도 추이 ──
    Text(
        text = stringResource(R.string.temp_trend_title),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.temp_trend_sub, records.size),
        style = MaterialTheme.typography.labelSmall,
        color = Slate600
    )
    Spacer(Modifier.height(12.dp))

    if (trend == null) {
        Text(
            text = stringResource(R.string.temp_trend_empty),
            style = MaterialTheme.typography.bodySmall,
            color = Slate700
        )
    } else {
        TemperatureTrendChart(
            records = records.filter { it.peakTempC > 0f },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(
                R.string.temp_trend_avg,
                formatTemp(trend.averageC)
            ),
            style = MaterialTheme.typography.labelSmall,
            color = Slate400
        )
        Spacer(Modifier.height(6.dp))
        // 해석문은 표본이 충분할 때만 방향을 말합니다.
        Text(
            text = trendInterpretation(trend),
            style = MaterialTheme.typography.bodySmall,
            color = when (trend.direction) {
                BatteryInsights.Direction.RISING -> ElectricAmber
                else -> Slate400
            }
        )
    }

    // ── 충전 습관 ──
    Spacer(Modifier.height(22.dp))
    Text(
        text = stringResource(R.string.habits_title),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.habits_sub),
        style = MaterialTheme.typography.labelSmall,
        color = Slate600
    )
    Spacer(Modifier.height(12.dp))

    if (habits == null || habits.sessionCount == 0) {
        Text(
            text = stringResource(R.string.temp_trend_empty),
            style = MaterialTheme.typography.bodySmall,
            color = Slate700
        )
        Spacer(Modifier.height(16.dp))
        WhyNoSohNote()
        return
    }

    // 관측된 비율 세 가지
    HabitBar(
        label = stringResource(R.string.habits_full_ratio, (habits.fullChargeRatio * 100).toInt()),
        fraction = habits.fullChargeRatio,
        tint = ElectricAmber
    )
    HabitBar(
        label = stringResource(R.string.habits_deep_ratio, (habits.deepDischargeRatio * 100).toInt()),
        fraction = habits.deepDischargeRatio,
        tint = ElectricCyan
    )
    HabitBar(
        label = stringResource(R.string.habits_overnight_ratio, (habits.overnightRatio * 100).toInt()),
        fraction = habits.overnightRatio,
        tint = Slate600
    )

    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatTile(
            modifier = Modifier.weight(1f),
            value = "${habits.avgStartLevel}%",
            label = stringResource(R.string.habits_avg_start),
            tint = ElectricCyan
        )
        StatTile(
            modifier = Modifier.weight(1f),
            value = formatMinutesShort(habits.totalMinutes),
            label = stringResource(R.string.habits_total_time),
            tint = ElectricGreen
        )
    }

    Spacer(Modifier.height(14.dp))

    // 가장 먼저 고칠 것 하나만 강조합니다. 세 가지를 나열하면 아무것도 하지 않게 됩니다.
    val concern = habits.primaryConcern()
    ConcernCard(concern)

    Spacer(Modifier.height(16.dp))
    WhyNoSohNote()
}

/** 온도 추세 해석문. 표본이 적으면 방향을 말하지 않습니다. */
@Composable
private fun trendInterpretation(trend: BatteryInsights.TemperatureTrend): String {
    val abs = String.format(Locale.getDefault(), "%.1f", kotlin.math.abs(trend.deltaC))
    return when (trend.direction) {
        BatteryInsights.Direction.RISING -> stringResource(R.string.temp_trend_rising, abs)
        BatteryInsights.Direction.FALLING -> stringResource(R.string.temp_trend_falling, abs)
        BatteryInsights.Direction.STABLE -> stringResource(R.string.temp_trend_stable)
        BatteryInsights.Direction.UNKNOWN -> stringResource(R.string.temp_trend_unknown)
    }
}

/**
 * 세션별 최고 온도 추이.
 *
 * 막대 그래프 대신 **선** 을 씁니다. 이유는 세로 눈금이 0 이 아니기 때문입니다.
 * 사용자가 "평균 31℃ vs 33℃" 를 0 기준으로 그려 보면 두 그래프가 같은 높이처럼
 * 보이고, 변화가 사라집니다. 축을 데이터 범위에 맞춰야 변화가 보이고,
 * 그렇다고 축이 없는 선은 거짓말을 합니다. 그래서 최저~최고 사이를 정직하게
 * 축으로 표시합니다.
 *
 * 호출 시점에 [records] 는 오래된 순이어야 합니다.
 */
@Composable
private fun TemperatureTrendChart(
    records: List<ChargeRecord>,
    modifier: Modifier = Modifier
) {
    val temps = records.map { it.peakTempC }
    if (temps.isEmpty()) return

    // 축 범위: 최저~최고 에 여유를 준 뒤, 최소 폭 5℃ 를 확보합니다.
    val rawMin = temps.min()
    val rawMax = temps.max()
    val padding = ((rawMax - rawMin) * 0.25f).coerceAtLeast(2.5f)
    val axisMin = rawMin - padding
    val axisMax = rawMax + padding

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(88.dp)) {
                val stepX = if (temps.size > 1) size.width / (temps.size - 1) else 0f

                fun yOf(c: Float): Float {
                    val t = ((c - axisMin) / (axisMax - axisMin)).coerceIn(0f, 1f)
                    return size.height * (1f - t)
                }

                // 위험 구간(40℃ 이상) 을 뒤에 옅게 깔아 둡니다.
                if (axisMax > BatteryInsights.HOT_THRESHOLD_C) {
                    val yTop = yOf(axisMax)
                    val yHot = yOf(BatteryInsights.HOT_THRESHOLD_C)
                    drawRect(
                        color = ElectricAmber.copy(alpha = 0.10f),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, yTop),
                        size = androidx.compose.ui.geometry.Size(size.width, (yHot - yTop).coerceAtLeast(0f))
                    )
                    // 40℃ 선
                    drawLine(
                        color = ElectricAmber.copy(alpha = 0.45f),
                        start = androidx.compose.ui.geometry.Offset(0f, yHot),
                        end = androidx.compose.ui.geometry.Offset(size.width, yHot),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 추이 선
                val path = Path()
                temps.forEachIndexed { i, c ->
                    val x = if (temps.size > 1) i * stepX else size.width / 2f
                    val y = yOf(c)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = ElectricCyan,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // 점 — 구분이 보이도록 테두리를 진하게 줍니다.
                temps.forEachIndexed { i, c ->
                    val x = if (temps.size > 1) i * stepX else size.width / 2f
                    val y = yOf(c)
                    val dotColor = when (BatteryInsights.zoneOf(c)) {
                        BatteryInsights.Zone.HOT -> ElectricAmber
                        BatteryInsights.Zone.WARM -> ElectricCyan
                        BatteryInsights.Zone.COOL -> ElectricGreen
                    }
                    drawCircle(color = dotColor, radius = 3.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
                }
            }

            // 축 상단 값
            Text(
                text = formatTemp(axisMax),
                style = AppText.Axis,
                color = Slate700,
                modifier = Modifier.align(Alignment.TopEnd)
            )
            Text(
                text = formatTemp(axisMin),
                style = AppText.Axis,
                color = Slate700,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }

        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.stats_axis_old),
                style = AppText.Axis,
                color = Slate700
            )
            Text(
                text = stringResource(R.string.stats_axis_new),
                style = AppText.Axis,
                color = Slate700
            )
        }
        Spacer(Modifier.height(10.dp))
        TemperatureZoneLegend()
    }
}

/**
 * 점 색이 뜻하는 것을 밝힙니다.
 *
 * 색만 바꾸면 "앰버가 왜 나쁜 건지" 알 수 없습니다. 라벨 세 개면 충분합니다 —
 * 색 구간이 세 개(저온 / 다뜻 / 고온)이라서 더 늘릴 이유가 없습니다.
 */
@Composable
private fun TemperatureZoneLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        LegendDot(ElectricGreen, stringResource(R.string.temp_zone_cool))
        LegendDot(ElectricCyan, stringResource(R.string.temp_zone_warm))
        LegendDot(ElectricAmber, stringResource(R.string.temp_zone_hot))
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(AppShapes.pill)
                .background(color)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = AppText.Axis,
            color = Slate700
        )
    }
}

/** 관측 비율 한 줄. 막대 길이가 곧 비율입니다. */
@Composable
private fun HabitBar(
    label: String,
    fraction: Float,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(bottom = 10.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Slate400
        )
        Spacer(Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(AppShapes.pill)
                .background(Slate800)
        ) {
            // 0% 인 항목은 아무것도 그리지 않습니다. 최소 폭을 주면 "있다"는
            // 착각을 만들게 됩니다.
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(AppShapes.pill)
                        .background(tint)
                )
            }
        }
    }
}

/** 가장 먼저 고칠 습관 하나. */
@Composable
private fun ConcernCard(concern: BatteryInsights.Concern?) {
    if (concern == null) {
        Text(
            text = stringResource(R.string.habits_unknown),
            style = MaterialTheme.typography.bodySmall,
            color = Slate700
        )
        return
    }

    val (titleRes, bodyRes, tint) = when (concern) {
        BatteryInsights.Concern.PINNED_AT_FULL ->
            Triple(R.string.concern_pinned_title, R.string.concern_pinned_body, ElectricAmber)

        BatteryInsights.Concern.DEEP_DISCHARGE ->
            Triple(R.string.concern_deep_title, R.string.concern_deep_body, ElectricCyan)

        BatteryInsights.Concern.PLUGGED_OVERNIGHT ->
            Triple(R.string.concern_overnight_title, R.string.concern_overnight_body, ElectricAmber)

        BatteryInsights.Concern.NONE ->
            Triple(R.string.concern_none_title, R.string.concern_none_body, ElectricGreen)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.md)
            .background(tint.copy(alpha = 0.10f))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(Spacing.sm))
        Column {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = tint
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodySmall,
                color = Slate400,
                lineHeight = 17.sp
            )
        }
    }
}

/**
 * "건강도 % 가 왜 없냐" 를 한 번만 조용히 밝힙니다.
 *
 * 감추면 사용자가 "왜 없지?" 하고 더 의심합니다. 다만 길게 쓰지 않고
 * 접힌 한 줄로 둡니다 — 이건 부가 정보이지 경고가 아닙니다.
 */
@Composable
private fun WhyNoSohNote() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.md)
            .background(Slate900)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = stringResource(R.string.insights_why_title),
            style = AppText.Badge,
            color = Slate400
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.insights_why_body),
            style = AppText.Axis,
            color = Slate700,
            lineHeight = 14.sp
        )
    }
}

/** "31.4" 처럼 소수 첫째 자리까지만. 센서 정밀도를 넘겨 신뢰를 갉아먹지 않습니다. */
private fun formatTemp(c: Float): String =
    String.format(Locale.getDefault(), "%.1f", c)

/** "1h 24m" 형태. 0이하면 "0m". */
internal fun formatMinutesShort(minutes: Long): String {
    if (minutes <= 0) return "0m"
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

/** 기록 시각을 "오늘 14:32" / "10/12 09:05" 형태로 표시 */
@Composable
private fun formatRecordDate(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    val now = Calendar.getInstance()
    val sameYear = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    val sameDay = sameYear && cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    val hh = "%02d".format(cal.get(Calendar.HOUR_OF_DAY))
    val mm = "%02d".format(cal.get(Calendar.MINUTE))
    return if (sameDay) {
        stringResource(R.string.stats_today, hh, mm)
    } else {
        val md = "%02d/%02d".format(
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        "$md $hh:$mm"
    }
}