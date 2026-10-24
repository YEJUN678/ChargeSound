package com.chargesound.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * 워치 앱 진입점.
 *
 * 하는 일은 세 가지뿐입니다.
 * - 휴대폰이 보낸 마지막 배터리 상태 표시
 * - 충전 감지 켜기/끄기 명령 전달
 * - 휴대폰 앱 열기 요청
 */
class MainActivity : ComponentActivity() {

    private val repository: WearRepository by lazy { WearRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ChargeSoundWearTheme {
                val snapshot by repository.snapshot.collectAsState()
                WearScreen(
                    snapshot = snapshot,
                    onToggleService = { repository.sendCommand(WearRepository.CMD_TOGGLE) },
                    onOpenPhone = { repository.sendCommand(WearRepository.CMD_OPEN_APP) },
                    onRefresh = { repository.sendCommand(WearRepository.CMD_REFRESH) }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        repository.start()
    }

    override fun onStop() {
        super.onStop()
        repository.stop()
    }
}
