package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AnimationMode
import com.example.data.ChargeSettings
import com.example.data.PremiumCatalog
import com.example.ui.theme.AppShapes
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.Slate400
import com.example.ui.theme.Spacing
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

/** 이 앱의 개인정보처리방침 문서 (GitHub Pages) */
private const val PRIVACY_POLICY_URL =
    "https://YEJUN678.github.io/ChargeSound/privacy-policy.html"

/**
 * 상단 점3개(⋯) 오버플로 메뉴.
 *
 * 메인 화면은 핵심 기능(효과음 / 애니메이션 / 배터리)에 집중하고,
 * 프리미엄 콘텐츠·광고 없는 시간·앱 정보는 이 메뉴로 모았습니다.
 */
@Composable
fun OverflowMenu(
    onOpenAlarm: () -> Unit,
    onOpenAlerts: () -> Unit,
    onOpenSoundContext: () -> Unit,
    onOpenPremiumSounds: () -> Unit,
    onOpenPremiumAnimations: () -> Unit,
    onOpenAdFree: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenAbout: () -> Unit,
    adFreeMinutesRemaining: Int
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = stringResource(R.string.menu_more),
            tint = Color.White
        )
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_alarm)) },
            leadingIcon = { Icon(Icons.Default.Alarm, contentDescription = null) },
            onClick = {
                expanded = false
                onOpenAlarm()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_premium_sounds)) },
            leadingIcon = { Icon(Icons.Default.Star, contentDescription = null) },
            onClick = {
                expanded = false
                onOpenPremiumSounds()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_premium_animations)) },
            leadingIcon = { Icon(Icons.Default.Movie, contentDescription = null) },
            onClick = {
                expanded = false
                onOpenPremiumAnimations()
            }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_sound_context)) },
            leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null) },
            onClick = {
                expanded = false
                onOpenSoundContext()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_alerts)) },
            leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null) },
            onClick = {
                expanded = false
                onOpenAlerts()
            }
        )

        DropdownMenuItem(
            text = {
                Text(
                    if (adFreeMinutesRemaining > 0) {
                        stringResource(R.string.duration_minutes, adFreeMinutesRemaining)
                    } else {
                        stringResource(R.string.adfree_card_title)
                    }
                )
            },
            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
            onClick = {
                expanded = false
                onOpenAdFree()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_theme)) },
            leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
            onClick = {
                expanded = false
                onOpenTheme()
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_stats)) },
            leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null) },
            onClick = {
                expanded = false
                onOpenStats()
            }
        )
        // 크래시 기록이 남아 있으면 여기에 나타납니다 (로컬 저장, 서버 전송 없음).
        val crashContext = LocalContext.current
        val crashCount = remember { com.example.diagnostics.CrashReporter.count(crashContext) }
        if (crashCount > 0) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_crash_report, crashCount)) },
                leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null) },
                onClick = {
                    expanded = false
                    com.example.diagnostics.CrashReporter.share(crashContext)
                }
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(R.string.menu_about)) },
            leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
            onClick = {
                expanded = false
                onOpenAbout()
            }
        )
    }
}

/**
 * 프리미엄 사운드 목록.
 *
 * 프리미엄 항목은 광고 2회 시청으로 잠금이 해제됩니다.
 * 광고를 거부해도 무료 프리셋 사운드는 그대로 쓸 수 있습니다 (광고 정책).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumSoundsSheet(
    settings: ChargeSettings,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onWatchAd: (String) -> Unit
) {
    SheetScaffold(
        titleRes = R.string.premium_sounds_title,
        onDismiss = onDismiss,
        scrollable = false
    ) {
        Text(
            text = stringResource(
                R.string.premium_sounds_sub,
                PremiumCatalog.ADS_REQUIRED_PER_UNLOCK
            ),
            style = MaterialTheme.typography.bodySmall,
            color = Slate400,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(PremiumCatalog.sounds) { premium ->
                val accent = Color(premium.accentArgb)
                if (settings.isUnlocked(premium.id)) {
                    PremiumUnlockedRow(
                        nameRes = premium.nameRes,
                        descriptionRes = premium.descriptionRes,
                        accent = accent,
                        isSelected = settings.presetSoundId == premium.id,
                        onSelect = { onSelect(premium.id) }
                    )
                } else {
                    PremiumLockedRow(
                        nameRes = premium.nameRes,
                        descriptionRes = premium.descriptionRes,
                        accent = accent,
                        adsWatched = settings.rewardProgress[premium.id] ?: 0,
                        adsRequired = PremiumCatalog.ADS_REQUIRED_PER_UNLOCK,
                        onWatchAd = { onWatchAd(premium.id) }
                    )
                }
            }
        }
    }
}

/** 프리미엄 애니메이션 테마 목록 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumAnimationsSheet(
    settings: ChargeSettings,
    onDismiss: () -> Unit,
    onSelect: (AnimationMode) -> Unit,
    onWatchAd: (String) -> Unit
) {
    SheetScaffold(
        titleRes = R.string.premium_anims_title,
        onDismiss = onDismiss,
        scrollable = false
    ) {
        Text(
            text = stringResource(
                R.string.premium_anims_sub,
                PremiumCatalog.ADS_REQUIRED_PER_UNLOCK
            ),
            style = MaterialTheme.typography.bodySmall,
            color = Slate400,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(PremiumCatalog.animations) { premium ->
                val accent = Color(premium.accentArgb)
                if (settings.isUnlocked(premium.id)) {
                    PremiumUnlockedRow(
                        nameRes = premium.nameRes,
                        descriptionRes = premium.descriptionRes,
                        accent = accent,
                        isSelected = settings.animationMode == premium.mode,
                        onSelect = { onSelect(premium.mode) }
                    )
                } else {
                    PremiumLockedRow(
                        nameRes = premium.nameRes,
                        descriptionRes = premium.descriptionRes,
                        accent = accent,
                        adsWatched = settings.rewardProgress[premium.id] ?: 0,
                        adsRequired = PremiumCatalog.ADS_REQUIRED_PER_UNLOCK,
                        onWatchAd = { onWatchAd(premium.id) }
                    )
                }
            }
        }
    }
}

/** 앱 정보 + 개인정보처리방침 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(onDismiss: () -> Unit, onCheckUpdate: () -> Unit) {
    val context = LocalContext.current

    SheetScaffold(titleRes = R.string.menu_about, onDismiss = onDismiss) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.about_description),
            style = MaterialTheme.typography.bodySmall,
            color = Slate400
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Slate800)
        Spacer(Modifier.height(16.dp))

        // 수동 업데이트 확인 — 언제든 스토어 페이지로 연결합니다.
        Surface(
            color = Slate800,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Slate700),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckUpdate() }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.update_check_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.update_check_sub),
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            color = Slate800,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
                    context.startActivity(intent)
                }
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.about_privacy_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.about_privacy_sub),
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.about_data_note),
            style = MaterialTheme.typography.labelSmall,
            color = Slate600,
            lineHeight = 16.sp
        )
    }
}

/** 충전 알람 설정 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSettingsSheet(
    settings: ChargeSettings,
    onDismiss: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onTogglePercent: (Int) -> Unit,
    onSelectSnoozeMinutes: (Int) -> Unit,
    onToggleChargeLimit: (Boolean) -> Unit,
    onSelectChargeLimitPercent: (Int) -> Unit,
    onToggleWithSound: (Boolean) -> Unit
) {
    SheetScaffold(titleRes = R.string.menu_alarm, onDismiss = onDismiss) {
        ToggleRow(
            title = stringResource(R.string.alarm_enable_title),
            subtitle = stringResource(R.string.alarm_enable_sub),
            checked = settings.alarmEnabled,
            onCheckedChange = onToggleEnabled
        )

        Spacer(Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.alarm_target_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = stringResource(R.string.alarm_target_multi_sub),
            style = MaterialTheme.typography.labelSmall,
            color = Slate400
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(50, 60, 70, 80, 90, 100).forEach { percent ->
                val selected = percent in settings.alarmTargets
                Surface(
                    color = if (selected) ElectricCyan else Slate800,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (selected) ElectricCyan else Slate700),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTogglePercent(percent) }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$percent",
                            color = if (selected) Color.Black else Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (settings.alarmTargets.size > 1) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.alarm_targets_summary, settings.alarmTargets.joinToString(", ")),
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCyan
            )
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.alarm_snooze_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 20, 30).forEach { minutes ->
                val selected = settings.alarmSnoozeMinutes == minutes
                Surface(
                    color = if (selected) ElectricCyan else Slate800,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (selected) ElectricCyan else Slate700),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectSnoozeMinutes(minutes) }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.alarm_snooze_minutes, minutes),
                            color = if (selected) Color.Black else Color.White,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.alarm_snooze_sub),
            style = MaterialTheme.typography.labelSmall,
            color = Slate600
        )

        Spacer(Modifier.height(18.dp))

        ToggleRow(
            title = stringResource(R.string.alarm_sound_title),
            subtitle = stringResource(R.string.alarm_sound_sub),
            checked = settings.alarmWithSound,
            onCheckedChange = onToggleWithSound
        )

        HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Slate800)

        // 배터리 수명 보호: 100%까지 채우지 말고 이 지점에서 끊으라고 알려줍니다.
        ToggleRow(
            title = stringResource(R.string.limit_enable_title),
            subtitle = stringResource(R.string.limit_enable_sub),
            checked = settings.chargeLimitEnabled,
            onCheckedChange = onToggleChargeLimit
        )

        if (settings.chargeLimitEnabled) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 70, 80, 90).forEach { percent ->
                    val selected = settings.chargeLimitPercent == percent
                    Surface(
                        color = if (selected) ElectricGreen else Slate800,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (selected) ElectricGreen else Slate700),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectChargeLimitPercent(percent) }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$percent%",
                                color = if (selected) Color.Black else Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.limit_health_note),
                style = MaterialTheme.typography.labelSmall,
                color = Slate600,
                lineHeight = 15.sp
            )
        }
    }
}
/** 배터리 경고 설정 (온도 · 건강도) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertSettingsSheet(
    settings: ChargeSettings,
    onDismiss: () -> Unit,
    onToggleTemp: (Boolean) -> Unit,
    onSelectTempThreshold: (Float) -> Unit,
    onToggleHealth: (Boolean) -> Unit,
    onToggleLowBattery: (Boolean) -> Unit,
    onSelectLowBatteryThreshold: (Int) -> Unit,
    onToggleRapidDischarge: (Boolean) -> Unit,
    onSelectRapidWindow: (Int) -> Unit,
    onSelectRapidDrop: (Int) -> Unit
) {
    SheetScaffold(titleRes = R.string.menu_alerts, onDismiss = onDismiss) {
        ToggleRow(
            title = stringResource(R.string.temp_alert_title),
            subtitle = stringResource(R.string.temp_alert_sub),
            checked = settings.tempAlertEnabled,
            onCheckedChange = onToggleTemp
        )

        if (settings.tempAlertEnabled) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(40f, 42f, 45f, 48f, 50f).forEach { threshold ->
                    val selected = settings.tempAlertThresholdC == threshold
                    Surface(
                        color = if (selected) ElectricCyan else Slate800,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (selected) ElectricCyan else Slate700),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectTempThreshold(threshold) }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${threshold.toInt()}°",
                                color = if (selected) Color.Black else Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        ToggleRow(
            title = stringResource(R.string.health_alert_title),
            subtitle = stringResource(R.string.health_alert_sub),
            checked = settings.healthAlertEnabled,
            onCheckedChange = onToggleHealth
        )

        HorizontalDivider(color = Slate800)
        Spacer(Modifier.height(16.dp))

        ToggleRow(
            title = stringResource(R.string.low_battery_title),
            subtitle = stringResource(R.string.low_battery_sub),
            checked = settings.lowBatteryAlertEnabled,
            onCheckedChange = onToggleLowBattery
        )

        if (settings.lowBatteryAlertEnabled) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(10, 15, 20, 25, 30).forEach { threshold ->
                    val selected = settings.lowBatteryThreshold == threshold
                    Surface(
                        color = if (selected) ElectricCyan else Slate800,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (selected) ElectricCyan else Slate700),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectLowBatteryThreshold(threshold) }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$threshold%",
                                color = if (selected) Color.Black else Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        ToggleRow(
            title = stringResource(R.string.rapid_discharge_title),
            subtitle = stringResource(R.string.rapid_discharge_sub),
            checked = settings.rapidDischargeAlertEnabled,
            onCheckedChange = onToggleRapidDischarge
        )

        if (settings.rapidDischargeAlertEnabled) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.rapid_window_title),
                style = MaterialTheme.typography.labelSmall,
                color = Slate400
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(15, 30, 60, 120).forEach { minutes ->
                    val selected = settings.rapidDischargeWindowMinutes == minutes
                    Surface(
                        color = if (selected) ElectricCyan else Slate800,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (selected) ElectricCyan else Slate700),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectRapidWindow(minutes) }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.duration_minutes, minutes),
                                color = if (selected) Color.Black else Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.rapid_drop_title),
                style = MaterialTheme.typography.labelSmall,
                color = Slate400
            )
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(5, 10, 15, 20).forEach { drop ->
                    val selected = settings.rapidDischargeDropPercent == drop
                    Surface(
                        color = if (selected) ElectricCyan else Slate800,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (selected) ElectricCyan else Slate700),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectRapidDrop(drop) }
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "-$drop%",
                                color = if (selected) Color.Black else Color.White,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.anim_exit_hint),
            style = MaterialTheme.typography.labelSmall,
            color = Slate600,
            lineHeight = 16.sp
        )
    }
}

@Composable
internal fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.md)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(Spacing.md))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * 업데이트 안내 다이얼로그.
 *
 * 스토어에 새 버전이 올라가 있을 때 앱을 열 때마다 자동으로 표시됩니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateAvailableDialog(
    latestVersionName: String,
    onUpdate: () -> Unit,
    onLater: () -> Unit
) {
    androidx.compose.material3.BasicAlertDialog(
        onDismissRequest = onLater,
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Slate900,
            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.update_dialog_title),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.update_dialog_body, latestVersionName),
                    color = Slate400,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onUpdate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.update_dialog_btn),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.update_dialog_later),
                    color = Slate400,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clickable { onLater() }
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/** 공통 바텀시트 껍데기 — 제목은 리소스 ID로, 닫기는 호출자가 소유한 상태로 처리합니다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SheetScaffold(
    @androidx.annotation.StringRes titleRes: Int,
    onDismiss: () -> Unit,
    /**
     * 내용을 세로 스크롤로 감쌀지 여부.
     *
     * 기본값 true — 테마/충전 통계 시트처럼 내용이 화면보다 길어지면
     * 아래쪽 항목(고대비 토글, 기록 목록 등)이 잘려서 만질 수 없었습니다.
     *
     * 단, 내용물이 이미 LazyColumn 인 시트(프리미엄 사운드/애니메이션)는
     * 세로 스크롤을 겹치면 "infinite maximum height" 로 크래시하므로
     * false 로 넘겨야 합니다.
     */
    scrollable: Boolean = true,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // 시트는 앱 배경(950)보다 확실히 밝아야 위에 떠 있는 시트로 읽힙니다.
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl)
                .padding(bottom = Spacing.xxl)
                .then(if (scrollable) Modifier.verticalScroll(scrollState) else Modifier)
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = Spacing.md)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(Spacing.md))
            content()
        }
    }
}
