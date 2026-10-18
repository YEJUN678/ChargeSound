package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ad.AdManager
import com.example.battery.BatteryInfo
import com.example.battery.PowerTracker
import com.example.data.AnimationMode
import com.example.data.ChargeSettings
import com.example.data.PremiumCatalog
import com.example.data.SoundMode
import com.example.service.ChargingSessionTracker
import com.example.service.UpdateChecker
import com.example.sound.SoundManager
import com.example.ui.theme.AppShapes
import com.example.ui.theme.AppText
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.Spacing
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val batteryInfo by viewModel.batteryInfo.collectAsState()
    val chargeHistory by viewModel.chargeHistory.collectAsState()
    val isPlayingPreview by viewModel.isPlayingPreview.collectAsState()
    val activePlayingId by viewModel.activePlayingId.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val overlayGranted by viewModel.overlayGranted.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAdFreeDialog by remember { mutableStateOf(false) }
    var showPremiumSounds by remember { mutableStateOf(false) }
    var showPremiumAnimations by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showAlarmSheet by remember { mutableStateOf(false) }
    var showAlertsSheet by remember { mutableStateOf(false) }
    var showSoundContextSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showStatsSheet by remember { mutableStateOf(false) }
    var showTrimSheet by remember { mutableStateOf(false) }
    var latestVersionName by remember { mutableStateOf<String?>(null) }

    // 앱을 열 때마다 업데이트를 확인합니다.
    // 네트워크가 없거나 확인에 실패해도 아무 것도 하지 않습니다 (조용히 넘어갑니다).
    LaunchedEffect(Unit) {
        UpdateChecker.isUpdateAvailable()?.let { latestVersionName = it.versionName }
    }

    // 보상형 광고를 표시하려면 Activity가 필요합니다 (Compose Context로는 부족).
    // LocalActivity 는 "가장 가까운 Activity" 를 안전하게 찾아주므로
    // Context 를 Activity 로 캐스팅하는 것보다 안전합니다.
    val activity = LocalActivity.current

    // 광고 없는 시간 활성 여부 (1분마다 재평가)
    var isAdFreeActive by remember { mutableStateOf(viewModel.isAdFreeActive()) }
    LaunchedEffect(Unit) {
        while (true) {
            isAdFreeActive = viewModel.isAdFreeActive()
            delay(30_000L)
        }
    }

    // Request Notification permission on Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Audio file picker (광고 강제 없음 — 파일 선택 자체는 언제든 가능합니다)
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.selectCustomAudio(it) }
    }

    // Video file picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.handleSelectedVideo(it) }
    }

    // "다른 앱 위에 표시" 권한 요청
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // 사용자가 설정 화면에서 돌아오면 실제 권한 상태를 다시 읽습니다.
        viewModel.refreshOverlayPermission()
    }

    // 보상형 광고 프리로드 (잠금 해제 버튼과 "광고 없이 사용하기" 카드에서 즉시 표시할 수 있도록)
    LaunchedEffect(Unit) {
        AdManager.loadRewarded(context)
        viewModel.refreshOverlayPermission()
    }

    // 광고 없는 시간 카운트다운 표시 갱신
    var adFreeMinutes by remember { mutableStateOf(viewModel.adFreeRemainingMinutes()) }
    LaunchedEffect(isAdFreeActive) {
        while (isAdFreeActive) {
            adFreeMinutes = viewModel.adFreeRemainingMinutes()
            delay(30_000L)
        }
        adFreeMinutes = 0
    }

    // 충전 속도 배지 — 충전 중일 때만 폴링합니다.
    // 배터리는 1% 단위로 올라가므로 20초마다 갱신하면 충분합니다.
    var session by remember { mutableStateOf(ChargingSessionTracker.Snapshot(0L, null, null, false)) }
    LaunchedEffect(batteryInfo.isCharging) {
        while (batteryInfo.isCharging) {
            session = ChargingSessionTracker.snapshot(context, batteryInfo)
            delay(20_000L)
        }
        session = ChargingSessionTracker.Snapshot(0L, null, null, false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ChargeSound",
                                    style = AppText.Brand,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                Surface(
                                    color = if (settings.isServiceEnabled) {
                                        ElectricGreen.copy(alpha = 0.18f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                    shape = AppShapes.pill
                                ) {
                                    Text(
                                        text = if (settings.isServiceEnabled) "ON" else "OFF",
                                        color = if (settings.isServiceEnabled) {
                                            ElectricGreen
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        style = AppText.Badge,
                                        modifier = Modifier.padding(
                                            horizontal = Spacing.sm,
                                            vertical = Spacing.xxs,
                                        )
                                    )
                                }
                            }
                            Text(
                                text = if (settings.isServiceEnabled) {
                                    stringResource(R.string.status_monitoring_active)
                                } else {
                                    stringResource(R.string.status_monitoring_idle)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (settings.isServiceEnabled) {
                                    ElectricGreen
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                },
                actions = {
                    OverflowMenu(
                        onOpenAlarm = { showAlarmSheet = true },
                        onOpenAlerts = { showAlertsSheet = true },
                        onOpenSoundContext = { showSoundContextSheet = true },
                        onOpenPremiumSounds = { showPremiumSounds = true },
                        onOpenPremiumAnimations = { showPremiumAnimations = true },
                        onOpenAdFree = { showAdFreeDialog = true },
                        onOpenTheme = { showThemeSheet = true },
                        onOpenStats = { showStatsSheet = true },
                        onOpenAbout = { showAbout = true },
                        adFreeMinutesRemaining = adFreeMinutes
                    )
                    Switch(
                        checked = settings.isServiceEnabled,
                        onCheckedChange = { viewModel.toggleServiceEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = ElectricCyan,
                            checkedBorderColor = ElectricCyan,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .padding(end = Spacing.md)
                            .testTag("service_toggle_switch")
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Battery & Quick Test Banner with Hero Asset
            BatteryLiveHeaderCard(
                batteryInfo = batteryInfo,
                isServiceEnabled = settings.isServiceEnabled,
                rateBadge = {
                    ChargingRateBadge(
                        percentPerHour = session.percentPerHour,
                        elapsedMillis = session.elapsedMillis
                    )
                },
                onTestClick = { viewModel.simulateCharging(context) }
            )

            // Tabs: 0: Sound (MP3), 1: Animation (MP4/AVI), 2: Battery Info
            //
            // 인디케이터를 직접 그리지 않고 기본값을 씁니다.
            // 이전에 쓰던 SecondaryIndicator 는 Material 3 초기 스타일이라
            // 지금은 얇아 보입니다. 기본 Primary 인디케이터가 두껍고 현재 표준입니다.
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = ElectricCyan,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) },
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = stringResource(R.string.tab_sound),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    },
                    selectedContentColor = ElectricCyan,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = stringResource(R.string.tab_animation),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    },
                    selectedContentColor = ElectricCyan,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = stringResource(R.string.tab_battery_info),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    },
                    selectedContentColor = ElectricCyan,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTabIndex) {
                    0 -> SoundSettingsTab(
                        settings = settings,
                        isPlayingPreview = isPlayingPreview,
                        activePlayingId = activePlayingId,
                        onPickAudio = { audioPickerLauncher.launch("audio/*") },
                        onClearAudio = { viewModel.clearCustomAudio() },
                        onTrimAudio = { showTrimSheet = true },
                        hasSoundRange = settings.customSoundEndMs > settings.customSoundStartMs,
                        onToggleSound = { viewModel.toggleSoundEnabled(it) },
                        onVolumeChange = { viewModel.setVolume(it) },
                        onSelectPreset = { viewModel.selectPresetSound(it) },
                        onToggleVibration = { viewModel.toggleVibration(it) },
                        onPlayCurrent = {
                            // 사운드 미리듣기 중에는 전면류 광고를 차단합니다
                            AdManager.setCoreFeatureBusy(true)
                            viewModel.previewPlayCurrentSound()
                        },
                        onStopSound = {
                            viewModel.stopPreviewSound()
                            AdManager.setCoreFeatureBusy(false)
                        },
                        onWatchAdToUnlock = { premiumId ->
                            viewModel.watchAdToUnlock(activity, premiumId)
                        },
                        isAdFreeActive = isAdFreeActive,
                        adFreeRemainingMinutes = adFreeMinutes,
                        onWatchAdForAdFree = { viewModel.watchAdForAdFree(activity) },
                        onChooseAdFreeDuration = { showAdFreeDialog = true },
                        overlayGranted = overlayGranted,
                        onRequestOverlayPermission = {
                            overlayPermissionLauncher.launch(viewModel.overlaySettingsIntent())
                        }
                    )
                    1 -> AnimationSettingsTab(
                        settings = settings,
                        onPickVideo = { videoPickerLauncher.launch("video/*") },
                        onClearVideo = { viewModel.clearCustomVideo() },
                        onTrimVideo = { showTrimSheet = true },
                        hasVideoRange = settings.customVideoEndMs > settings.customVideoStartMs,
                        onToggleAnimation = { viewModel.toggleAnimationEnabled(it) },
                        onSelectAnimationMode = { viewModel.selectAnimationMode(it) },
                        onSelectDuration = { viewModel.setVideoDuration(it) },
                        onToggleLoop = { viewModel.setVideoLoop(it) },
                        onPreviewFullScreen = { viewModel.previewFullScreenAnimation(context) },
                        onWatchAdToUnlock = { premiumId ->
                            viewModel.watchAdToUnlock(activity, premiumId)
                        }
                    )
                    2 -> BatteryInfoTab(batteryInfo = batteryInfo)
                }
            }

            // 배너 광고 (앱 하단 고정)
            AdBannerSlot(isAdFreeActive = isAdFreeActive)
        }
    }

    // 커스텀 오디오/영상 적용 후 보상형 광고 "제안" (거부해도 기능에 영향 없음)
    val freeUnlockOffer by viewModel.offerFreeUnlockCredit.collectAsState()
    if (freeUnlockOffer != null) {
        FreeUnlockOfferDialog(
            appliedLabel = freeUnlockOffer ?: "",
            onWatchAd = { viewModel.watchAdForFreeCredit(activity) },
            onDismiss = { viewModel.dismissFreeUnlockOffer() }
        )
    }

    // "광고 없는 시간" 선택 다이얼로그
    if (showAdFreeDialog) {
        AdFreeDurationDialog(
            onDismiss = { showAdFreeDialog = false },
            onSelect = { minutes ->
                showAdFreeDialog = false
                val act = activity
                if (act == null) {
                    viewModel.startAdFreePeriod(minutes)
                } else {
                    AdManager.showRewarded(
                        activity = act,
                        onReward = { viewModel.startAdFreePeriod(minutes) },
                        onDismiss = { },
                        // 광고를 못 불러온 경우에도 시간 시작은 막지 않습니다
                        // (광고 정책: 광고 실패가 앱 사용을 막아서는 안 됨)
                        onNotAvailable = { viewModel.startAdFreePeriod(minutes) }
                    )
                }
            }
        )
    }

    // 점3개 메뉴에서 연 시트들
    if (showPremiumSounds) {
        PremiumSoundsSheet(
            settings = settings,
            onDismiss = { showPremiumSounds = false },
            onSelect = { id ->
                showPremiumSounds = false
                selectedTabIndex = 0
                viewModel.selectPresetSound(id)
            },
            onWatchAd = { id -> viewModel.watchAdToUnlock(activity, id) }
        )
    }

    if (showPremiumAnimations) {
        PremiumAnimationsSheet(
            settings = settings,
            onDismiss = { showPremiumAnimations = false },
            onSelect = { mode ->
                showPremiumAnimations = false
                selectedTabIndex = 1
                viewModel.selectAnimationMode(mode)
            },
            onWatchAd = { id -> viewModel.watchAdToUnlock(activity, id) }
        )
    }

    if (showAbout) {
        AboutSheet(
            onDismiss = { showAbout = false },
            onCheckUpdate = {
                showAbout = false
                // 수동 확인은 항상 즉시 스토어 페이지를 엽니다.
                UpdateChecker.openStorePage(context)
            }
        )
    }

    if (latestVersionName != null) {
        UpdateAvailableDialog(
            latestVersionName = latestVersionName!!,
            onUpdate = {
                latestVersionName = null
                UpdateChecker.openUpdatePage(context)
            },
            onLater = { latestVersionName = null }
        )
    }

    if (showAlarmSheet) {
        AlarmSettingsSheet(
            settings = settings,
            onDismiss = { showAlarmSheet = false },
            onToggleEnabled = { viewModel.setAlarmEnabled(it) },
            onTogglePercent = { viewModel.toggleAlarmTarget(it) },
            onSelectSnoozeMinutes = { viewModel.setAlarmSnoozeMinutes(it) },
            onToggleChargeLimit = { viewModel.setChargeLimitEnabled(it) },
            onSelectChargeLimitPercent = { viewModel.setChargeLimitPercent(it) },
            onToggleWithSound = { viewModel.setAlarmWithSound(it) }
        )
    }

    if (showAlertsSheet) {
        AlertSettingsSheet(
            settings = settings,
            onDismiss = { showAlertsSheet = false },
            onToggleTemp = { viewModel.setTempAlertEnabled(it) },
            onSelectTempThreshold = { viewModel.setTempAlertThreshold(it) },
            onToggleHealth = { viewModel.setHealthAlertEnabled(it) },
            onToggleLowBattery = { viewModel.setLowBatteryAlertEnabled(it) },
            onSelectLowBatteryThreshold = { viewModel.setLowBatteryThreshold(it) },
            onToggleRapidDischarge = { viewModel.setRapidDischargeAlertEnabled(it) },
            onSelectRapidWindow = { viewModel.setRapidDischargeWindow(it) },
            onSelectRapidDrop = { viewModel.setRapidDischargeDrop(it) }
        )
    }

    if (showSoundContextSheet) {
        SoundContextSettingsSheet(
            settings = settings,
            onDismiss = { showSoundContextSheet = false },
            onTogglePlugSpecific = { viewModel.setPlugSpecificSounds(it) },
            onSelectPreset = { plugType, presetId ->
                viewModel.setPlugPreset(plugType, presetId)
            },
            onToggleQuiet = { viewModel.setQuietHoursEnabled(it) },
            onSetQuietHours = { start, end -> viewModel.setQuietHours(start, end) }
        )
    }

    if (showThemeSheet) {
        ThemeSettingsSheet(
            settings = settings,
            onDismiss = { showThemeSheet = false },
            onSelectAccent = { viewModel.setAccentKey(it) },
            fontScale = settings.fontScale,
            onSelectFontScale = { viewModel.setFontScale(it) },
            dynamicColor = settings.dynamicColor,
            onToggleDynamicColor = { viewModel.setDynamicColor(it) },
            highContrast = settings.highContrast,
            onToggleHighContrast = { viewModel.setHighContrast(it) }
        )
    }

    if (showStatsSheet) {
        ChargeStatsSheet(
            records = chargeHistory,
            onDismiss = { showStatsSheet = false },
            onClear = { viewModel.clearChargeHistory() }
        )
    }

    // 커스텀 미디어 구간 선택.
    // 어느 파일을 자를지는 설정에 파일이 걸려 있는지(커스텀 사운드가 있으면 사운드로 판단)로
    // 알 수 있습니다 — 사용자가 어느 탭의 가위 버튼을 눌렀는지를 별도로 기억하지 않습니다.
    if (showTrimSheet) {
        val soundPath = settings.customSoundPath
        val videoPath = settings.customVideoPath
        val isSound = !soundPath.isNullOrBlank()
        val path = if (isSound) soundPath!! else videoPath
        if (!path.isNullOrBlank()) {
            TrimRangeSheet(
                path = path,
                initialStartMs = if (isSound) settings.customSoundStartMs else settings.customVideoStartMs,
                initialEndMs = if (isSound) settings.customSoundEndMs else settings.customVideoEndMs,
                titleRes = if (isSound) R.string.trim_sound_title else R.string.trim_video_title,
                onDismiss = { showTrimSheet = false },
                onApply = { start, end ->
                    if (isSound) viewModel.setCustomSoundRange(start, end)
                    else viewModel.setCustomVideoRange(start, end)
                }
            )
        }
    }
}

@Composable
fun BatteryLiveHeaderCard(
    batteryInfo: BatteryInfo,
    isServiceEnabled: Boolean,
    rateBadge: @Composable () -> Unit = {},
    onTestClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "test_pulse"
    )

    val scheme = MaterialTheme.colorScheme
    // 히어로 카드에는 이미지 자산을 쓰지 않고 절차적 그라디언트를 그립니다.
    // 이유: 강조색을 바꾸면 카드가 같이 따라가고, APK 가 가볍고,
    // 어떤 화면 비율에서도 잘리지 않습니다.
    val heroBrush = Brush.linearGradient(
        colors = listOf(
            ElectricCyan.copy(alpha = if (batteryInfo.isCharging) 0.20f else 0.05f),
            ElectricPurple.copy(alpha = if (batteryInfo.isCharging) 0.10f else 0.02f),
            Color.Transparent,
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen, vertical = Spacing.sm),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        shape = AppShapes.lg,
        border = BorderStroke(1.dp, if (batteryInfo.isCharging) ElectricCyan.copy(alpha = 0.45f) else scheme.outlineVariant)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
                    .background(heroBrush)
            )

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    if (batteryInfo.isCharging) ElectricCyan.copy(alpha = 0.25f) else Slate800,
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    if (batteryInfo.isCharging) ElectricCyan else Color.Transparent,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = if (batteryInfo.isCharging) ElectricCyan else ElectricAmber,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${batteryInfo.level}%",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = scheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text(
                                    text = if (batteryInfo.isCharging) {
                                        stringResource(R.string.battery_charging_short)
                                    } else {
                                        stringResource(R.string.battery_idle_short)
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (batteryInfo.isCharging) ElectricGreen else scheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }
                            Text(
                                text = stringResource(
                                    R.string.battery_header_plug,
                                    stringResource(batteryInfo.plugType.displayNameRes),
                                    batteryInfo.temperatureC
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                            // 충전 속도 배지 (충전 중 + 측정 가능할 때만 그려짐)
                            rateBadge()
                        }
                    }

                    Button(
                        onClick = onTestClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = Slate950
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .scale(pulseScale)
                            .testTag("test_charging_simulation_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.btn_test_charging),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { (batteryInfo.level / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(CircleShape),
                    color = if (batteryInfo.level > 20) ElectricCyan else ElectricAmber,
                    trackColor = Slate800
                )
            }
        }
    }
}

@Composable
fun SoundSettingsTab(
    settings: ChargeSettings,
    isPlayingPreview: Boolean,
    activePlayingId: String?,
    onPickAudio: () -> Unit,
    onClearAudio: () -> Unit,
    onTrimAudio: () -> Unit,
    hasSoundRange: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onSelectPreset: (String) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onPlayCurrent: () -> Unit,
    onStopSound: () -> Unit,
    onWatchAdToUnlock: (String) -> Unit,
    isAdFreeActive: Boolean,
    adFreeRemainingMinutes: Int,
    onWatchAdForAdFree: () -> Unit,
    onChooseAdFreeDuration: () -> Unit,
    overlayGranted: Boolean,
    onRequestOverlayPermission: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Sound Switch Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = AppShapes.md,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.sound_enable_title),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.sound_enable_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                    }

                    Switch(
                        checked = settings.soundEnabled,
                        onCheckedChange = onToggleSound,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Slate950,
                            checkedTrackColor = ElectricCyan,
                            uncheckedThumbColor = Slate400,
                            uncheckedTrackColor = Slate800
                        ),
                        modifier = Modifier.testTag("sound_enable_switch")
                    )
                }
            }
        }

        // Volume Slider & Visualizer Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = AppShapes.md,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val volumeIcon = when {
                                settings.volume <= 0.05f -> Icons.AutoMirrored.Filled.VolumeMute
                                settings.volume <= 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                                else -> Icons.AutoMirrored.Filled.VolumeUp
                            }
                            Icon(
                                imageVector = volumeIcon,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.sound_volume_title),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isPlayingPreview) {
                                PlayingIndicator()
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "${(settings.volume * 100).toInt()}%",
                                fontWeight = FontWeight.ExtraBold,
                                color = ElectricCyan,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Slider(
                        value = settings.volume,
                        onValueChange = onVolumeChange,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = Slate800
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("volume_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (isPlayingPreview) onStopSound() else onPlayCurrent()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("test_sound_preview_button")
                        ) {
                            Icon(
                                imageVector = if (isPlayingPreview) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPlayingPreview) {
                                    stringResource(R.string.btn_stop_sound)
                                } else {
                                    stringResource(R.string.btn_preview_volume)
                                },
                                color = ElectricCyan,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Custom MP3 Picker Section (Triggers Test Ad)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = AppShapes.md,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.sound_custom_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Text(
                        text = stringResource(R.string.sound_custom_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (settings.soundMode == SoundMode.CUSTOM && !settings.customSoundName.isNullOrBlank()) {
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = ElectricGreen.copy(alpha = 0.2f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = ElectricGreen,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = settings.customSoundName ?: "custom_audio.mp3",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White
                                        )
                                        Text(
                                            text = stringResource(R.string.sound_custom_applied),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricGreen
                                        )
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            if (isPlayingPreview && activePlayingId == "custom") onStopSound() else onPlayCurrent()
                                        },
                                        modifier = Modifier.testTag("custom_audio_preview_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingPreview && activePlayingId == "custom") Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = stringResource(R.string.a11y_play_short),
                                            tint = ElectricCyan
                                        )
                                    }
                                    // 구간 선택 — 파일을 자르지 않고 이 부분만 반복합니다.
                                    IconButton(
                                        onClick = onTrimAudio,
                                        modifier = Modifier.testTag("custom_audio_trim_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCut,
                                            contentDescription = stringResource(R.string.trim_open),
                                            tint = if (hasSoundRange) ElectricAmber else Slate400
                                        )
                                    }
                                    IconButton(
                                        onClick = onClearAudio,
                                        modifier = Modifier.testTag("custom_audio_delete_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.a11y_delete),
                                            tint = Slate400
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onPickAudio,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Slate800,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_mp3_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (settings.customSoundName != null) {
                                stringResource(R.string.btn_change_mp3)
                            } else {
                                stringResource(R.string.btn_pick_mp3)
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Built-in Presets Section (Triggers Test Ad on Selection)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = AppShapes.md,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.sound_presets_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.sound_presets_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 기본(무료) 프리셋 — 광고와 무관하게 항상 사용 가능합니다
                        SoundManager.FREE_PRESETS.forEach { preset ->
                        val isSelected = settings.soundMode == SoundMode.PRESET && settings.presetSoundId == preset.id
                        val isThisPlaying = isPlayingPreview && activePlayingId == preset.id

                        Surface(
                            color = if (isSelected) ElectricCyan.copy(alpha = 0.12f) else Slate800,
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) BorderStroke(1.dp, ElectricCyan) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSelectPreset(preset.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.ElectricBolt,
                                        contentDescription = null,
                                        tint = if (isSelected) ElectricCyan else Slate400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = stringResource(preset.nameRes),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isSelected) ElectricCyan else Color.White
                                        )
                                        Text(
                                            text = stringResource(preset.descriptionRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate400
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (isThisPlaying) onStopSound() else onSelectPreset(preset.id)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.a11y_play),
                                        tint = if (isSelected) ElectricCyan else Slate400
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 프리미엄 사운드는 상단 점3개(⋯) 메뉴로 옮겼습니다 ──

        // "광고 없이 사용하기" 카드
        item {
            AdFreeCard(
                remainingMinutes = adFreeRemainingMinutes,
                isAdFreeActive = isAdFreeActive,
                onWatchAd = onWatchAdForAdFree,
                onChooseDuration = onChooseAdFreeDuration
            )
        }

        // 네이티브 광고
        item {
            AdNativeCard(isAdFreeActive = isAdFreeActive)
        }

        // Haptic Vibration Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = AppShapes.md,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = ElectricAmber.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Vibration,
                                    contentDescription = null,
                                    tint = ElectricAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.vibration_title),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.vibration_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                    }

                    Switch(
                        checked = settings.vibrationEnabled,
                        onCheckedChange = onToggleVibration,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Slate950,
                            checkedTrackColor = ElectricAmber,
                            uncheckedThumbColor = Slate400,
                            uncheckedTrackColor = Slate800
                        ),
                        modifier = Modifier.testTag("vibration_toggle_switch")
                    )
                }
            }
        }

        // "다른 앱 위에 표시" 권한 카드
        // Android 10+ 는 백그라운드 액티비티 시작을 차단하므로 이 권한이 없으면
        // 다른 앱 위에 충전 애니메이션을 띄울 수 없습니다.
        item {
            OverlayPermissionCard(
                isGranted = overlayGranted,
                onRequestPermission = onRequestOverlayPermission
            )
        }
    }
}

@Composable
fun AnimationSettingsTab(
    settings: ChargeSettings,
    onPickVideo: () -> Unit,
    onClearVideo: () -> Unit,
    onTrimVideo: () -> Unit,
    hasVideoRange: Boolean,
    onToggleAnimation: (Boolean) -> Unit,
    onSelectAnimationMode: (AnimationMode) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onToggleLoop: (Boolean) -> Unit,
    onPreviewFullScreen: () -> Unit,
    onWatchAdToUnlock: (String) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Animation Toggle Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = AppShapes.md,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = ElectricAmber.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Movie,
                                    contentDescription = null,
                                    tint = ElectricAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.anim_enable_title),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.anim_enable_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                    }

                    Switch(
                        checked = settings.animationEnabled,
                        onCheckedChange = onToggleAnimation,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Slate950,
                            checkedTrackColor = ElectricAmber,
                            uncheckedThumbColor = Slate400,
                            uncheckedTrackColor = Slate800
                        ),
                        modifier = Modifier.testTag("animation_enable_switch")
                    )
                }
            }
        }

        // Custom Video Picker Card (MP4, AVI, MKV)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = AppShapes.md,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = ElectricAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.anim_custom_title),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                    Text(
                        text = stringResource(R.string.anim_custom_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (settings.animationMode == AnimationMode.CUSTOM_VIDEO && !settings.customVideoName.isNullOrBlank()) {
                        Surface(
                            color = ElectricAmber.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ElectricAmber.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = ElectricAmber.copy(alpha = 0.2f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = ElectricAmber,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = settings.customVideoName ?: "custom_video.mp4",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White
                                        )
                                        Text(
                                            text = stringResource(R.string.anim_custom_applied),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ElectricAmber
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onTrimVideo,
                                    modifier = Modifier.testTag("custom_video_trim_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCut,
                                        contentDescription = stringResource(R.string.trim_open),
                                        tint = if (hasVideoRange) ElectricAmber else Slate400
                                    )
                                }
                                IconButton(
                                    onClick = onClearVideo,
                                    modifier = Modifier.testTag("custom_video_delete_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.a11y_delete),
                                        tint = Slate400
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onPickVideo,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Slate800,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_video_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            tint = ElectricAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (settings.customVideoName != null) {
                                stringResource(R.string.btn_change_video)
                            } else {
                                stringResource(R.string.btn_pick_video)
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // NEW: Playback Duration SLIDER Card (requested by user)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = AppShapes.md,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.anim_duration_title),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }

                        // Prominent current duration badge
                        Surface(
                            color = ElectricCyan.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = settings.durationLabel(context),
                                color = ElectricCyan,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.anim_duration_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Special mode mode chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isSliderMode = settings.videoDurationSeconds in 1..30
                        FilterChip(
                            selected = isSliderMode,
                            onClick = { if (!isSliderMode) onSelectDuration(5) },
                            label = { Text(stringResource(R.string.anim_chip_slider), style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = Slate950
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = settings.videoDurationSeconds == -1,
                            onClick = { onSelectDuration(-1) },
                            label = { Text(stringResource(R.string.anim_chip_full), style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = Slate950
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = settings.videoDurationSeconds == 0,
                            onClick = { onSelectDuration(0) },
                            label = { Text(stringResource(R.string.anim_chip_touch), style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = Slate950
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // The Slider Bar
                    val currentSliderValue = if (settings.videoDurationSeconds in 1..30) {
                        settings.videoDurationSeconds.toFloat()
                    } else {
                        5f
                    }

                    Slider(
                        value = currentSliderValue,
                        onValueChange = { onSelectDuration(it.toInt()) },
                        valueRange = 1f..30f,
                        steps = 28, // 1초 단위로 스냅
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = Slate800
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("video_duration_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf(1, 10, 20, 30).forEach { tick ->
                        Text(
                            text = stringResource(R.string.duration_label_seconds, tick),
                            color = Slate600,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Audio Fade-out Info Callout
                    Surface(
                        color = Slate800.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = ElectricGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.anim_fadeout_note),
                                color = ElectricGreen,
                                style = MaterialTheme.typography.labelSmall,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Video Loop Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.anim_loop_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.anim_loop_sub),
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400
                            )
                        }
                        Switch(
                            checked = settings.videoLoop,
                            onCheckedChange = onToggleLoop,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Slate950,
                                checkedTrackColor = ElectricCyan,
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = Slate800
                            )
                        )
                    }
                }
            }
        }

        // Built-in Visual Themes
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = AppShapes.md,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.anim_builtin_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.anim_builtin_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val builtInModes = listOf(
                        AnimationMode.BUILTIN_LIGHTNING,
                        AnimationMode.BUILTIN_REACTOR,
                        AnimationMode.BUILTIN_PLASMA
                    )

                    builtInModes.forEach { mode ->
                        val isSelected = settings.animationMode == mode
                        Surface(
                            color = if (isSelected) ElectricAmber.copy(alpha = 0.12f) else Slate800,
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) BorderStroke(1.dp, ElectricAmber) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onSelectAnimationMode(mode) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = if (isSelected) ElectricAmber else Slate400,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = stringResource(mode.nameRes),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isSelected) ElectricAmber else Color.White
                                        )
                                        Text(
                                            text = stringResource(mode.descRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Slate400
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 프리미엄 애니메이션 테마는 상단 점3개(⋯) 메뉴로 옮겼습니다 ──

        // Fullscreen Preview Action Button
        item {
            Button(
                onClick = onPreviewFullScreen,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricAmber,
                    contentColor = Slate950
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("preview_fullscreen_animation_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_preview_fullscreen),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

/**
 * "광고 없는 시간" 선택 다이얼로그.
 *
 * 시간을 고르면 보상형 광고가 표시되고, 광고를 끝까지 시청하면 해당 시간이 시작됩니다.
 * 광고가 로드되지 않은 경우에는 광고 없이도 시간을 시작합니다 (기능을 막지 않기 위함).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdFreeDurationDialog(
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Slate900,
            border = BorderStroke(1.dp, ElectricAmber.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.adfree_dialog_title),
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.adfree_dialog_sub),
                    color = Slate400,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                PremiumCatalog.AD_FREE_DURATION_OPTIONS.forEach { minutes ->
                    Surface(
                        color = Slate800,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Slate700),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(minutes) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(
                                when {
                                    minutes < 60 -> R.string.duration_minutes
                                    minutes < 60 * 24 -> R.string.duration_hours
                                    else -> R.string.duration_days
                                },
                                when {
                                    minutes < 60 -> minutes
                                    minutes < 60 * 24 -> minutes / 60
                                    else -> minutes / (60 * 24)
                                }
                            ),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = ElectricAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.btn_cancel),
                    color = Slate400,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * 사운드 재생 중 표시되는 이퀄라이저 인디케이터.
 */
@Composable
private fun PlayingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val heights = listOf(10, 18, 26, 14).mapIndexed { index, maxHeight ->
        infiniteTransition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 420 + index * 110,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eq_$index"
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.testTag("playing_indicator")
    ) {
        heights.forEach { height ->
            val fraction by height
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((18 * fraction).dp)
                    .background(ElectricCyan, RoundedCornerShape(2.dp))
            )
        }
    }
}

/**
 * 배터리 정보 탭.
 *
 * 배터리 상태, 온도, 전압, 건강도, 충전 방식 등 기기 정보를 표시합니다.
 * 여기에는 광고를 배치하지 않습니다. 이 탭은 정보 조회 기능으로,
 * 광고 정책상 광고로 기능 사용을 방해하면 안 됩니다.
 */
@Composable
fun BatteryInfoTab(batteryInfo: BatteryInfo) {
    // ── 충전 출력(와트) ────────────────────────────────────────────
    //
    // 이 화면 안에 sampling 을 두는 이유: 폴링이 화면에 보이는 동안에만 돌게 됩니다.
    // 메인 탭에 두면 사용자가 이 탭을 안 볼 때도 배터리를 갉아먹습니다.
    //
    // 에너지원을 시간차 두 번 읽어 미분해야 하므로 배터리가 바뀌었다는 브로드캐스트
    // 만으로는 값이 나오지 않습니다. 그래서 별도 주기로 샘플을 쌓습니다.
    val context = LocalContext.current
    val powerTracker = remember { PowerTracker.get(context) }
    var watts by remember { mutableStateOf<Float?>(null) }

    // 지원 여부는 한 번만 확인합니다. 기기가 값을 안 주면 몇 번을 재도 0 이므로
    // 폴링 자체를 아예 시작하지 않습니다.
    val powerSupported = remember {
        powerTracker.hasEverMeasured || PowerTracker.isSupported(context)
    }

    LaunchedEffect(Unit) {
        while (powerSupported) {
            watts = powerTracker.sample()
            delay(6_000L)
        }
        watts = null
    }

    // 충전이 끊기면 미분 기준이 사라지므로 버립니다. 다음 충전에서 새 기준으로 시작합니다.
    LaunchedEffect(batteryInfo.isCharging) {
        if (!batteryInfo.isCharging) {
            powerTracker.reset()
            watts = null
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 충전 상태 헤더
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = AppShapes.md,
                border = BorderStroke(
                    1.dp,
                    if (batteryInfo.isCharging) ElectricGreen.copy(alpha = 0.5f) else Slate800
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (batteryInfo.isCharging) {
                                    stringResource(R.string.battery_charging_short)
                                } else {
                                    stringResource(R.string.battery_discharging)
                                },
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.headlineMedium,
                                color = if (batteryInfo.isCharging) ElectricGreen else Color.White
                            )
                            Text(
                                text = stringResource(batteryInfo.statusRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate400
                            )
                        }
                        Text(
                            text = "${batteryInfo.level}%",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.displayMedium,
                            color = if (batteryInfo.level > 20) ElectricCyan else ElectricAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LinearProgressIndicator(
                        progress = { (batteryInfo.level / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (batteryInfo.level > 20) ElectricCyan else ElectricAmber,
                        trackColor = Slate800
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(batteryInfo.plugType.displayNameRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                }
            }
        }

        // 상세 정보 카드들
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BatteryInfoRow(
                    icon = Icons.Default.Info,
                    label = stringResource(R.string.battery_temp_label),
                    value = stringResource(R.string.value_celsius, batteryInfo.temperatureC)
                )
                BatteryInfoRow(
                    icon = Icons.Default.ElectricBolt,
                    label = stringResource(R.string.battery_voltage_label),
                    value = stringResource(R.string.value_volts, batteryInfo.voltageV)
                )
                // ── 충전 출력 (W) ──────────────────────────────────────
                //
                // 값을 못 내는 기기가 많으므로 세 갈래로 나눕니다.
                //   측정 불가 (기기 미지원) → 한 줄 설명
                //   측정 중                 → 흐린 "측정 중"
                //   값 있음                 → 실제 와트 + 방향
                //
                // 0W 로 보여주는 것은 절대 하지 않습니다. 충전기가 안 물고 있다는
                // 잘못된 결론을 만들게 됩니다.
                //
                // 위임 프로퍼티는 smart cast 가 되지 않으므로 지역 값으로 꺼냅니다.
                val currentWatts = watts
                BatteryInfoRow(
                    icon = Icons.Default.Bolt,
                    label = stringResource(R.string.power_label),
                    value = when {
                        !powerSupported -> stringResource(R.string.power_unsupported)
                        currentWatts == null -> stringResource(R.string.power_measuring)
                        currentWatts >= 0f -> stringResource(
                            R.string.power_watts_charging,
                            formatWatts(currentWatts)
                        )

                        else -> stringResource(
                            R.string.power_watts_discharging,
                            formatWatts(-currentWatts)
                        )
                    },
                    valueTint = when {
                        currentWatts == null -> Slate600
                        // 색은 "많이 흐르면 진하게" 뿐 급속/저속 판정이 아닙니다.
                        // 그 판정은 배터리 용량 대비 비율이라 이 데이터로는 못 합니다.
                        currentWatts >= 20f -> ElectricGreen
                        currentWatts >= 8f -> ElectricCyan
                        else -> Slate400
                    }
                )
                // ── 배터리 건강도 (SOH) 대신 ──────────────────────────
                //
                // 숫자로 넣지 않았습니다. Android 에는 만충 용량 API 가 없고,
                // 설계 용량(BATTERY_PROPERTY_DESIGN_CAPACITY)은 AOSP @SystemApi 로
                // 공개 SDK 에 없어 컴파일조차 되지 않습니다. 즉 "SOH 92%" 를
                // 띄우려면 반드시 지어내야 하고, 사용자가 가장 신뢰하는 자리에서
                // 거짓말을 하게 됩니다.
                //
                // 대신 온도·충전 습관 기반 인사이트를 통계 시트에 넣었습니다
                // (BatteryInsights). 전부 이 앱이 직접 기록한 값에서 나옵니다.
                BatteryInfoRow(
                    icon = Icons.Default.Check,
                    label = stringResource(R.string.battery_health_label),
                    value = stringResource(batteryInfo.healthRes)
                )
                BatteryInfoRow(
                    icon = Icons.Default.Power,
                    label = stringResource(R.string.battery_technology_label),
                    value = batteryInfo.technology
                )
                BatteryInfoRow(
                    icon = Icons.Default.Audiotrack,
                    label = stringResource(R.string.battery_status_label),
                    value = stringResource(batteryInfo.statusRes)
                )
            }
        }

        // 온도 경고
        if (batteryInfo.temperatureC >= 40f) {
            item {
                Surface(
                    color = ElectricAmber.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ElectricAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = ElectricAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.battery_temp_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = ElectricAmber
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueTint: Color = Color.White
) {
    Surface(
        color = Slate900,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Slate400,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = Slate400
                )
            }
            // SOH / 전력처럼 "값이 없을 수 있는" 행이 들어오게 되었습니다.
            // 측정 불가능한 항목도 흰 글씨로 뜨면 이상해 보이므로 회색으로 물러나게 합니다.
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueTint,
                modifier = Modifier.padding(start = Spacing.md)
            )
        }
    }
}

/**
 * 와트를 읽기 좋게 만듭니다.
 *
 * 1 미만은 소수 첫째 자리, 그 이상은 정수 + 소수 첫째 자리.
 * 계측 자체가 ± 오차를 가지므로 12.37W 같은 정밀도를 내보내면
 * 정확해 보이지만 오히려 신뢰를 갉아먹습니다.
 */
internal fun formatWatts(watts: Float): String {
    val v = if (watts < 0f) 0f else watts
    return if (v < 10f) {
        String.format(java.util.Locale.getDefault(), "%.1f", v)
    } else {
        String.format(java.util.Locale.getDefault(), "%.0f", v)
    }
}
