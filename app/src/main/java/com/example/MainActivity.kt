package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ad.AdManager
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // AdMob SDK 초기화 (배너/보상형/앱 오픈 프리로드 시작)
        AdManager.initialize(applicationContext)

        setContent {
            MyApplicationTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 사용자가 설정 화면에서 "다른 앱 위에 표시" 권한을 허용하고 돌아올 수 있습니다.
        viewModel.refreshOverlayPermission()
    }

    override fun onResume() {
        super.onResume()
        AdManager.resetSessionCounters()
        // 앱 오픈 광고: 4시간 이상 미접속 + 세션당 1회 + 광고 없는 시간 종료 후에만 표시
        AdManager.maybeShowAppOpen(
            activity = this,
            adFreeUntil = viewModel.settings.value.adFreeUntil,
            onDismissed = { }
        )
    }

    override fun onPause() {
        super.onPause()
        // 충전 애니메이션 화면(다른 Activity)이 광고를 차단한 상태로 넘어왔을 수 있으므로 해제합니다.
        AdManager.setCoreFeatureBusy(false)
    }
}