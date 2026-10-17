package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.battery.PlugType
import com.example.data.ChargeSettings
import com.example.sound.SoundManager
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

/** 하루 기준 분(0~1439)을 "HH:mm" 으로 변환합니다. */
private fun formatMinute(minuteOfDay: Int): String {
    val m = ((minuteOfDay % 1440) + 1440) % 1440
    return "%02d:%02d".format(m / 60, m % 60)
}

/**
 * 충전 방식별 사운드 + 조용한 시간 설정.
 *
 * 두 설정은 "소리를 언제, 어떤 상황에서 낼지"를 정하므로 한 화면에 묶었습니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundContextSettingsSheet(
    settings: ChargeSettings,
    onDismiss: () -> Unit,
    onTogglePlugSpecific: (Boolean) -> Unit,
    onSelectPreset: (PlugType, String) -> Unit,
    onToggleQuiet: (Boolean) -> Unit,
    onSetQuietHours: (Int, Int) -> Unit
) {
    val presets = remember { SoundManager.FREE_PRESETS }

    SheetScaffold(titleRes = R.string.menu_sound_context, onDismiss = onDismiss) {
        ToggleRow(
            title = stringResource(R.string.plug_sound_title),
            subtitle = stringResource(R.string.plug_sound_sub),
            checked = settings.plugSpecificSounds,
            onCheckedChange = onTogglePlugSpecific
        )

        if (settings.plugSpecificSounds) {
            Spacer(Modifier.height(14.dp))
            listOf(
                PlugType.AC to settings.acPresetId,
                PlugType.USB to settings.usbPresetId,
                PlugType.WIRELESS to settings.wirelessPresetId
            ).forEach { (plugType, currentId) ->
                Column(Modifier.padding(bottom = 14.dp)) {
                    Text(
                        text = stringResource(plugType.displayNameRes),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate400
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { preset ->
                            val selected = preset.id == currentId
                            Surface(
                                color = if (selected) ElectricCyan else Slate800,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (selected) ElectricCyan else Slate700),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectPreset(plugType, preset.id) }
                            ) {
                                Box(
                                    modifier = Modifier.padding(
                                        vertical = 8.dp,
                                        horizontal = 2.dp
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(preset.nameRes),
                                        color = if (selected) Color.Black else Color.White,
                                        fontSize = 9.sp,
                                        lineHeight = 11.sp,
                                        maxLines = 2,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Slate800)
        Spacer(Modifier.height(16.dp))

        ToggleRow(
            title = stringResource(R.string.quiet_hours_title),
            subtitle = stringResource(R.string.quiet_hours_sub),
            checked = settings.quietHoursEnabled,
            onCheckedChange = onToggleQuiet
        )

        if (settings.quietHoursEnabled) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TimePickerField(
                    label = stringResource(R.string.quiet_hours_start),
                    minuteOfDay = settings.quietStartMinute,
                    onPick = { onSetQuietHours(it, settings.quietEndMinute) },
                    modifier = Modifier.weight(1f)
                )
                TimePickerField(
                    label = stringResource(R.string.quiet_hours_end),
                    minuteOfDay = settings.quietEndMinute,
                    onPick = { onSetQuietHours(settings.quietStartMinute, it) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.quiet_hours_range,
                    formatMinute(settings.quietStartMinute),
                    formatMinute(settings.quietEndMinute)
                ),
                fontSize = 11.sp,
                color = Slate400
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerField(
    label: String,
    minuteOfDay: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberTimePickerState(
        initialHour = (minuteOfDay / 60).coerceIn(0, 23),
        initialMinute = (minuteOfDay % 60).coerceIn(0, 59),
        is24Hour = true
    )
    var open by remember { mutableStateOf(false) }

    Surface(
        color = Slate800,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Slate700),
        modifier = modifier.clickable { open = true }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 10.sp, color = Slate400)
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatMinute(minuteOfDay),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    if (open) {
        // 이 Material3 버전에는 TimePickerDialog 가 없어 AlertDialog + TimePicker 로 구성합니다.
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onPick(state.hour * 60 + state.minute)
                        open = false
                    }
                ) { Text(stringResource(R.string.btn_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { open = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            },
            title = { Text(stringResource(R.string.quiet_hours_pick_title)) },
            text = { TimePicker(state = state) },
            containerColor = Slate900,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}
