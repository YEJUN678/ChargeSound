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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NotificationAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ad.AdManager
import com.example.audio.VoiceRecorder
import com.example.battery.BatteryInfo
import com.example.battery.PowerTracker
import com.example.data.AnimationMode
import com.example.data.AdFreePlan
import com.example.data.ChargeSettings
import com.example.data.PremiumCatalog
import com.example.data.SoundMode
import com.example.data.SoundPreset
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
import com.example.ui.theme.Motion
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
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
    var showVoiceRecorder by remember { mutableStateOf(false) }
    var showEdgeGlowSheet by remember { mutableStateOf(false) }
    var showThemeSheet by remember { mutableStateOf(false) }
    var showScenesSheet by remember { mutableStateOf(false) }
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

    // 커스텀 알림음 파일 피커 (충전 완료 / 저전력 / 온도 경고)
    var pendingAlertSoundTarget by remember { mutableStateOf<String?>(null) }
    val alertSoundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            when (pendingAlertSoundTarget) {
                "charge" -> viewModel.setChargeCompleteSound(it.toString())
                "low" -> viewModel.setLowBatterySound(it.toString())
                "temp" -> viewModel.setTemperatureSound(it.toString())
            }
        }
        pendingAlertSoundTarget = null
    }

    // 마이크 권한 — 허용하면 곧바로 녹음 다이얼로그를 엽니다.
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showVoiceRecorder = true
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
                        onOpenScenes = { showScenesSheet = true },
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
        // 원터치 퀵 액션 — 미리듣기 ▶ / ■ (음소거 시 🔇). 설정 탭을 3번 오가는 대신 1번 탭.
        floatingActionButton = {
            val fabColor = when {
                !settings.soundEnabled -> ElectricAmber
                isPlayingPreview -> ElectricCyan
                else -> ElectricCyan
            }
            ExtendedFloatingActionButton(
                onClick = {
                    when {
                        !settings.soundEnabled -> viewModel.toggleSoundEnabled(true)
                        isPlayingPreview -> {
                            viewModel.stopPreviewSound()
                            AdManager.setCoreFeatureBusy(false)
                        }
                        else -> {
                            // 미리듣기는 앱 핵심 기능이므로 전면류 광고를 차단합니다
                            AdManager.setCoreFeatureBusy(true)
                            viewModel.previewPlayCurrentSound()
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = when {
                            !settings.soundEnabled -> Icons.AutoMirrored.Filled.VolumeOff
                            isPlayingPreview -> Icons.Default.Stop
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = stringResource(
                            when {
                                !settings.soundEnabled -> R.string.a11y_sound_off
                                isPlayingPreview -> R.string.btn_stop_sound
                                else -> R.string.a11y_play
                            }
                        )
                    )
                },
                text = {
                    Text(
                        text = stringResource(
                            when {
                                !settings.soundEnabled -> R.string.fab_unmute
                                isPlayingPreview -> R.string.fab_stop_preview
                                else -> R.string.fab_preview
                            }
                        ),
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                containerColor = fabColor.copy(alpha = 0.16f),
                contentColor = fabColor,
                modifier = Modifier.testTag("quick_preview_fab")
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Live Battery & Quick Test Banner with Hero Asset
            BatteryLiveHeaderCard(
                batteryInfo = batteryInfo,
                isServiceEnabled = settings.isServiceEnabled,
                chargeRatePerHour = session.percentPerHour,
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
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.screen, vertical = Spacing.xs),
                shape = AppShapes.lg,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.Transparent,
                    contentColor = ElectricCyan,
                    divider = {},
                    indicator = { tabPositions ->
                        TabRowDefaults.PrimaryIndicator(
                            modifier = Modifier
                                .tabIndicatorOffset(tabPositions[selectedTabIndex])
                                .padding(horizontal = Spacing.lg)
                                // 선택된 탭 아래로 시안 글로우가 번집니다
                                .shadow(10.dp, RoundedCornerShape(4.dp), spotColor = ElectricCyan),
                            width = 32.dp,
                            color = ElectricCyan,
                        )
                    },
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
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = selectedTabIndex,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (fadeIn(animationSpec = tween(Motion.MEDIUM_MILLIS)) +
                            slideInHorizontally(
                                animationSpec = tween(Motion.MEDIUM_MILLIS),
                                initialOffsetX = { width -> if (forward) width / 8 else -width / 8 }
                            )) togetherWith
                            (fadeOut(animationSpec = tween(Motion.FAST_MILLIS)) +
                                slideOutHorizontally(
                                    animationSpec = tween(Motion.FAST_MILLIS),
                                    targetOffsetX = { width -> if (forward) -width / 12 else width / 12 }
                                ))
                    },
                    label = "tab_content"
                ) { tabIndex ->
                when (tabIndex) {
                    0 -> SoundSettingsTab(
                        settings = settings,
                        isPlayingPreview = isPlayingPreview,
                        activePlayingId = activePlayingId,
                        onPickAudio = { audioPickerLauncher.launch("audio/*") },
                        onRecordAudio = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                showVoiceRecorder = true
                            } else {
                                recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        onClearAudio = { viewModel.clearCustomAudio() },
                        onTrimAudio = { showTrimSheet = true },
                        hasSoundRange = settings.customSoundEndMs > settings.customSoundStartMs,
                        onToggleSound = { viewModel.toggleSoundEnabled(it) },
                        onToggleAmbientSound = { viewModel.setAmbientSoundEnabled(it) },
                        onAmbientVolumeChange = { viewModel.setAmbientSoundVolume(it) },
                        onVolumeChange = { viewModel.setVolume(it) },
                        onChannelVolumeChange = { viewModel.setChannelVolume(it) },
                        onPanChange = { viewModel.setPan(it) },
                        onEqPresetChange = { viewModel.setEqPreset(it) },
                        onPickAlertSound = { target ->
                            pendingAlertSoundTarget = target
                            alertSoundPickerLauncher.launch("audio/*")
                        },
                        onClearAlertSound = { target ->
                            when (target) {
                                "charge" -> viewModel.setChargeCompleteSound(null)
                                "low" -> viewModel.setLowBatterySound(null)
                                "temp" -> viewModel.setTemperatureSound(null)
                            }
                        },
                        onWidgetSkinStyleChange = { viewModel.setWidgetSkinStyle(it) },
                        onWidgetBgColorChange = { viewModel.setWidgetBackgroundColor(it) },
                        onWidgetTextColorChange = { viewModel.setWidgetTextColor(it) },
                        onRingStyleChange = { thickness, cap, glow -> viewModel.setRingStyle(thickness, cap, glow) },
                        onSelectPreset = { viewModel.selectPresetSound(it) },
                        onPreviewPreset = { viewModel.previewPlaySound(it) },
                        onToggleVibration = { viewModel.toggleVibration(it) },
                        onToggleVoiceAnnounce = { viewModel.setVoiceAnnounce(it) },
                        onSaveVoiceTemplates = { start, target -> viewModel.setVoiceTemplates(start, target) },
                        onToggleLevelTones = { viewModel.setLevelTones(it) },
                        onToggleShuffle = { viewModel.setShuffleSounds(it) },
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
                        onWatchAdForAdFree = {
                            viewModel.watchAdForAdFree(activity, PremiumCatalog.DEFAULT_AD_FREE_PLAN)
                        },
                        onChooseAdFreeDuration = { showAdFreeDialog = true },
                        onToggleEdgeGlow = { viewModel.setEdgeGlowEnabled(it) },
                        onToggleEdgeGlowPersistent = { viewModel.setEdgeGlowPersistent(it) },
                        onPreviewEdgeGlow = { viewModel.previewEdgeGlow() },
                        onCustomizeEdgeGlow = { showEdgeGlowSheet = true },
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
            settings = settings,
            onDismiss = { showAdFreeDialog = false },
            onSelect = { plan -> viewModel.watchAdForAdFree(activity, plan) }
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
            onToggleWithSound = { viewModel.setAlarmWithSound(it) },
            onRoutineChange = { sound, tts, vibration, animation, notification ->
                viewModel.setCompletionRoutine(sound, tts, vibration, animation, notification)
            }
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
            onSetQuietHours = { start, end -> viewModel.setQuietHours(start, end) },
            onToggleEdgeGlow = { viewModel.setEdgeGlowEnabled(it) },
            onToggleEdgeGlowPersistent = { viewModel.setEdgeGlowPersistent(it) }
        )
    }

    if (showVoiceRecorder) {
        VoiceRecorderDialog(
            onDismiss = { showVoiceRecorder = false },
            onRecorded = { file ->
                showVoiceRecorder = false
                viewModel.applyRecordedAudio(file)
            }
        )
    }

    if (showEdgeGlowSheet) {
        EdgeGlowSettingsSheet(
            settings = settings,
            onDismiss = { showEdgeGlowSheet = false },
            onOpacityChange = { viewModel.setEdgeGlowOpacity(it) },
            onSweepChange = { viewModel.setEdgeGlowSweepMs(it) },
            onToggleAutoColor = { viewModel.setEdgeGlowAutoColor(it) },
            onColorStartChange = { viewModel.setEdgeGlowColorStart(it) },
            onColorMidChange = { viewModel.setEdgeGlowColorMid(it) },
            onColorEndChange = { viewModel.setEdgeGlowColorEnd(it) },
            onToggleGauge = { viewModel.setEdgeGlowGauge(it) },
            onToggleBreathing = { viewModel.setEdgeGlowBreathing(it) },
            onToggleComet = { viewModel.setEdgeGlowComet(it) },
            onToggleSparks = { viewModel.setEdgeGlowSparks(it) },
            onTogglePill = { viewModel.setEdgeGlowPill(it) },
            onToggleLiquid = { viewModel.setEdgeGlowLiquid(it) },
            onApplyPreset = { start, mid, end ->
                viewModel.applyEdgeGlowPalette(start, mid, end)
            },
            onPreview = { viewModel.previewEdgeGlow() }
        )
    }

    if (showThemeSheet) {
        ThemeSettingsSheet(
            settings = settings,
            onDismiss = { showThemeSheet = false },
            onSelectAccent = { viewModel.setAccentKey(it) },
            onCustomAccentChange = { viewModel.setCustomAccentArgb(it) },
            fontScale = settings.fontScale,
            onSelectFontScale = { viewModel.setFontScale(it) },
            dynamicColor = settings.dynamicColor,
            onToggleDynamicColor = { viewModel.setDynamicColor(it) },
            highContrast = settings.highContrast,
            onToggleHighContrast = { viewModel.setHighContrast(it) }
        )
    }
    if (showScenesSheet) {
        ChargeScenesSheet(
            onDismiss = { showScenesSheet = false },
            onApply = { scene -> viewModel.applyScene(scene); showScenesSheet = false }
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
    chargeRatePerHour: Float? = null,
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
    val batteryPulse by infiniteTransition.animateFloat(
        initialValue = 0.99f,
        targetValue = 1.018f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "battery_percent_pulse"
    )
    val glowStrength by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "battery_percent_glow"
    )

    val scheme = MaterialTheme.colorScheme
    // 링은 위험(온도)을 가장 우선해서 알리고, 그 다음으로 완충·급속·일반 상태를 구분합니다.
    val ringColor = when {
        batteryInfo.temperatureC >= 42f -> Color(0xFFFB7185)
        batteryInfo.temperatureC >= 38f -> ElectricAmber
        batteryInfo.isCharging && batteryInfo.level >= 100 -> ElectricGreen
        batteryInfo.isCharging && (chargeRatePerHour ?: 0f) >= 20f -> ElectricPurple
        batteryInfo.isCharging -> ElectricCyan
        batteryInfo.level <= 20 -> ElectricAmber
        else -> Slate400
    }
    // 히어로 카드에는 이미지 자산을 쓰지 않고 절차적 그라디언트를 그립니다.
    // 이유: 강조색을 바꾸면 카드가 같이 따라가고, APK 가 가볍고,
    // 어떤 화면 비율에서도 잘리지 않습니다.
    val heroBrush = Brush.linearGradient(
        colors = listOf(
            ElectricCyan.copy(alpha = if (batteryInfo.isCharging) 0.32f else 0.10f),
            ElectricPurple.copy(alpha = if (batteryInfo.isCharging) 0.18f else 0.07f),
            Color.Transparent,
        )
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.screen, vertical = Spacing.sm),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        shape = AppShapes.xl,
        border = BorderStroke(1.dp, ringColor.copy(alpha = if (batteryInfo.isCharging) 0.60f else 0.38f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .background(heroBrush)
            )

            Column(modifier = Modifier.padding(Spacing.lg)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .background(
                                    ringColor.copy(alpha = if (batteryInfo.isCharging) 0.22f else 0.14f),
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    ringColor.copy(alpha = if (batteryInfo.isCharging) 0.9f else 0.5f),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { (batteryInfo.level / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.size(58.dp),
                                color = ringColor,
                                trackColor = Color.Transparent,
                                strokeWidth = 2.dp
                            )
                            Icon(
                                imageVector = if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = ringColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${batteryInfo.level}%",
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        shadow = Shadow(
                                            color = ElectricCyan.copy(
                                                alpha = if (batteryInfo.isCharging) glowStrength else 0.16f
                                            ),
                                            offset = Offset.Zero,
                                            blurRadius = if (batteryInfo.isCharging) 18f else 8f
                                        )
                                    ),
                                    color = scheme.onSurface,
                                    modifier = Modifier.scale(if (batteryInfo.isCharging) batteryPulse else 1f)
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
                            Surface(
                                color = scheme.surface.copy(alpha = 0.46f),
                                shape = AppShapes.pill,
                                border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.padding(top = Spacing.xs)
                            ) {
                                Text(
                                    text = stringResource(
                                        R.string.battery_header_plug,
                                        stringResource(batteryInfo.plugType.displayNameRes),
                                        batteryInfo.temperatureC
                                    ),
                                    style = AppText.Badge,
                                    color = scheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs)
                                )
                            }
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
                        shape = AppShapes.pill,
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

                Spacer(modifier = Modifier.height(Spacing.md))

                LinearProgressIndicator(
                    progress = { (batteryInfo.level / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = ringColor,
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
    onRecordAudio: () -> Unit,
    onClearAudio: () -> Unit,
    onTrimAudio: () -> Unit,
    hasSoundRange: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onToggleAmbientSound: (Boolean) -> Unit,
    onAmbientVolumeChange: (Float) -> Unit,
    onVolumeChange: (Float) -> Unit,
    onChannelVolumeChange: (Float) -> Unit,
    onPanChange: (Float) -> Unit,
    onEqPresetChange: (String) -> Unit,
    onPickAlertSound: (String) -> Unit,
    onClearAlertSound: (String) -> Unit,
    onWidgetSkinStyleChange: (String) -> Unit,
    onWidgetBgColorChange: (Long?) -> Unit,
    onWidgetTextColorChange: (Long?) -> Unit,
    onRingStyleChange: (Float?, String?, Boolean?) -> Unit,
    onSelectPreset: (String) -> Unit,
    onPreviewPreset: (String) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onToggleVoiceAnnounce: (Boolean) -> Unit,
    onSaveVoiceTemplates: (String, String) -> Unit,
    onToggleLevelTones: (Boolean) -> Unit,
    onToggleShuffle: (Boolean) -> Unit,
    onPlayCurrent: () -> Unit,
    onStopSound: () -> Unit,
    onWatchAdToUnlock: (String) -> Unit,
    isAdFreeActive: Boolean,
    adFreeRemainingMinutes: Int,
    onWatchAdForAdFree: () -> Unit,
    onChooseAdFreeDuration: () -> Unit,
    onToggleEdgeGlow: (Boolean) -> Unit,
    onToggleEdgeGlowPersistent: (Boolean) -> Unit,
    onPreviewEdgeGlow: () -> Unit,
    onCustomizeEdgeGlow: () -> Unit,
    overlayGranted: Boolean,
    onRequestOverlayPermission: () -> Unit
) {
    val context = LocalContext.current

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

        // 엣지 라이팅 — 사운드 탭에서 바로 보이도록 별도 카드로 노출합니다.
        item {
            EdgeGlowSettingsCard(
                settings = settings,
                overlayGranted = overlayGranted,
                onToggle = onToggleEdgeGlow,
                onTogglePersistent = onToggleEdgeGlowPersistent,
                onPreview = onPreviewEdgeGlow,
                onCustomize = onCustomizeEdgeGlow,
                onRequestOverlayPermission = onRequestOverlayPermission
            )
        }

        // 충전 앰비언트 — 충전 중에만 은은한 패드가 흐릅니다.
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = AppShapes.md,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = ElectricPurple.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Audiotrack,
                                    contentDescription = null,
                                    tint = ElectricPurple,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.ambient_sound_title),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.ambient_sound_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                        Switch(
                            checked = settings.ambientSoundEnabled,
                            onCheckedChange = onToggleAmbientSound,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Slate950,
                                checkedTrackColor = ElectricPurple,
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = Slate800
                            ),
                            modifier = Modifier.testTag("ambient_sound_switch")
                        )
                    }
                    AnimatedVisibility(visible = settings.ambientSoundEnabled) {
                        Column {
                            Text(
                                text = stringResource(R.string.ambient_sound_note),
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.ambient_sound_volume_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Slate400
                                )
                                Text(
                                    text = stringResource(
                                        R.string.ambient_sound_volume_value,
                                        (settings.ambientSoundVolume * 100).toInt()
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricPurple
                                )
                            }
                            Slider(
                                value = settings.ambientSoundVolume,
                                onValueChange = onAmbientVolumeChange,
                                valueRange = 0.05f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = ElectricPurple,
                                    activeTrackColor = ElectricPurple,
                                    inactiveTrackColor = Slate700
                                ),
                                modifier = Modifier.testTag("ambient_volume_slider")
                            )
                            Text(
                                text = stringResource(R.string.ambient_sound_volume_sub),
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate600
                            )
                        }
                    }
                }
            }
        }

        // 음성 안내 · 레벨 톤 · 셔플 — 모두 "알림 방식" 설정이라 한 카드에 묶습니다.
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = AppShapes.md,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    ToggleRow(
                        title = stringResource(R.string.voice_announce_title),
                        subtitle = stringResource(R.string.voice_announce_sub),
                        checked = settings.voiceAnnounceEnabled,
                        onCheckedChange = onToggleVoiceAnnounce
                    )
                    if (settings.voiceAnnounceEnabled) {
                        VoiceTemplateEditor(
                            startTemplate = settings.voiceStartTemplate,
                            targetTemplate = settings.voiceTargetTemplate,
                            onSave = onSaveVoiceTemplates
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    ToggleRow(
                        title = stringResource(R.string.level_tone_title),
                        subtitle = stringResource(R.string.level_tone_sub),
                        checked = settings.levelTonesEnabled,
                        onCheckedChange = onToggleLevelTones
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                    ToggleRow(
                        title = stringResource(R.string.shuffle_title),
                        subtitle = stringResource(R.string.shuffle_sub),
                        checked = settings.shuffleSoundsEnabled,
                        onCheckedChange = onToggleShuffle
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // 앱에서 바로 3~10초 녹음해 충전음으로 사용
                    Button(
                        onClick = onRecordAudio,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricPurple.copy(alpha = 0.18f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("record_voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = ElectricPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_record_voice),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 유튜브 음원 무료 다운로드 — 외부 사이트로 이동
                    OutlinedButton(
                        onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                Uri.parse("https://ybaudio.hiroobabo0.workers.dev")
                            )
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // 브라우저가 없으면 무시합니다
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Slate800,
                            contentColor = ElectricCyan
                        ),
                        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("youtube_audio_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_youtube_audio),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = stringResource(R.string.youtube_audio_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400,
                        modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                    )
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

                    // 기본(무료) 프리셋 — 앨범형 2열 그리드.
                    // 탭 = 선택(자동 미리듣기), 길게 누르기 = 설정을 바꾸지 않고 미리듣기만.
                    SoundManager.FREE_PRESETS.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { preset ->
                                PresetAlbumCard(
                                    preset = preset,
                                    coverIndex = SoundManager.FREE_PRESETS.indexOf(preset),
                                    isSelected = settings.soundMode == SoundMode.PRESET &&
                                        settings.presetSoundId == preset.id,
                                    isPlaying = isPlayingPreview && activePlayingId == preset.id,
                                    onSelect = { onSelectPreset(preset.id) },
                                    onPreviewToggle = {
                                        if (isPlayingPreview && activePlayingId == preset.id) {
                                            onStopSound()
                                        } else {
                                            onPreviewPreset(preset.id)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
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

        // 사운드 믹서 — 채널 볼륨 · 스테레오 팬 · EQ
        item {
            SoundLayerMixerCard(
                settings = settings,
                isPlayingPreview = isPlayingPreview,
                onChannelVolumeChange = onChannelVolumeChange,
                onPanChange = onPanChange,
                onEqPresetChange = onEqPresetChange
            )
        }

        // 커스텀 알림음 — 충전 완료 / 저전력 / 온도 경고
        item {
            CustomAlertSoundsCard(
                settings = settings,
                onPick = onPickAlertSound,
                onClear = onClearAlertSound
            )
        }

        // 커스텀 위젯 스킨 — 스타일 + 배경/글자색
        item {
            WidgetSkinCard(
                settings = settings,
                onStyleChange = onWidgetSkinStyleChange,
                onBgColorChange = onWidgetBgColorChange,
                onTextColorChange = onWidgetTextColorChange,
                onRingStyleChange = onRingStyleChange
            )
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

/** 음성 안내 문구와 토큰을 설정하는 인라인 편집기. */
@Composable
private fun VoiceTemplateEditor(
    startTemplate: String,
    targetTemplate: String,
    onSave: (String, String) -> Unit,
) {
    var startDraft by remember(startTemplate) { mutableStateOf(startTemplate) }
    var targetDraft by remember(targetTemplate) { mutableStateOf(targetTemplate) }
    Column(modifier = Modifier.padding(bottom = Spacing.md)) {
        Text(
            text = stringResource(R.string.voice_template_hint),
            style = MaterialTheme.typography.labelSmall,
            color = Slate400,
            modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.sm)
        )
        OutlinedTextField(
            value = startDraft,
            onValueChange = { startDraft = it },
            label = { Text(stringResource(R.string.voice_template_start)) },
            minLines = 2,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Spacing.sm))
        OutlinedTextField(
            value = targetDraft,
            onValueChange = { targetDraft = it },
            label = { Text(stringResource(R.string.voice_template_target)) },
            minLines = 2,
            textStyle = MaterialTheme.typography.bodySmall,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = stringResource(R.string.voice_template_save),
                color = ElectricCyan,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clickable { onSave(startDraft, targetDraft) }
                    .padding(top = Spacing.sm, bottom = Spacing.xs)
            )
        }
    }
}

/** 정적인 막대로 프리셋의 리듬감을 보여 줍니다. 재생 권한이나 오디오 분석은 필요 없습니다. */
@Composable
private fun PresetWaveform(samples: List<Float>, accent: Color) {
    Row(
        modifier = Modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        samples.forEach { sample ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((5f + 13f * sample.coerceIn(0f, 1f)).dp)
                    .background(accent.copy(alpha = 0.85f), AppShapes.pill)
            )
        }
    }
}

/** 프리셋 아이콘 이름 → Material 아이콘. 목록에 없으면 음표 아이콘으로 대체합니다. */
private fun presetIconFor(iconName: String): ImageVector = when (iconName) {
    "bolt" -> Icons.Default.Bolt
    "flash_on" -> Icons.Default.FlashOn
    "upgrade" -> Icons.Default.Upgrade
    "notifications" -> Icons.Default.Notifications
    "speed" -> Icons.Default.Speed
    else -> Icons.Default.Audiotrack
}

/** 앨범 커버 그라데이션 — 프리셋 인덱스로 순환하며 선택되지 않은 카드도 구분됩니다. */
private val presetCoverGradients: List<List<Color>> = listOf(
    listOf(Color(0xFF00E5FF), Color(0xFF0077A8)),
    listOf(Color(0xFFFFB300), Color(0xFFB26A00)),
    listOf(Color(0xFF3DDC84), Color(0xFF0B7A4B)),
    listOf(Color(0xFF9C6ADE), Color(0xFF4C2A9E)),
    listOf(Color(0xFFFF6B6B), Color(0xFFB02E2E)),
    listOf(Color(0xFF4FC3F7), Color(0xFF0277BD))
)

/**
 * 사운드 프리셋 "앨범" 카드 — 상단은 그라데이션 커버(아이콘+파형), 하단은 이름·분위기·길이.
 *
 * - 탭 = 선택(자동 미리듣기) [onSelect]
 * - 길게 누르기 = 설정을 바꾸지 않고 미리듣기만 [onPreviewToggle]
 * - 재생 중에는 테두리 글로우 + ▶ 아이콘이 ■ 로 바뀝니다
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetAlbumCard(
    preset: SoundPreset,
    coverIndex: Int,
    isSelected: Boolean,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onPreviewToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cover = presetCoverGradients[coverIndex % presetCoverGradients.size]
    val accent = if (isSelected) ElectricCyan else cover.first()
    val glow by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(Motion.MEDIUM_MILLIS),
        label = "presetGlow"
    )

    Surface(
        color = if (isSelected) ElectricCyan.copy(alpha = 0.10f) else Slate800,
        shape = AppShapes.md,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                ElectricCyan
            } else {
                // 재생 중 테두리가 시안으로 서서히 살아납니다 (선택 상태와 구분되는 글로우)
                ElectricCyan.copy(alpha = 0.15f + 0.65f * glow)
            }
        ),
        modifier = modifier.combinedClickable(
            onClick = onSelect,
            onLongClick = onPreviewToggle
        )
    ) {
        Column {
            // ── 앨범 커버 ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
                    .background(Brush.linearGradient(cover))
            ) {
                Icon(
                    imageVector = presetIconFor(preset.iconName),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 14.dp)
                        .size(34.dp)
                )

                // 재생 중인 파형: 커버 우측에 겹쳐 리듬감을 표시
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp)
                ) {
                    PresetWaveform(
                        samples = preset.waveform,
                        accent = if (isPlaying) Color.White else Color.White.copy(alpha = 0.55f)
                    )
                }

                // 선택 표시(체크) — 좌상단 핀 배지
                if (isSelected) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.45f),
                        shape = AppShapes.pill,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = stringResource(R.string.sound_selected_badge),
                                style = AppText.Badge,
                                color = ElectricCyan
                            )
                        }
                    }
                }

                // ▶ / ■ 토글 — 커버 우하단
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = if (isPlaying) 0.62f else 0.38f))
                        .clickable { onPreviewToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = stringResource(
                            if (isPlaying) R.string.btn_stop_sound else R.string.a11y_play
                        ),
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // ── 정보 ──
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(preset.nameRes),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isSelected) ElectricCyan else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    preset.moodRes?.let { moodRes ->
                        Text(
                            text = stringResource(moodRes),
                            style = AppText.Badge,
                            color = Slate400,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    Text(
                        text = stringResource(
                            R.string.sound_duration_seconds,
                            preset.previewDurationMs / 1_000f
                        ),
                        style = AppText.Badge,
                        color = if (isPlaying) ElectricCyan else Slate400
                    )
                }
            }
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
    settings: ChargeSettings,
    onDismiss: () -> Unit,
    onSelect: (AdFreePlan) -> Unit
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

                PremiumCatalog.AD_FREE_DURATION_OPTIONS.forEach { plan ->
                    val minutes = plan.minutes
                    val watched = if (settings.adFreePendingMinutes == minutes) {
                        settings.adFreeRewardProgress
                    } else {
                        0
                    }
                    Surface(
                        color = if (watched > 0) ElectricAmber.copy(alpha = 0.13f) else Slate800,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            if (watched > 0) ElectricAmber.copy(alpha = 0.75f) else Slate700
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onSelect(plan) }
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
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(
                                        R.string.adfree_ads_required,
                                        plan.rewardsRequired
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ElectricAmber,
                                    fontWeight = FontWeight.Bold
                                )
                                if (watched > 0) {
                                    Text(
                                        text = stringResource(
                                            R.string.adfree_progress,
                                            watched,
                                            plan.rewardsRequired
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Slate400
                                    )
                                }
                            }
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

/**
 * 사운드 탭에 노출되는 엣지 라이팅 설정 카드.
 *
 * 이전에는 오버플로 메뉴 → "소리 · 조용한 시간" 안에 숨어 있어 찾기 어려웠습니다.
 * 여기서 바로 켜고, 상시 표시 여부를 정하고, 미리보기까지 할 수 있습니다.
 */
@Composable
private fun EdgeGlowSettingsCard(
    settings: ChargeSettings,
    overlayGranted: Boolean,
    onToggle: (Boolean) -> Unit,
    onTogglePersistent: (Boolean) -> Unit,
    onPreview: () -> Unit,
    onCustomize: () -> Unit,
    onRequestOverlayPermission: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = AppShapes.md,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ElectricCyan.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
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
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.edge_glow_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.edge_glow_card_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }
                Switch(
                    checked = settings.edgeGlowEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Slate950,
                        checkedTrackColor = ElectricCyan,
                        uncheckedThumbColor = Slate400,
                        uncheckedTrackColor = Slate800
                    ),
                    modifier = Modifier.testTag("edge_glow_switch")
                )
            }

            AnimatedVisibility(visible = settings.edgeGlowEnabled) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.edge_glow_persistent_title),
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Text(
                                text = stringResource(R.string.edge_glow_persistent_sub),
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400
                            )
                        }
                        Switch(
                            checked = settings.edgeGlowPersistent,
                            onCheckedChange = onTogglePersistent,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Slate950,
                                checkedTrackColor = ElectricCyan,
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = Slate800
                            ),
                            modifier = Modifier.testTag("edge_glow_persistent_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (overlayGranted) {
                        OutlinedButton(
                            onClick = onPreview,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edge_glow_preview_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.edge_glow_preview),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.edge_glow_permission_needed),
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricAmber
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onRequestOverlayPermission,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricAmber),
                            border = BorderStroke(1.dp, ElectricAmber.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.overlay_btn_grant))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 투명도 · 색상(시작/중간/끝) · 회전 시간 설정
                    OutlinedButton(
                        onClick = onCustomize,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edge_glow_customize_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.edge_glow_customize),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 앱 안에서 짧은 충전음을 직접 녹음하는 다이얼로그.
 *
 * MP3 를 받아 오거나 자를 필요 없이 마이크로 10초까지 녹음하고,
 * [onRecorded] 로 파일을 넘겨 커스텀 충전음으로 바로 적용합니다.
 * 다이얼로그를 닫으면 마이크를 반드시 놓아 줍니다.
 */
@Composable
private fun VoiceRecorderDialog(
    onDismiss: () -> Unit,
    onRecorded: (java.io.File) -> Unit
) {
    val context = LocalContext.current
    val recorder = remember { VoiceRecorder(context.applicationContext) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var errorText by remember { mutableStateOf<String?>(null) }

    // 화면이 사라질 때 녹음 중이면 조용히 정리합니다. (마이크 점유 방지)
    DisposableEffect(Unit) {
        onDispose { recorder.cancel() }
    }

    // 녹음 중 1초마다 경과 시간을 올리고, 최대 길이에 닿으면 자동으로 멈춥니다.
    LaunchedEffect(isRecording) {
        if (!isRecording) return@LaunchedEffect
        elapsedSeconds = 0
        repeat(VoiceRecorder.MAX_RECORDING_SECONDS) {
            delay(1_000)
            if (!isRecording) return@LaunchedEffect
            elapsedSeconds += 1
        }
        if (isRecording) {
            val file = recorder.stop()
            isRecording = false
            if (file != null) {
                onRecorded(file)
            } else {
                errorText = context.getString(R.string.record_failed)
            }
        }
    }

    fun startRecording() {
        if (recorder.start()) {
            errorText = null
            elapsedSeconds = 0
            isRecording = true
        } else {
            errorText = context.getString(R.string.record_failed)
        }
    }

    fun stopAndApply() {
        val file = recorder.stop()
        isRecording = false
        if (file != null) {
            onRecorded(file)
        } else {
            errorText = context.getString(R.string.record_failed)
        }
    }

    fun cancelAndDismiss() {
        recorder.cancel()
        isRecording = false
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = ::cancelAndDismiss,
        title = { Text(stringResource(R.string.record_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.record_dialog_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
                if (isRecording) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(
                            R.string.record_seconds,
                            elapsedSeconds,
                            VoiceRecorder.MAX_RECORDING_SECONDS
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = ElectricPurple
                    )
                }
                errorText?.let { message ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricAmber
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (isRecording) stopAndApply() else startRecording() }) {
                Text(
                    text = if (isRecording) {
                        stringResource(R.string.btn_record_stop)
                    } else {
                        stringResource(R.string.btn_record_start)
                    },
                    color = if (isRecording) ElectricAmber else ElectricCyan
                )
            }
        },
        dismissButton = {
            TextButton(onClick = ::cancelAndDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        },
        containerColor = Slate900,
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

/**
 * 원형 로터리 노브 — 세로 드래그로 값을 조절합니다.
 *
 * 135°(왼쪽 아래)부터 270° 스윕으로 그려 턴테이블 다이얼 느낌을 냅니다.
 * 터치 영역은 84dp 로 넉넉하게 잡아 한 손으로 잡기 쉽게 했습니다.
 */
@Composable
private fun RotaryKnob(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    accent: Color,
    label: String,
    valueText: String,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val total = valueRange.endInclusive - valueRange.start
    val normalized = if (total <= 0f) 0f else ((value - valueRange.start) / total).coerceIn(0f, 1f)
    val angleDeg = 135f + 270f * normalized

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .pointerInput(valueRange) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // 세로 드래그: 위로 밀수록 큼 (170px = 전체 범위)
                        val delta = -dragAmount.y / 170f * total
                        val next = (value + delta).coerceIn(valueRange.start, valueRange.endInclusive)
                        if (next != value) onValueChange(next)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                val inset = stroke.width / 2
                val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                val topLeft = Offset(inset, inset)

                // 트랙
                drawArc(
                    color = Slate800,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
                // 값
                drawArc(
                    color = accent,
                    startAngle = 135f,
                    sweepAngle = 270f * normalized,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )

                // 다이얼 본체
                val radius = arcSize.minDimension / 2f
                drawCircle(color = Slate900, radius = radius, center = center)
                drawCircle(
                    color = Slate700,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // 포인터
                val rad = Math.toRadians(angleDeg.toDouble()).toFloat()
                val inner = radius * 0.42f
                val outer = radius * 0.86f
                drawLine(
                    color = accent,
                    start = center + Offset(cos(rad) * inner, sin(rad) * inner),
                    end = center + Offset(cos(rad) * outer, sin(rad) * outer),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Slate400,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = accent,
            maxLines = 1
        )
    }
}

/** 미리듣기 중에 춤추는 스펙트럼 바. 재생 중이 아니면 낮게 정지해 있습니다. */
@Composable
private fun MixerSpectrum(
    isActive: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "mixer_spectrum")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "mixer_spectrum_phase"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(30.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(22) { i ->
            val wobble = (sin(phase + i * 0.55f) + 1f) / 2f
            val h = if (isActive) 5f + 22f * wobble else 4f
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(h.dp)
                    .background(
                        accent.copy(alpha = if (isActive) 0.35f + 0.55f * wobble else 0.25f),
                        AppShapes.pill
                    )
            )
        }
    }
}

/** 사운드 믹서 — 채널 볼륨 · 스테레오 팬 · EQ 프리셋 카드 */
@Composable
private fun SoundLayerMixerCard(
    settings: ChargeSettings,
    isPlayingPreview: Boolean,
    onChannelVolumeChange: (Float) -> Unit,
    onPanChange: (Float) -> Unit,
    onEqPresetChange: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = AppShapes.md,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = ElectricCyan.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.sound_mixer_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.sound_mixer_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 원형 노브 — 채널 볼륨 · 스테레오 팬 (세로 드래그로 조절)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                RotaryKnob(
                    value = settings.channelVolume,
                    valueRange = 0f..1f,
                    accent = ElectricCyan,
                    label = stringResource(R.string.mixer_volume_label),
                    valueText = "${(settings.channelVolume * 100).toInt()}%",
                    onValueChange = onChannelVolumeChange,
                    modifier = Modifier.weight(1f)
                )
                RotaryKnob(
                    value = settings.pan,
                    valueRange = -1f..1f,
                    accent = ElectricAmber,
                    label = stringResource(R.string.mixer_pan_label),
                    valueText = when {
                        settings.pan < -0.05f ->
                            stringResource(R.string.mixer_pan_left, (settings.pan * -100).toInt())
                        settings.pan > 0.05f ->
                            stringResource(R.string.mixer_pan_right, (settings.pan * 100).toInt())
                        else -> stringResource(R.string.mixer_pan_center)
                    },
                    onValueChange = onPanChange,
                    modifier = Modifier.weight(1f)
                )
            }

            // 미리듣기 중에는 스펙트럼 바가 춤을 춥니다
            Spacer(modifier = Modifier.height(14.dp))
            MixerSpectrum(isActive = isPlayingPreview, accent = ElectricCyan)

            Spacer(modifier = Modifier.height(12.dp))

            // EQ 프리셋
            Text(
                text = stringResource(R.string.mixer_eq_label),
                style = MaterialTheme.typography.labelMedium,
                color = Slate400,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("flat", "bass_boost", "treble_boost", "vocal_boost").forEach { eq ->
                    val label = when (eq) {
                        "flat" -> stringResource(R.string.eq_flat)
                        "bass_boost" -> stringResource(R.string.eq_bass_boost)
                        "treble_boost" -> stringResource(R.string.eq_treble_boost)
                        "vocal_boost" -> stringResource(R.string.eq_vocal_boost)
                        else -> eq
                    }
                    val isSelected = settings.eqPreset == eq
                    FilterChip(
                        selected = isSelected,
                        onClick = { onEqPresetChange(eq) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Slate950 else Slate400
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (isSelected) ElectricCyan else Slate800,
                            labelColor = if (isSelected) Slate950 else Slate400
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) ElectricCyan else Slate700,
                            selectedBorderColor = ElectricCyan,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            Text(
                text = stringResource(R.string.mixer_eq_sub),
                style = MaterialTheme.typography.labelSmall,
                color = Slate600,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/** 커스텀 알림음 — 충전 완료 / 저전력 / 온도 경고별 사운드 지정 */
@Composable
private fun CustomAlertSoundsCard(
    settings: ChargeSettings,
    onPick: (String) -> Unit,
    onClear: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = AppShapes.md,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = ElectricCyan.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.NotificationAdd,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.custom_sounds_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.custom_sounds_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AlertSoundRow(
                label = stringResource(R.string.charge_complete_sound),
                path = settings.chargeCompleteSoundPath,
                onPick = { onPick("charge") },
                onClear = { onClear("charge") }
            )
            HorizontalDivider(color = Slate800, modifier = Modifier.padding(vertical = 4.dp))
            AlertSoundRow(
                label = stringResource(R.string.low_battery_sound),
                path = settings.lowBatterySoundPath,
                onPick = { onPick("low") },
                onClear = { onClear("low") }
            )
            HorizontalDivider(color = Slate800, modifier = Modifier.padding(vertical = 4.dp))
            AlertSoundRow(
                label = stringResource(R.string.temperature_sound),
                path = settings.temperatureSoundPath,
                onPick = { onPick("temp") },
                onClear = { onClear("temp") }
            )

            Text(
                text = stringResource(R.string.custom_sounds_hint),
                style = MaterialTheme.typography.labelSmall,
                color = Slate600,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/** 알림음 한 줄 — 라벨 + 선택/지우기 버튼 */
@Composable
private fun AlertSoundRow(
    label: String,
    path: String?,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            Text(
                text = if (path != null) stringResource(R.string.default_sound_set)
                else stringResource(R.string.default_sound),
                style = MaterialTheme.typography.labelSmall,
                color = if (path != null) ElectricCyan else Slate600
            )
        }
        TextButton(onClick = onPick) {
            Text(
                text = stringResource(R.string.pick_sound),
                style = MaterialTheme.typography.labelMedium,
                color = ElectricCyan
            )
        }
        if (path != null) {
            TextButton(onClick = onClear) {
                Text(
                    text = stringResource(R.string.clear_sound),
                    style = MaterialTheme.typography.labelMedium,
                    color = Slate400
                )
            }
        }
    }
}

/** 커스텀 위젯 스킨 — 스타일 칩 + 배경/글자색 빠른 선택 */
@Composable
private fun WidgetSkinCard(
    settings: ChargeSettings,
    onStyleChange: (String) -> Unit,
    onBgColorChange: (Long?) -> Unit,
    onTextColorChange: (Long?) -> Unit,
    onRingStyleChange: (Float?, String?, Boolean?) -> Unit
) {
    val bgSwatches = listOf<Long?>(
        null,
        0xFF101828L,
        0xFF0B3B36L,
        0xFF2B2140L,
        0xFFFDF6ECL
    )
    val textSwatches = listOf<Long?>(
        null,
        0xFFFFFFFFL,
        0xFF00E5FFL,
        0xFFFFC94DL,
        0xFF101828L
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = AppShapes.md,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = ElectricAmber.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Widgets,
                            contentDescription = null,
                            tint = ElectricAmber,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.widget_skin_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.widget_skin_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 스타일 칩
            Text(
                text = stringResource(R.string.widget_skin_style_label),
                style = MaterialTheme.typography.labelMedium,
                color = Slate400,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("default", "minimal", "glass", "neon").forEach { style ->
                    val label = when (style) {
                        "default" -> stringResource(R.string.widget_skin_default)
                        "minimal" -> stringResource(R.string.widget_skin_minimal)
                        "glass" -> stringResource(R.string.widget_skin_glass)
                        "neon" -> stringResource(R.string.widget_skin_neon)
                        else -> style
                    }
                    val isSelected = settings.widgetSkinStyle == style
                    FilterChip(
                        selected = isSelected,
                        onClick = { onStyleChange(style) },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Slate950 else Slate400
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = if (isSelected) ElectricAmber else Slate800,
                            labelColor = if (isSelected) Slate950 else Slate400
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) ElectricAmber else Slate700,
                            selectedBorderColor = ElectricAmber,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 배경색
            Text(
                text = stringResource(R.string.widget_bg_color),
                style = MaterialTheme.typography.labelMedium,
                color = Slate400,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            ColorSwatchRow(
                swatches = bgSwatches,
                selected = settings.widgetBackgroundColor,
                onSelect = onBgColorChange
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 글자색
            Text(
                text = stringResource(R.string.widget_text_color),
                style = MaterialTheme.typography.labelMedium,
                color = Slate400,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            ColorSwatchRow(
                swatches = textSwatches,
                selected = settings.widgetTextColor,
                onSelect = onTextColorChange
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 실시간 미리보기 — 홈 화면에 나가지 않고도 스킨을 확인할 수 있습니다
            Text(
                text = stringResource(R.string.widget_skin_preview_label),
                style = MaterialTheme.typography.labelMedium,
                color = Slate400,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            WidgetSkinPreview(settings = settings)

            Text(
                text = stringResource(R.string.widget_skin_hint),
                style = MaterialTheme.typography.labelSmall,
                color = Slate600,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))
            Text("배터리 링", style = MaterialTheme.typography.labelMedium, color = Slate400)
            Slider(
                value = settings.ringThickness,
                onValueChange = { onRingStyleChange(it, null, null) },
                valueRange = 0.55f..1.8f,
                colors = SliderDefaults.colors(thumbColor = ElectricAmber, activeTrackColor = ElectricAmber)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("round" to "둥근 끝", "square" to "각진 끝").forEach { (style, label) ->
                    FilterChip(
                        selected = settings.ringCapStyle == style,
                        onClick = { onRingStyleChange(null, style, null) },
                        label = { Text(label) }
                    )
                }
                FilterChip(
                    selected = settings.ringGlowEnabled,
                    onClick = { onRingStyleChange(null, null, !settings.ringGlowEnabled) },
                    label = { Text("글로우") }
                )
            }
        }
    }
}

/**
 * 위젯 스킨 실시간 미리보기 — 선택한 스타일/배경색/글자색을 축소판 위젯에 즉시 반영합니다.
 *
 * 실제 홈 화면 위젯(3종 레이아웃)을 흉내 낸 미니 버전이며,
 * 배경색은 RemoteViews 와 동일한 "둥근 사각형"으로 표현합니다.
 */
@Composable
private fun WidgetSkinPreview(settings: ChargeSettings) {
    val style = settings.widgetSkinStyle
    val bg = settings.widgetBackgroundColor?.let { Color(it) }
        ?: when (style) {
            "glass" -> Color(0xB31E293B)
            "neon" -> Color(0xFF030710)
            else -> Slate950
        }
    val fg = settings.widgetTextColor?.let { Color(it) } ?: Color.White
    val accent = when (style) {
        "neon" -> Color(0xFF39FF14)
        "glass" -> Color(0xFF7DD3FC)
        else -> if (settings.widgetTextColor != null) fg else ElectricCyan
    }
    val border = when (style) {
        "glass" -> Color(0x59FFFFFF)
        "neon" -> Color(0xFF39FF14)
        "minimal" -> Slate700
        else -> Color.Transparent
    }
    val pillBg = when (style) {
        "glass" -> Color(0x26FFFFFF)
        "neon" -> Color(0x1A39FF14)
        else -> Color(0x33000000)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .then(
                if (border != Color.Transparent) {
                    Modifier.border(1.5.dp, border, RoundedCornerShape(20.dp))
                } else {
                    Modifier
                }
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 링 게이지 + 중앙 배터리 %
        Box(modifier = Modifier.size(68.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(
                    width = (if (settings.ringThickness >= 1f) 7f else 5f).dp.toPx(),
                    cap = if (settings.ringCapStyle == "square") StrokeCap.Butt else StrokeCap.Round
                )
                val inset = stroke.width / 2
                val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                val topLeft = Offset(inset, inset)
                drawArc(
                    color = if (style == "neon") Color(0xFF14532D) else Slate700,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * 0.72f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
                if (settings.ringGlowEnabled) {
                    drawArc(
                        color = accent.copy(alpha = 0.28f),
                        startAngle = -90f,
                        sweepAngle = 360f * 0.72f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke.width * 2.2f, cap = stroke.cap)
                    )
                }
            }
            Text(
                text = "72%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = fg,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.widget_preview_status),
                style = MaterialTheme.typography.labelSmall,
                color = fg.copy(alpha = 0.75f)
            )
            Text(
                text = stringResource(R.string.widget_preview_charging),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
                color = fg
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    stringResource(R.string.widget_preview_power) to "18.4 W",
                    stringResource(R.string.widget_preview_temp) to "31°"
                ).forEach { (label, value) ->
                    Surface(
                        color = pillBg,
                        shape = AppShapes.pill
                    ) {
                        Text(
                            text = "$label $value",
                            style = AppText.Badge,
                            color = fg.copy(alpha = 0.9f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/** 색상 스와치 가로 줄 — null은 "기본" 선택 */@Composable
private fun ColorSwatchRow(
    swatches: List<Long?>,
    selected: Long?,
    onSelect: (Long?) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        swatches.forEach { color ->
            val isSelected = color == selected
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (color != null) Color(color)
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) ElectricCyan else Slate700,
                        shape = CircleShape
                    )
                    .clickable { onSelect(color) },
                contentAlignment = Alignment.Center
            ) {
                if (color == null) {
                    Text(
                        text = stringResource(R.string.widget_skin_default),
                        style = MaterialTheme.typography.labelSmall,
                        color = Slate400,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
