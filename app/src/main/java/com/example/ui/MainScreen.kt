package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ad.AdManager
import com.example.battery.BatteryInfo
import com.example.data.AnimationMode
import com.example.data.ChargeSettings
import com.example.data.PremiumCatalog
import com.example.data.SoundMode
import com.example.sound.SoundManager
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ElectricPurple
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
    val isPlayingPreview by viewModel.isPlayingPreview.collectAsState()
    val activePlayingId by viewModel.activePlayingId.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val overlayGranted by viewModel.overlayGranted.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showAdFreeDialog by remember { mutableStateOf(false) }

    // 보상형 광고를 표시하려면 Activity가 필요합니다 (Compose Context로는 부족).
    val activity = LocalContext.current as? Activity

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = ElectricCyan.copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
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
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ChargeSound",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 19.sp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = if (settings.isServiceEnabled) ElectricGreen.copy(alpha = 0.2f) else Slate800,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (settings.isServiceEnabled) "ON" else "OFF",
                                        color = if (settings.isServiceEnabled) ElectricGreen else Slate400,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (settings.isServiceEnabled) "충전 감지 모니터링 실행 중" else "모니터링 대기 모드",
                                fontSize = 11.sp,
                                color = if (settings.isServiceEnabled) ElectricGreen else Slate400
                            )
                        }
                    }
                },
                actions = {
                    Switch(
                        checked = settings.isServiceEnabled,
                        onCheckedChange = { viewModel.toggleServiceEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Slate950,
                            checkedTrackColor = ElectricCyan,
                            uncheckedThumbColor = Slate400,
                            uncheckedTrackColor = Slate800
                        ),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("service_toggle_switch")
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate950
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Slate950
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
                onTestClick = { viewModel.simulateCharging(context) }
            )

            // Tabs: 0: Sound (MP3), 1: Animation (MP4/AVI), 2: Battery Info
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Slate900,
                contentColor = ElectricCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = ElectricCyan,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "효과음 (MP3)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    selectedContentColor = ElectricCyan,
                    unselectedContentColor = Slate400
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "애니메이션", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    selectedContentColor = ElectricCyan,
                    unselectedContentColor = Slate400
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "배터리 정보", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    selectedContentColor = ElectricCyan,
                    unselectedContentColor = Slate400
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
                        // (AdMob 정책: 광고 실패가 앱 사용을 막아서는 안 됨)
                        onNotAvailable = { viewModel.startAdFreePeriod(minutes) }
                    )
                }
            }
        )
    }
}

@Composable
fun BatteryLiveHeaderCard(
    batteryInfo: BatteryInfo,
    isServiceEnabled: Boolean,
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, if (batteryInfo.isCharging) ElectricCyan.copy(alpha = 0.5f) else Slate800)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.charge_hero_banner_1791458034500),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(RoundedCornerShape(22.dp)),
                alpha = 0.22f
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Slate900.copy(alpha = 0.95f),
                                Slate900.copy(alpha = 0.7f),
                                Slate900.copy(alpha = 0.95f)
                            )
                        )
                    )
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
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${batteryInfo.level}%",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (batteryInfo.isCharging) "충전 중 ⚡" else "배터리",
                                    fontSize = 13.sp,
                                    color = if (batteryInfo.isCharging) ElectricGreen else Slate400,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Text(
                                text = "${batteryInfo.plugType.displayName} • ${batteryInfo.temperatureC}°C",
                                fontSize = 12.sp,
                                color = Slate400
                            )
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
                            text = "충전 테스트",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
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
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Slate800)
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
                                text = "충전 효과음 활성화",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = "케이블 연결 시 설정한 사운드를 재생합니다",
                                fontSize = 12.sp,
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
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Slate800)
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
                                text = "사운드 볼륨 조절",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
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
                                fontSize = 16.sp
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
                                text = if (isPlayingPreview) "소리 멈추기" else "볼륨 미리듣기",
                                color = ElectricCyan,
                                fontSize = 12.sp,
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
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Slate800)
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
                            text = "내 MP3 / 오디오 직접 선택",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "원하는 오디오를 고르면 테스트 광고 후 즉시 적용됩니다",
                        fontSize = 12.sp,
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
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "사용자 커스텀 효과음 적용 중",
                                            fontSize = 11.sp,
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
                                            contentDescription = "재생",
                                            tint = ElectricCyan
                                        )
                                    }
                                    IconButton(
                                        onClick = onClearAudio,
                                        modifier = Modifier.testTag("custom_audio_delete_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "삭제",
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
                            text = if (settings.customSoundName != null) "다른 MP3 파일로 변경하기" else "기기에서 MP3 파일 선택하기",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Built-in Presets Section (Triggers Test Ad on Selection)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "기본 제공 프리셋 사운드",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "사운드를 터치하면 테스트 광고 후 설정됩니다",
                        fontSize = 12.sp,
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
                                            text = preset.name,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = if (isSelected) ElectricCyan else Color.White
                                        )
                                        Text(
                                            text = preset.description,
                                            fontSize = 11.sp,
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
                                        contentDescription = "미리듣기",
                                        tint = if (isSelected) ElectricCyan else Slate400
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 프리미엄 사운드 (보상형 광고 ${PremiumCatalog.ADS_REQUIRED_PER_UNLOCK}회 = 잠금 해제) ──
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = ElectricPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "프리미엄 사운드 팩",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "광고 ${PremiumCatalog.ADS_REQUIRED_PER_UNLOCK}회 시청 시 잠금 해제. " +
                            "광고를 거부해도 위의 기본 사운드는 그대로 사용하실 수 있습니다.",
                        fontSize = 12.sp,
                        color = Slate400,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PremiumCatalog.sounds.forEach { premium ->
                        val accent = Color(premium.accentArgb)
                        val unlocked = settings.isUnlocked(premium.id)
                        val watched = settings.rewardProgress[premium.id] ?: 0

                        if (unlocked) {
                            PremiumUnlockedRow(
                                name = premium.name,
                                description = premium.description,
                                accent = accent,
                                isSelected = settings.soundMode == SoundMode.PRESET &&
                                        settings.presetSoundId == premium.id,
                                onSelect = { onSelectPreset(premium.id) }
                            )
                        } else {
                            PremiumLockedRow(
                                name = premium.name,
                                description = premium.description,
                                accent = accent,
                                adsWatched = watched,
                                adsRequired = PremiumCatalog.ADS_REQUIRED_PER_UNLOCK,
                                onWatchAd = { onWatchAdToUnlock(premium.id) }
                            )
                        }
                    }
                }
            }
        }

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
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Slate800)
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
                                text = "충전 연결 시 햅틱 진동",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = "사운드와 함께 진동 피드백을 전달합니다",
                                fontSize = 12.sp,
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
    onToggleAnimation: (Boolean) -> Unit,
    onSelectAnimationMode: (AnimationMode) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onToggleLoop: (Boolean) -> Unit,
    onPreviewFullScreen: () -> Unit,
    onWatchAdToUnlock: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Master Animation Toggle Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Slate800)
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
                                text = "충전 애니메이션 화면 표시",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.White
                            )
                            Text(
                                text = "충전기 연결 시 전체화면 애니메이션 재생",
                                fontSize = 12.sp,
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
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Slate800)
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
                            text = "내 동영상 (MP4 / AVI 등) 직접 선택",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "스마트폰에 있는 애니메이션 또는 비디오를 충전 화면으로 사용하세요",
                        fontSize = 12.sp,
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
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "사용자 비디오 영상 활성화됨",
                                            fontSize = 11.sp,
                                            color = ElectricAmber
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onClearVideo,
                                    modifier = Modifier.testTag("custom_video_delete_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "삭제",
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
                            text = if (settings.customVideoName != null) "다른 영상 파일로 변경하기" else "기기에서 비디오 파일 선택 (MP4, AVI)",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // NEW: Playback Duration SLIDER Card (requested by user)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Slate800)
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
                                text = "동영상 실행 시간 (슬라이더 조절)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
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
                                text = settings.durationLabel,
                                color = ElectricCyan,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "슬라이더를 좌우로 드래그하여 재생 시간(1초~30초)을 정밀하게 조절하세요",
                        fontSize = 12.sp,
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
                            label = { Text("슬라이더 시간", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = Slate950
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = settings.videoDurationSeconds == -1,
                            onClick = { onSelectDuration(-1) },
                            label = { Text("영상 전체 재생", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan,
                                selectedLabelColor = Slate950
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = settings.videoDurationSeconds == 0,
                            onClick = { onSelectDuration(0) },
                            label = { Text("터치 시까지", fontSize = 12.sp) },
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
                        Text(text = "1초", color = Slate600, fontSize = 11.sp)
                        Text(text = "10초", color = Slate600, fontSize = 11.sp)
                        Text(text = "20초", color = Slate600, fontSize = 11.sp)
                        Text(text = "30초", color = Slate600, fontSize = 11.sp)
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
                                text = "음악 동기화: 종료 1.5초 전부터 사운드가 점점 작아지며 부드럽게 페이드아웃 됩니다.",
                                color = ElectricGreen,
                                fontSize = 11.sp,
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
                                text = "짧은 영상 반복 재생 (Loop)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                            Text(
                                text = "설정 시간 동안 영상을 끊김 없이 반복합니다",
                                fontSize = 11.sp,
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
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "기본 내장 애니메이션 테마",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "커스텀 영상이 없거나 네온 그래픽 효과를 원할 때 사용됩니다",
                        fontSize = 12.sp,
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
                                            text = mode.displayName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = if (isSelected) ElectricAmber else Color.White
                                        )
                                        Text(
                                            text = mode.description,
                                            fontSize = 11.sp,
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

        // ── 프리미엄 애니메이션 테마 (보상형 광고 ${PremiumCatalog.ADS_REQUIRED_PER_UNLOCK}회 = 잠금 해제) ──
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ElectricPurple.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = ElectricPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "프리미엄 애니메이션 테마",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "광고 ${PremiumCatalog.ADS_REQUIRED_PER_UNLOCK}회 시청 시 잠금 해제. " +
                            "거부해도 기본 테마 3종은 계속 사용할 수 있습니다.",
                        fontSize = 12.sp,
                        color = Slate400,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PremiumCatalog.animations.forEach { premium ->
                        val accent = Color(premium.accentArgb)
                        val unlocked = settings.isUnlocked(premium.id)
                        val watched = settings.rewardProgress[premium.id] ?: 0

                        if (unlocked) {
                            PremiumUnlockedRow(
                                name = premium.name,
                                description = premium.description,
                                accent = accent,
                                isSelected = settings.animationMode == premium.mode,
                                onSelect = { onSelectAnimationMode(premium.mode) }
                            )
                        } else {
                            PremiumLockedRow(
                                name = premium.name,
                                description = premium.description,
                                accent = accent,
                                adsWatched = watched,
                                adsRequired = PremiumCatalog.ADS_REQUIRED_PER_UNLOCK,
                                onWatchAd = { onWatchAdToUnlock(premium.id) }
                            )
                        }
                    }
                }
            }
        }

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
                    text = "애니메이션 전체화면 미리보기",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
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
                    text = "광고 없이 사용하기",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "광고 1회 시청으로 선택한 시간 동안 배너가 숨겨집니다.",
                    color = Slate400,
                    fontSize = 12.sp,
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
                                text = formatAdFreeDuration(minutes),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
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
                    text = "취소",
                    color = Slate400,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clickable { onDismiss() }
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private fun formatAdFreeDuration(minutes: Int): String = when {
    minutes < 60 -> "${minutes}분"
    minutes < 60 * 24 -> "${minutes / 60}시간"
    else -> "${minutes / (60 * 24)}일"
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
 * AdMob 정책상 광고로 기능 사용을 방해하면 안 됩니다.
 */
@Composable
fun BatteryInfoTab(batteryInfo: BatteryInfo) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 충전 상태 헤더
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Slate900),
                shape = RoundedCornerShape(18.dp),
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
                                text = if (batteryInfo.isCharging) "충전 중 ⚡" else "방전 중",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = if (batteryInfo.isCharging) ElectricGreen else Color.White
                            )
                            Text(
                                text = batteryInfo.statusText,
                                fontSize = 13.sp,
                                color = Slate400
                            )
                        }
                        Text(
                            text = "${batteryInfo.level}%",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 30.sp,
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
                            text = batteryInfo.plugType.displayName,
                            fontSize = 12.sp,
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
                    label = "배터리 온도",
                    value = "%.1f°C".format(batteryInfo.temperatureC)
                )
                BatteryInfoRow(
                    icon = Icons.Default.ElectricBolt,
                    label = "현재 전압",
                    value = "%.2f V".format(batteryInfo.voltageV)
                )
                BatteryInfoRow(
                    icon = Icons.Default.Check,
                    label = "배터리 건강도",
                    value = batteryInfo.healthText
                )
                BatteryInfoRow(
                    icon = Icons.Default.Power,
                    label = "배터리 기술",
                    value = batteryInfo.technology
                )
                BatteryInfoRow(
                    icon = Icons.Default.Audiotrack,
                    label = "충전 상태",
                    value = batteryInfo.statusText
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
                            text = "배터리 온도가 높습니다. 화기와 직사광선을 피해주세요.",
                            fontSize = 12.sp,
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
    value: String
) {
    Surface(
        color = Slate900,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Slate800),
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
                    fontSize = 14.sp,
                    color = Slate400
                )
            }
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
