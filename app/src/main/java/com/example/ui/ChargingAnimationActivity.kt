package com.example.ui

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ad.AdManager
import com.example.battery.BatteryInfoHelper
import com.example.data.AnimationMode
import com.example.sound.SoundManager
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.Slate950
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ChargingAnimationActivity : ComponentActivity() {

    companion object {
        const val EXTRA_BATTERY_LEVEL = "extra_battery_level"
        const val EXTRA_PLUG_NAME = "extra_plug_name"
        const val EXTRA_ANIM_MODE = "extra_anim_mode"
        const val EXTRA_VIDEO_PATH = "extra_video_path"
        const val EXTRA_DURATION_SEC = "extra_duration_sec"
        const val EXTRA_LOOP = "extra_loop"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureScreenWakeAndLock()
        enableEdgeToEdge()
        // 충전 애니메이션이 재생 중에는 전면류 광고를 표시하지 않습니다.
        AdManager.setCoreFeatureBusy(true)

        val initialLevel = intent.getIntExtra(EXTRA_BATTERY_LEVEL, 75)
        val plugName = intent.getStringExtra(EXTRA_PLUG_NAME) ?: "충전기 연결됨"
        val animModeStr = intent.getStringExtra(EXTRA_ANIM_MODE) ?: AnimationMode.BUILTIN_LIGHTNING.name
        val videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH)
        val durationSec = intent.getIntExtra(EXTRA_DURATION_SEC, 5)
        val isLoop = intent.getBooleanExtra(EXTRA_LOOP, true)

        val animMode = try {
            AnimationMode.valueOf(animModeStr)
        } catch (e: Exception) {
            AnimationMode.BUILTIN_LIGHTNING
        }

        setContent {
            MyApplicationTheme {
                ChargingScreenContent(
                    initialLevel = initialLevel,
                    plugName = plugName,
                    animMode = animMode,
                    videoPath = videoPath,
                    durationSec = durationSec,
                    isLoop = isLoop,
                    onDismiss = {
                        SoundManager.fadeOutAndStop(500L)
                        finish()
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

    override fun onPause() {
        super.onPause()
        // 화면이 가려지면 광고 차단 상태를 해제합니다
        AdManager.setCoreFeatureBusy(false)
    }

    override fun onResume() {
        super.onResume()
        AdManager.setCoreFeatureBusy(true)
    }

    @Suppress("DEPRECATION")
    private fun configureScreenWakeAndLock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
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
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentBatteryLevel by remember { mutableIntStateOf(initialLevel) }
    var remainingSeconds by remember { mutableIntStateOf(if (durationSec > 0) durationSec else 0) }

    // Read real battery info
    LaunchedEffect(Unit) {
        val info = BatteryInfoHelper.getBatteryInfo(context)
        if (info.level > 0) {
            currentBatteryLevel = info.level
        }
    }

    // Auto dismiss timer
    LaunchedEffect(durationSec) {
        if (durationSec > 0) {
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds--
            }
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate950)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .testTag("charging_animation_screen")
    ) {
        // Animation Layer
        val hasValidCustomVideo = animMode == AnimationMode.CUSTOM_VIDEO &&
                !videoPath.isNullOrBlank() &&
                File(videoPath).exists()

        if (hasValidCustomVideo) {
            VideoPlaybackView(
                videoPath = videoPath!!,
                isLoop = isLoop,
                onVideoFinished = {
                    if (durationSec == -1) {
                        onDismiss()
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

        // Dark gradient vignette overlay to ensure text contrast
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Top bar with close button & timer info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = ElectricAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = plugName,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .size(40.dp)
                    .testTag("close_animation_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = Color.White
                )
            }
        }

        // Center HUD: Battery percentage & neon ring
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BatteryCenterGlow(batteryLevel = currentBatteryLevel)
        }

        // Bottom HUD: Tap instruction & timer indicator
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (durationSec > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "⏱ ${remainingSeconds}초 후 자동 종료 (사운드 페이드아웃)",
                        color = ElectricCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            } else if (durationSec == 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "화면 터치 시까지 연속 재생",
                        color = ElectricGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Text(
                text = "화면 아무 곳이나 터치하면 즉시 닫힙니다",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun VideoPlaybackView(
    videoPath: String,
    isLoop: Boolean,
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
fun BatteryCenterGlow(batteryLevel: Int) {
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
                        ElectricCyan.copy(alpha = 0.25f),
                        ElectricCyan.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // Dynamic progress arc
            val sweepAngle = (batteryLevel / 100f) * 360f
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(ElectricCyan, ElectricGreen, ElectricAmber, ElectricCyan)
                ),
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )

            // Background arc
            drawArc(
                color = Color.White.copy(alpha = 0.1f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 8.dp.toPx())
            )
        }

        // Percentage & Charging Bolt
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.FlashOn,
                contentDescription = null,
                tint = ElectricAmber,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$batteryLevel",
                    fontSize = 58.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "%",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = ElectricCyan,
                    modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                )
            }
            Text(
                text = "FAST CHARGING",
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricGreen
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
