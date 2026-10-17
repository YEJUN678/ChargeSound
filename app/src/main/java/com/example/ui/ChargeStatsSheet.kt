package com.example.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ChargeRecord
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import java.util.Calendar

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
                fontSize = 13.sp,
                color = Slate600,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.stats_empty_sub),
                fontSize = 11.sp,
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
                value = "+$avgGain%",
                label = stringResource(R.string.stats_avg_gain),
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
                value = if (avgRate != null) formatRate(avgRate) else "—",
                label = stringResource(R.string.stats_avg_rate),
                tint = ElectricCyan
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                modifier = Modifier.weight(1f),
                value = "%.1f °C".format(avgPeakTemp),
                label = stringResource(R.string.stats_peak_temp),
                tint = if (avgPeakTemp >= 45f) ElectricAmber else ElectricGreen
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
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.stats_chart_sub, MAX_BARS),
            fontSize = 11.sp,
            color = Slate600
        )
        Spacer(Modifier.height(12.dp))

        ChargeBarChart(records = recent.reversed(), modifier = Modifier.fillMaxWidth())

        Spacer(Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.stats_recent_title),
            fontSize = 13.sp,
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = formatRecordDate(record.startedAt) + " · " +
                            formatMinutesShort(record.durationMillis / 60_000L),
                        fontSize = 10.sp,
                        color = Slate600
                    )
                }
                record.percentPerHour?.let { rate ->
                    Text(
                        text = formatRate(rate),
                        fontSize = 11.sp,
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
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Slate400
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.stats_local_note),
            fontSize = 10.sp,
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
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            color = tint,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
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
                fontSize = 9.sp,
                color = Slate700
            )
            Text(
                text = stringResource(R.string.stats_axis_max, maxGain),
                fontSize = 9.sp,
                color = Slate700
            )
            Text(
                text = stringResource(R.string.stats_axis_new),
                fontSize = 9.sp,
                color = Slate700
            )
        }
    }
}

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