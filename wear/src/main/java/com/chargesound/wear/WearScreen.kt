package com.chargesound.wear

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// 휴대폰 앱의 팔레트와 같은 색을 씁니다. 워치 모듈은 휴대폰 모듈의 리소스를
// 참조할 수 없어서(별도 APK) 값만 복사해 둡니다.
private val ElectricCyan = Color(0xFF00E5FF)
private val Slate950 = Color(0xFF0A0F1D)
private val Slate900 = Color(0xFF111827)
private val Slate800 = Color(0xFF1E293B)
private val Slate600 = Color(0xFF475569)
private val Slate400 = Color(0xFF94A3B8)

@Composable
fun ChargeSoundWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ElectricCyan,
            background = Slate950,
            surface = Slate950,
            onBackground = Color.White,
            onSurface = Color.White
        ),
        content = content
    )
}

/**
 * 워치 메인 화면.
 *
 * 원형 화면을 가정하고 위아래 여백을 넉넉히 둡니다. 내용이 길어지면
 * 스크롤되므로 작은 워치(1.2인치)에서도 버튼이 잘리지 않습니다.
 */
@Composable
fun WearScreen(
    snapshot: PhoneSnapshot?,
    onToggleService: () -> Unit,
    onOpenPhone: () -> Unit,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Slate950, Slate900)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            if (snapshot == null) {
                WaitingContent(onRefresh = onRefresh, onOpenPhone = onOpenPhone)
            } else {
                BatteryContent(
                    snapshot = snapshot,
                    onToggleService = onToggleService,
                    onOpenPhone = onOpenPhone
                )
            }
        }
    }
}

@Composable
private fun BatteryContent(
    snapshot: PhoneSnapshot,
    onToggleService: () -> Unit,
    onOpenPhone: () -> Unit
) {
    val level = snapshot.level.coerceIn(0, 100)
    val accent = if (snapshot.charging) ElectricCyan else Slate400

    Box(
        modifier = Modifier.size(136.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { level / 100f },
            modifier = Modifier.fillMaxSize(),
            color = accent,
            trackColor = Slate800,
            strokeWidth = 7.dp
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.wear_battery_fmt, level),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = stringResource(
                    if (snapshot.charging) R.string.wear_status_charging
                    else R.string.wear_status_discharging
                ),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = accent
            )
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Text(
        text = speedText(snapshot),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = if (snapshot.charging) ElectricCyan else Slate400
    )

    Text(
        text = stringResource(R.string.wear_temp_fmt, snapshot.tempC),
        fontSize = 11.sp,
        color = Slate400
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
        text = stringResource(R.string.wear_sync_fmt, syncTime(snapshot.updatedAt)),
        fontSize = 9.sp,
        color = Slate600
    )

    Spacer(modifier = Modifier.height(14.dp))

    WatchButton(
        text = stringResource(
            if (snapshot.serviceEnabled) R.string.wear_action_toggle_off
            else R.string.wear_action_toggle_on
        ),
        primary = true,
        onClick = onToggleService
    )

    Spacer(modifier = Modifier.height(8.dp))

    WatchButton(
        text = stringResource(R.string.wear_action_open_phone),
        primary = false,
        onClick = onOpenPhone
    )
}

@Composable
private fun WaitingContent(onRefresh: () -> Unit, onOpenPhone: () -> Unit) {
    Text(
        text = stringResource(R.string.wear_waiting_title),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = stringResource(R.string.wear_waiting_desc),
        fontSize = 11.sp,
        color = Slate400,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 6.dp)
    )

    Spacer(modifier = Modifier.height(18.dp))

    WatchButton(
        text = stringResource(R.string.wear_action_refresh),
        primary = true,
        onClick = onRefresh
    )

    Spacer(modifier = Modifier.height(8.dp))

    WatchButton(
        text = stringResource(R.string.wear_action_open_phone),
        primary = false,
        onClick = onOpenPhone
    )
}

@Composable
private fun WatchButton(text: String, primary: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .background(if (primary) ElectricCyan.copy(alpha = 0.16f) else Slate800)
            .border(
                width = 1.dp,
                color = if (primary) ElectricCyan.copy(alpha = 0.6f) else Slate600,
                shape = shape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (primary) ElectricCyan else Color.White
        )
    }
}

/** W 값 -> 초고속/고속/일반/저속. 휴대폰 위젯과 같은 기준을 씁니다. */
@Composable
private fun speedText(snapshot: PhoneSnapshot): String {
    val watts = snapshot.watts
    if (watts == null || watts <= 0f) {
        return stringResource(R.string.wear_speed_unavailable)
    }
    val labelRes = when {
        watts >= 25f -> R.string.wear_speed_super
        watts >= 15f -> R.string.wear_speed_fast
        watts >= 5f -> R.string.wear_speed_normal
        else -> R.string.wear_speed_slow
    }
    return stringResource(R.string.wear_speed_fmt, stringResource(labelRes), watts)
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun syncTime(epochMillis: Long): String {
    if (epochMillis <= 0L) return "--:--"
    return Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)
}
