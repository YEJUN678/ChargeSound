package com.example.ui

import android.content.Context
import android.app.KeyguardManager
import android.os.Build
import com.example.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ad.AdManager
import com.example.battery.BatteryInfoHelper
import com.example.data.AnimationMode
import com.example.service.ChargingSessionTracker
import com.example.sound.SoundManager
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.AppText
import com.example.ui.theme.Motion
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate950
import com.example.ui.theme.Spacing
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ChargingAnimationActivity : ComponentActivity() {

    /** 테마 진입점에서 읽을 강조색 키 (onCreate에서 설정 저장소를 읽어 채웁니다) */
    private var accentKey: String = "cyan"

    companion object {
        private const val TAG = "ChargingAnimationActivity"
        const val EXTRA_BATTERY_LEVEL = "extra_battery_level"
        const val EXTRA_PLUG_NAME = "extra_plug_name"
        const val EXTRA_ANIM_MODE = "extra_anim_mode"
        const val EXTRA_VIDEO_PATH = "extra_video_path"
        const val EXTRA_DURATION_SEC = "extra_duration_sec"
        const val EXTRA_LOOP = "extra_loop"

        /** 구간 선택: 영상 재생 시작 지점 (ms). 0 이면 파일 처음부터 */
        const val EXTRA_VIDEO_START_MS = "extra_video_start_ms"
        /** 구간 선택: 영상 재생 끝 지점 (ms). 0 이면 파일 끝까지 */
        const val EXTRA_VIDEO_END_MS = "extra_video_end_ms"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOnLockScreen = runBlocking {
            SettingsRepository(applicationContext).settingsFlow.first().showOnLockScreen
        }
        // 강조색도 한 번만 읽습니다 (세션 동안 바뀔 일은 없습니다).
        accentKey = runBlocking {
            SettingsRepository(applicationContext).settingsFlow.first().accentKey
        }
        configureScreenWakeAndLock()
        enableEdgeToEdge()
        // 충전 애니메이션이 재생 중에는 전면류 광고를 표시하지 않습니다.
        AdManager.setCoreFeatureBusy(true)

        val initialLevel = intent.getIntExtra(EXTRA_BATTERY_LEVEL, 75)
        val plugName = intent.getStringExtra(EXTRA_PLUG_NAME) ?: getString(R.string.anim_plug_connected)
        val animModeStr = intent.getStringExtra(EXTRA_ANIM_MODE) ?: AnimationMode.BUILTIN_LIGHTNING.name
        val videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH)
        val durationSec = intent.getIntExtra(EXTRA_DURATION_SEC, 5)
        val isLoop = intent.getBooleanExtra(EXTRA_LOOP, true)
        // 구간 선택 (0 이면 전체 재생)
        val videoStartMs = intent.getLongExtra(EXTRA_VIDEO_START_MS, 0L)
        val videoEndMs = intent.getLongExtra(EXTRA_VIDEO_END_MS, 0L)

        val animMode = try {
            AnimationMode.valueOf(animModeStr)
        } catch (e: Exception) {
            AnimationMode.BUILTIN_LIGHTNING
        }

        setContent {
            // 충전 애니메이션도 사용자 강조색을 따라갑니다 (onCreate에서 이미 읽은 설정 재사용).
            MyApplicationTheme(
                darkTheme = true,
                accent = AccentPalette.fromKey(accentKey)
            ) {
                ChargingScreenContent(
                    initialLevel = initialLevel,
                    plugName = plugName,
                    animMode = animMode,
                    videoPath = videoPath,
                    durationSec = durationSec,
                    isLoop = isLoop,
                    videoStartMs = videoStartMs,
                    videoEndMs = videoEndMs,
                    onExitComplete = {
                        finishAndReturnToPreviousApp()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AdManager.setCoreFeatureBusy(false)
        SoundManager.fadeOutAndStop(300L)
    }

    /**
     * 충전 애니메이션을 끝내고 사용자가 보던 앱으로 돌아갑니다.
     *
     * 이 화면은 백그라운드(BAL)에서 시작되므로 별도 태스크로 뜹니다.
     * 그냥 finish()만 하면 홈 화면으로 떨어질 수 있어, 태스크를 뒤로 보내서
     * 직전에 보고 있던 앱이 다시 보이도록 합니다.
     */
    private fun finishAndReturnToPreviousApp() {
        try {
            // Activity.moveTaskToBack()를 쓰면 직전에 보고 있던 앱이 다시 앞으로 옵니다.
            if (isTaskRoot) {
                moveTaskToBack(true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "moveTaskToBack failed", e)
        }
        finish()
    }

    override fun onPause() {
        super.onPause()
        // 화면이 가려지면 광고 차단 상태를 해제합니다
        AdManager.setCoreFeatureBusy(false)
    }

    override fun onResume() {
        super.onResume()
        AdManager.setCoreFeatureBusy(true)
    }

    /** 잠금 화면 위 표시를 허용할지 (설정에서 읽어옴) */
    private var showOnLockScreen: Boolean = true

    /**
 * 잠금 화면에서도 충전 애니메이션이 보이도록 화면을 깨웁니다.
 *
 * 사용자가 [com.example.data.ChargeSettings.showOnLockScreen] 을 껐다면
 * 잠금 화면 위 표시와 화면 깨우기를 하지 않습니다.
 * 그래도 사운드·진동과 Foreground 알림은 그대로 동작합니다 (기능 손실 없음).
 */
@Suppress("DEPRECATION")
    private fun configureScreenWakeAndLock() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (!showOnLockScreen) {
            // 잠금 화면 위에는 표시하지 않되, 화면은 깨우지 않습니다.
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        // 보안 잠금(패턴/비밀번호)을 해제하지는 않습니다.
        // 잠금 위에 띄우기만 하고, 잠금 자체를 풀지 않습니다.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val keyguard = getSystemService(KeyguardManager::class.java)
            if (keyguard != null && !keyguard.isKeyguardLocked) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
            }
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        }
    }
}

@Composable
fun ChargingScreenContent(
    initialLevel: Int,
    plugName: String,
    animMode: AnimationMode,
    videoPath: String?,
    durationSec: Int,
    isLoop: Boolean,
    videoStartMs: Long,
    videoEndMs: Long,
    onExitComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentBatteryLevel by remember { mutableIntStateOf(initialLevel) }
    var remainingSeconds by remember { mutableIntStateOf(if (durationSec > 0) durationSec else 0) }

    // 충전 상태 폴링.
    //
    // 이 화면이 앱의 얼굴 이므로 가장 쓸모 있는 정보(충전 속도)를 여기에 둡니다.
    // 배터리를 이미 읽고 있으니 같은 경로로 세션 스냅샷만 추가로 얻습니다.
    //
    // 배터리는 1% 단위로 바뀌므로 10초 주기면 충분합니다. 더 짧게 폴링해도
    // 표시되는 숫자는 달라지지 않고 배터리 소모만 늘어납니다.
    var chargeRate by remember { mutableStateOf<Float?>(null) }
    var measuringRate by remember { mutableStateOf(true) }
    LaunchedEffect(animMode) {
        while (true) {
            val info = BatteryInfoHelper.getBatteryInfo(context)
            if (info.level > 0) currentBatteryLevel = info.level
            val snap = ChargingSessionTracker.snapshot(context, info)
            chargeRate = snap.percentPerHour
            // 시간당 상승률을 믿을 만하게 추정하려면 최소 1분은 지켜봐야 합니다.
            measuringRate = snap.elapsedMillis < 60_000L
            delay(10_000L)
        }
    }

    // 진입 스케일 — 화면이 아주 짧게 떠오르는 느낌으로 나타나게 합니다.
    // 오래 두면 시선을 뺏으므로 한 번만 스프링을 주고 끝냅니다.
    var entranceDone by remember { mutableStateOf(false) }
    val entranceScale by animateFloatAsState(
        targetValue = if (entranceDone) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Motion.SPRING_STIFFNESS
        ),
        label = "entrance_scale"
    )
    LaunchedEffect(Unit) { entranceDone = true }

    // ── 종료 전환 ──────────────────────────────────────────────────
    //
    // 예전에는 닫기를 누르는 즉시 `finish()` 였습니다. 화면이 통째로 증발합니다.
    //
    // 지금은 "닫기 요청"과 "실제 종료"를 분리했습니다.
    //  1) 어떤 경로로든 `requestClose()` 를 부르면 종료 전환이 시작되고
    //  2) 전환이 끝난 뒤에야 `onExitComplete()` 가 불려 실제로 종료됩니다.
    //
    // 덕분에 소리 페이드아웃도 화면이 사라지는 그 순간에 맞춰집니다.
    var exitRequested by remember { mutableStateOf(false) }
    val exit = remember { ExitTransition() }

    fun requestClose() {
        // 이미 전환 중이면 무시합니다 (연속 탭으로 두 번 finish 되는 것 방지)
        if (exitRequested) return
        exitRequested = true
        // 소리는 화면이 사라지는 그 0.2초에 맞춰 함께 사라집니다.
        // 전환이 끝난 뒤에 페이드하면 소리만 0.2초 더 남아서 "끊긴" 느낌이 납니다.
        // 시각(240ms)보다 약간 길게 잡아 화면이 닫힐 때쯤 이미 조용해 있게 합니다.
        SoundManager.fadeOutAndStop(260L)
    }

    LaunchedEffect(exitRequested) {
        if (!exitRequested) return@LaunchedEffect
        exit.run()
        onExitComplete()
    }

    // Auto dismiss timer
    LaunchedEffect(durationSec) {
        if (durationSec > 0) {
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
            }
            requestClose()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { requestClose() }
            )
            .testTag("charging_animation_screen")
    ) {
        // ── 종료 레이어 ────────────────────────────────────────
        //
        // 배경(Slate950)은 여기서 페이드시키지 않습니다. 창 배경을 색다시 나타내지
        // 않으려면 이 레이어는 불투명해야 하고, 그 위에 있는 것들만 사라집니다.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = exit.alpha.value
                    scaleX = exit.scale.value
                    scaleY = exit.scale.value
                }
        ) {

        // 진입 스케일은 애니메이션 레이어 에만 적용합니다.
        // HUD 는 글자라서 흔들리면 읽기 어렵고, 배경만 떠오르면 충분히 인상적입니다.
        Box(modifier = Modifier.fillMaxSize().scale(entranceScale)) {
        // Animation Layer
        val hasValidCustomVideo = animMode == AnimationMode.CUSTOM_VIDEO &&
                !videoPath.isNullOrBlank() &&
                File(videoPath).exists()

        if (hasValidCustomVideo) {
            VideoPlaybackView(
                videoPath = videoPath!!,
                isLoop = isLoop,
                startMs = videoStartMs,
                endMs = videoEndMs,
                onVideoFinished = {
                    if (durationSec == -1) {
                        requestClose()
                    }
                }
            )
        } else {
            // Built-in animated effects
            when (animMode) {
                AnimationMode.BUILTIN_REACTOR -> ReactorAnimation()
                AnimationMode.BUILTIN_PLASMA -> PlasmaAnimation()
                // 프리미엄 테마 (보상형 광고로 해제)
                AnimationMode.PREMIUM_NEBULA -> NebulaAnimation()
                AnimationMode.PREMIUM_MATRIX -> MatrixRainAnimation()
                AnimationMode.PREMIUM_ABYSS -> AbyssAnimation()
                AnimationMode.PREMIUM_PRISM -> PrismAnimation()
                AnimationMode.PREMIUM_FIREWORKS -> FireworksAnimation()
                AnimationMode.PREMIUM_BLACKHOLE -> BlackHoleAnimation()
                else -> LightningAnimation()
            }
        }

        }   // 진입 스케일 Box 끝 — 비네트는 아래에서 화면에 고정됩니다

        // 비네트 — HUD 글자가 배경 위에서도 읽히도록 위/아래를 어둡게 덮습니다.
        // 화면에 고정(스케일 대상 아님)이라 애니메이션이 커져도 가장자리만 어두워집니다.
        // Slate950 을 씁니다 — 순수 검정보다 앱 자신의 배경색이라 이질감이 없습니다.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Slate950.copy(alpha = 0.72f),
                            Color.Transparent,
                            Color.Transparent,
                            Slate950.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        // HUD 레이어: 상단 상태 / 중앙 배터리 / 하단 안내
        // (Box 안에서 align 로 위치를 정하는 대신 Column + weight 로 배분해
        //  배터리가 "상단/하단 HUD 사이의 남은 공간" 정중앙에 오게 합니다.)
        Column(
            modifier = Modifier.fillMaxSize()
        ) {

            // Top bar with close button & timer info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = HudPad, vertical = Spacing.lg)
                    .hudEntrance(delayMillis = HudStagger.TOP, fromTop = true),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChargingPlugChip(plugName = plugName)

                HudCloseButton(
                    onClick = { requestClose() },
                    modifier = Modifier.testTag("close_animation_button")
                )
            }

            // Center HUD: Battery percentage & neon ring
            //
            // 이전에는 `.align(Center).padding(bottom = 60.dp)` 로 화면 정중앙에서
            // 일부러 30dp 올려두었는데, 위쪽 상태 필(plug pill) 과 아래쪽 HUD 의
            // 높이가 서로 달라 실제 화면에서 배터리가 너무 위로 뜨고 아래가 비어 보였습니다.
            //
            // 이제 상단/하단 HUD 를 자연스러운 높이로 두고, 그 사이의 남은 공간을
            // Column 의 weight 로 배분해 배터리가 "지금 보이는 영역의 정중앙"에 오도록 했습니다.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .hudEntrance(delayMillis = HudStagger.CENTER),
                contentAlignment = Alignment.Center
            ) {
                BatteryCenterGlow(
                    batteryLevel = currentBatteryLevel,
                    percentPerHour = chargeRate,
                    measuring = measuringRate
                )
            }

            // Bottom HUD: Tap instruction & timer indicator
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = Spacing.xxl)
                    .hudEntrance(delayMillis = HudStagger.BOTTOM),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (durationSec > 0) {
                    ChargingCountdownChip(
                        text = stringResource(R.string.anim_auto_close, remainingSeconds),
                        seconds = remainingSeconds
                    )
                    Spacer(Modifier.height(Spacing.sm))
                } else if (durationSec == 0) {
                    ChargingLoopChip(text = stringResource(R.string.anim_loop_forever))
                    Spacer(Modifier.height(Spacing.sm))
                }

                Text(
                    text = stringResource(R.string.anim_exit_hint),
                    style = AppText.Axis,
                    color = Color.White.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center
                )
            }
        }
        }   // 종료 레이어 끝 — 이 안의 것들만 사라지고 배경은 남습니다
    }
}

@Composable
fun VideoPlaybackView(
    videoPath: String,
    isLoop: Boolean,
    startMs: Long,
    endMs: Long,
    onVideoFinished: () -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            VideoView(ctx).apply {
                try {
                    setVideoPath(videoPath)
                    setOnPreparedListener { mp ->
                        mp.isLooping = isLoop
                        mp.setVolume(0f, 0f)
                        // 구간이 지정돼 있으면 시작 지점으로 이동합니다.
                        if (startMs > 0L) {
                            mp.seekTo(startMs.toInt())
                        }
                        start()
                    }
                    setOnCompletionListener {
                        onVideoFinished()
                    }
                    setOnErrorListener { _, _, _ ->
                        // Suppress unhandled error dialog, continue safely
                        true
                    }
                } catch (e: Exception) {
                    // Fallback
                }
            }
        },
        update = { videoView ->
            try {
                    // 구간 선택: 끝 지점을 넘어가면 시작 지점으로 되돌립니다.
                    // 파일을 자르지 않으므로 반복 재생도 이 구간 안에서만 일어납니다.
                    if (endMs > startMs) {
                        val pos = videoView.currentPosition.toLong()
                        if (pos >= endMs) {
                            videoView.seekTo(startMs.toInt())
                        }
                    }
                if (!videoView.isPlaying) {
                    videoView.start()
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    )
}

@Composable
fun BatteryCenterGlow(
    batteryLevel: Int,
    percentPerHour: Float? = null,
    measuring: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // 100% 에서는 충전이 끝났다는 사실을 색으로 말합니다.
    // 번쩍이는 효과는 넣지 않았습니다 — 만충인 화면에서 빛 effects 는 시끄럽습니다.
    val isFull = batteryLevel >= 100
    val accent = if (isFull) ElectricGreen else ElectricAmber

    // 링이 1% 마다 순간이동하던 것을 부드럽게 잇습니다.
    // 스프링을 쓴 이유: 1% 는 3.6도라 tween 로는 눈에 띄지 않고, spring 은
    // "한 칸이 채워지는" 느낌을 줍니다.
    val animatedLevel by animateFloatAsState(
        targetValue = batteryLevel.coerceIn(0, 100).toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Motion.SPRING_STIFFNESS
        ),
        label = "battery_level"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(240.dp)
    ) {
        // Outer pulsing energy rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.width / 2.2f) * pulseScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = if (isFull) 0.3f else 0.25f),
                        accent.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // ── 그리는 순서가 중요합니다 ──
            // 배경 고리(회색)를 먼저 깔고, 그 위에 충전 고리를 얹어야 합니다.
            // 예전에는 반대로 그려서, 10% 불투명 흰색 고리가 충전 고리 위로 올라와
            // 색을 희석시켰습니다. 눈에 크게 드러나진 않지만 잘못된 순서입니다.
            drawArc(
                color = Color.White.copy(alpha = 0.10f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx())
            )

            // Dynamic progress arc
            val sweepAngle = animatedLevel / 100f * 360f
            // 0% 에서도 Round 캡이 점 하나를 남기는데, 아무것도 안 그린 게 맞습니다.
            if (sweepAngle > 0.1f) {
                drawArc(
                    brush = if (isFull) {
                        // 만충이면 단색 — 그라데이션은 "진행 중"이라는 암시
                        SolidColor(ElectricGreen)
                    } else {
                        Brush.sweepGradient(
                            colors = listOf(
                                ElectricCyan,
                                ElectricGreen,
                                ElectricAmber,
                                ElectricCyan
                            )
                        )
                    },
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Percentage & Charging Bolt
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.height(Spacing.xxs))
            Row(verticalAlignment = Alignment.Bottom) {
                // 숫자가 1 올라갈 때 아래에서 새 숫자가 밀려오며 바뀝니다.
                // 정지해 있던 숫자가 순간적으로 바뀌는 것보다 "계속 증가하고 있다"는
                // 감각이 훨씬 선명하게 전달됩니다.
                AnimatedContent(
                    targetState = batteryLevel,
                    transitionSpec = {
                        (slideInVertically(tween(Motion.MEDIUM_MILLIS)) { height -> height } +
                            fadeIn(tween(Motion.MEDIUM_MILLIS)))
                            .togetherWith(
                                slideOutVertically(tween(Motion.FAST_MILLIS)) { height -> -height } +
                                    fadeOut(tween(Motion.FAST_MILLIS))
                            )
                    },
                    label = "battery_digits"
                ) { level ->
                    Text(
                        text = "$level",
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White
                    )
                }
                Text(
                    text = "%",
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isFull) ElectricGreen else ElectricCyan,
                    modifier = Modifier.padding(bottom = Spacing.sm, start = Spacing.xxs)
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            // 예전에는 여기 "FAST CHARGING" 이 고정 표시했는데 충전 속도와 상관없이
            // 5W 충전기에서도 같은 문구가 나왔습니다. 이제 실제로 잰 값만 보여줍니다.
            ChargingStatusLine(
                percentPerHour = percentPerHour,
                measuring = measuring,
                isFull = isFull
            )
        }
    }
}

@Composable
fun LightningAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "lightning")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val flicker by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)

        rotate(rotation, center) {
            for (i in 0 until 8) {
                val angle = (i * 45f) * (PI / 180f).toFloat()
                val radius1 = size.width * 0.2f
                val radius2 = size.width * 0.45f
                val start = Offset(center.x + cos(angle) * radius1, center.y + sin(angle) * radius1)
                val midAngle = angle + 0.1f
                val mid = Offset(center.x + cos(midAngle) * (radius1 + radius2) / 2, center.y + sin(midAngle) * (radius1 + radius2) / 2)
                val end = Offset(center.x + cos(angle) * radius2, center.y + sin(angle) * radius2)

                val path = Path().apply {
                    moveTo(start.x, start.y)
                    lineTo(mid.x, mid.y)
                    lineTo(end.x, end.y)
                }

                drawPath(
                    path = path,
                    color = ElectricCyan.copy(alpha = 0.4f * flicker),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
fun ReactorAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor")
    val rotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "r1"
    )
    val rotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing)
        ),
        label = "r2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)

        // Outer reactor ring
        rotate(rotation1, center) {
            for (i in 0 until 12) {
                val startAngle = i * 30f
                drawArc(
                    brush = Brush.linearGradient(listOf(ElectricCyan, ElectricPurple)),
                    startAngle = startAngle,
                    sweepAngle = 18f,
                    useCenter = false,
                    topLeft = Offset(center.x - 170.dp.toPx(), center.y - 170.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(340.dp.toPx(), 340.dp.toPx()),
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Inner reactor counter-rotating ring
        rotate(rotation2, center) {
            for (i in 0 until 8) {
                val startAngle = i * 45f
                drawArc(
                    color = ElectricAmber.copy(alpha = 0.8f),
                    startAngle = startAngle,
                    sweepAngle = 25f,
                    useCenter = false,
                    topLeft = Offset(center.x - 130.dp.toPx(), center.y - 130.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(260.dp.toPx(), 260.dp.toPx()),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
fun PlasmaAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "plasma")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        ),
        label = "pulse_phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)

        for (i in 0 until 6) {
            val angle = (i * 60f) * (PI / 180f).toFloat() + pulse
            val dist = 120.dp.toPx() + sin(pulse + i) * 25.dp.toPx()
            val ballCenter = Offset(center.x + cos(angle) * dist, center.y + sin(angle) * dist)
            val ballRadius = 35.dp.toPx() + cos(pulse * 2 + i) * 10.dp.toPx()

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ElectricGreen.copy(alpha = 0.6f),
                        ElectricCyan.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    center = ballCenter,
                    radius = ballRadius
                ),
                radius = ballRadius,
                center = ballCenter
            )
        }
    }
}
