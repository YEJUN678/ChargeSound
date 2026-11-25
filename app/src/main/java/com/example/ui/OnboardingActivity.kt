package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.MainActivity
import com.example.data.SettingsRepository
import com.example.sound.SoundManager
import com.example.ui.theme.AppShapes
import com.example.ui.theme.AppText
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * 첫 실행 온보딩.
 *
 * 이 앱의 핵심 기능(충전 시 전체화면 애니메이션)은 **SYSTEM_ALERT_WINDOW** 권한이
 * 필요합니다. 안드로이드는 이 권한을 사용자가 직접 설정에서 켜야 합니다.
 *
 * 흐름:
 * 1) 권한이 왜 필요한지 한 장으로 설명
 * 2) "권한 허용" 버튼 → 설정 화면으로 이동
 * 3) 사용자가 켜고 돌아오면 자동으로 감지 → 다음 장
 * 4) 충전 애니메이션 자동 실행 ON/OFF 토글
 * 4) "시작하기" → 메인 진입, 플래그 저장
 */
class OnboardingActivity : ComponentActivity() {

    private val overlayLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // 설정에서 돌아오면 권한 상태를 다시 확인합니다.
        refreshOverlayStatus()
    }

    private var overlayGranted by mutableStateOf(false)
    private var autoStartEnabled by mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // 이미 온보딩 완료면 메인으로 바로 보냅니다.
            if (SettingsRepository(this@OnboardingActivity).onboardingDone()) {
                startMain()
                finish()
                return@launch
            }

            // 현재 권한 상태 확인
            overlayGranted = Settings.canDrawOverlays(this@OnboardingActivity)
            autoStartEnabled = SettingsRepository(this@OnboardingActivity).autoStartAnimation()
        }

        setContent {
            MaterialTheme {
                OnboardingScreen(
                    overlayGranted = overlayGranted,
                    onRequestOverlay = { requestOverlayPermission() },
                    onFinish = { finishOnboarding() },
                    autoStartEnabled = autoStartEnabled,
                    onAutoStartChange = { autoStartEnabled = it }
                )
            }
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName"))
            overlayLauncher.launch(intent)
        }
    }

    private fun refreshOverlayStatus() {
        overlayGranted = Settings.canDrawOverlays(this)
    }

    private fun finishOnboarding() {
        lifecycleScope.launch {
            val repo = SettingsRepository(this@OnboardingActivity)
            repo.setOnboardingDone(true)
            repo.setAutoStartAnimation(autoStartEnabled)
            startMain()
            finish()
        }
    }

    private fun startMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}

/**
 * 3단계 온보딩: ① 권한 → ② 사운드 테스트(즉시 재생) → ③ 자동 실행 + 위젯 안내.
 *
 * 페이지 상태는 컴포저블이 들고 있어요. 권한은 설정 다녀와야 완료되는라
 * [LaunchedEffect] 로 허용되는 순간 자동으로 다음 장으로 넘어갑니다.
 */
@Composable
fun OnboardingScreen(
    overlayGranted: Boolean,
    onRequestOverlay: () -> Unit,
    onFinish: () -> Unit,
    autoStartEnabled: Boolean,
    onAutoStartChange: (Boolean) -> Unit
) {
    var page by remember { mutableStateOf(if (overlayGranted) 1 else 0) }

    // 권한이 허용되면 0번 장에 머무르지 않고 다음 장으로 넘어갑니다.
    LaunchedEffect(overlayGranted) {
        if (overlayGranted && page == 0) page = 1
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 페이지 전환은 즉시 — 온보딩은 "한 번만 보는" 화면이라 스프링이 방해가 됩니다.
        when (page) {
            0 -> PermissionPage(
                granted = overlayGranted,
                onRequest = onRequestOverlay,
                onNext = { page = 1 }
            )
            1 -> SoundTestPage(
                onNext = { page = 2 },
                onSkip = onFinish
            )
            else -> FinishPage(
                enabled = autoStartEnabled,
                onChange = onAutoStartChange,
                onFinish = onFinish
            )
        }

        // 우상단 건너뛰기 — 권한 없이도 앱을 쓸 수 있습니다.
        if (page < 2) {
            Text(
                text = stringResource(R.string.onboard_skip),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.55f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onFinish() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        // 하단 점 인디케이터
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 40.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PageDot(selected = page == 0)
                PageDot(selected = page == 1)
                PageDot(selected = page == 2)
            }
        }
    }
}

@Composable
private fun PageDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(if (selected) 10.dp else 7.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.4f))
    )
}

@Composable
private fun PermissionPage(
    granted: Boolean,
    onRequest: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 아이콘
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.size(120.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.08f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FlashOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboard_permission_title),
            style = AppText.Brand,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboard_permission_body),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(32.dp))

        if (!granted) {
            Button(
                onClick = onRequest,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboard_permission_button),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        } else {
            // 권한이 이미 있으면(예: 재진입) 체크 표시 + 다음
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.onboard_permission_granted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.onboard_next),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

/**
 * 2단계 — 사운드 테스트. 충전 알림음을 바로 한 번 재생해 줍니다.
 *
 * 온보딩에서 "설명"만 읽히면 어떤 소리가 날지 모르는 채로 끝나기 때문에,
 * 여기서 실제로 들어보고 마음에 안 들면 설정에서 바꿀 수 있다는 걸 알려 줍니다.
 */
@Composable
private fun SoundTestPage(
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }

    // 화면을 떠나면 소리를 멈춥니다.
    DisposableEffect(Unit) {
        onDispose { SoundManager.fadeOutAndStop(0L) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.size(120.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.08f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playing) Icons.Default.GraphicEq else Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboard_sound_title),
            style = AppText.Brand,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboard_sound_body),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = {
                if (playing) {
                    SoundManager.fadeOutAndStop(200L)
                    playing = false
                } else {
                    val file = SoundManager.getPresetFile(context, "cyber_chime")
                    SoundManager.playSound(
                        context = context,
                        filePath = file.absolutePath,
                        volume = 0.9f
                    )
                    playing = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = if (playing) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(
                    if (playing) R.string.onboard_sound_stop else R.string.onboard_sound_play
                ),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = stringResource(R.string.onboard_next),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

/** 3단계 — 자동 실행 토글 + 위젯 안내, 그리고 시작하기. */
@Composable
private fun FinishPage(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.size(120.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.08f)
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryChargingFull,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.onboard_autostart_title),
            style = AppText.Brand,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboard_autostart_body),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.onboard_autostart_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.onboard_autostart_sub),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = onChange,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        // 위젯 안내 — 홈 화면에 위젯을 둬야 사운드/상태를 바로 볼 수 있습니다.
        Surface(
            color = Color.White.copy(alpha = 0.08f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.onboard_widget_label),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.onboard_widget_sub),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.65f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = stringResource(R.string.onboard_finish),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}