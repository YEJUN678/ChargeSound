package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ad.AdManager
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.interpolateTo

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // ironSource LevelPlay SDK 초기화 (배너/스폰서 카드/보상형 프리로드 시작)
        AdManager.initialize(applicationContext)

        setContent {
            // 강조색은 설정에서 바꾸므로 상태로 읽어야 테마가 다시 그려집니다.
            // (전역 색 상수에 반영하는 방식은 MyApplicationTheme 참조)
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val batteryInfo by viewModel.batteryInfo.collectAsStateWithLifecycle()

            // 충전 중에는 선명한 네온, 완충 상태에서는 차분한 그린으로 자연스럽게 전환합니다.
            // 연결이 끊긴 평소 화면에서는 사용자가 선택한 팔레트를 그대로 유지합니다.
            val userAccent = AccentPalette.fromKey(settings.accentKey, settings.customAccentArgb)
            val completionProgress by animateFloatAsState(
                targetValue = if (batteryInfo.isCharging && batteryInfo.level >= 100) 1f else 0f,
                animationSpec = tween(durationMillis = 1_100),
                label = "full_charge_green_transition"
            )
            val activeAccent = when {
                batteryInfo.isCharging -> AccentPalette.Default.interpolateTo(
                    AccentPalette.FullyCharged,
                    completionProgress
                )
                else -> userAccent
            }

            // 앱 전용 글자 크기 배율.
            // Density 의 fontScale 만 올려서 sp 단위가 전부 함께 커집니다 —
            // 개별 컴포저블에 fontScale 을 흘려보내는 것보다 훨씬 덜 번집니다.
            val density = LocalDensity.current
            val scaled = remember(settings.fontScale) {
                Density(density = density.density, fontScale = density.fontScale * settings.fontScale)
            }

            CompositionLocalProvider(LocalDensity provides scaled) {
                MyApplicationTheme(
                    darkTheme = true,
                    dynamicColor = settings.dynamicColor,
                    highContrast = settings.highContrast,
                    accent = activeAccent
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 사용자가 설정 화면에서 "다른 앱 위에 표시" 권한을 허용하고 돌아올 수 있습니다.
        viewModel.refreshOverlayPermission()
        // 런처 롱프레스 바로가기를 등록합니다 (앱을 한 번 실행하면 자동 생성).
        com.example.service.ShortcutsHelper.install(this)
        // 홈 화면 위젯도 현재 배터리 값으로 맞춰둡니다.
        com.example.widget.ChargingWidgetProvider.requestUpdate(this)
        // 워치 앱이 마지막 값을 들고 있으면 여기서 최신으로 갱신합니다.
        com.example.wear.PhoneWearSync.requestPush(this)
    }

    override fun onResume() {
        super.onResume()
        // ironSource SDK는 포그라운드/백그라운드 전환을 직접 추적해야 합니다.
        AdManager.onResume(this)
        AdManager.loadRewarded(applicationContext)
    }

    override fun onPause() {
        super.onPause()
        AdManager.onPause(this)
        // 충전 애니메이션 화면(다른 Activity)이 광고를 차단한 상태로 넘어왔을 수 있으므로 해제합니다.
        AdManager.setCoreFeatureBusy(false)
    }
}
