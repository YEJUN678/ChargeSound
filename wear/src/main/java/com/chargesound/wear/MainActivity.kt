package com.chargesound.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/**
 * 워치 앱 진입점.
 *
 * 하는 일은 네 가지입니다.
 * - 휴대폰이 보낸 마지막 배터리 상태 표시
 * - 휴대폰 앱이 없으면 **이 기기** 배터리 표시 (아래 설명 참고)
 * - 충전 감지 켜기/끄기 명령 전달
 * - 휴대폰 앱 열기 요청
 *
 * ## 왜 이 기기 배터리로 대신 표시하나
 * Galaxy Store 는 워치 앱을 중국에서만 판매하도록 막고 있습니다. 국내 판매를
 * 하려면 일반 Android 앱으로 등록해야 하고, 그러면 휴대폰에도 설치됩니다.
 * 휴대폰에서 "휴대폰 연결 대기 중" 만 보이면 쓸모가 없으므로, 연결된 휴대폰
 * 값이 없을 때는 이 기기의 배터리를 보여줍니다. 워치에서 실행하면 워치 배터리,
 * 휴대폰에서 실행하면 그 휴대폰 배터리입니다.
 */
class MainActivity : ComponentActivity() {

    private companion object {
        /** 휴대폰 값이 늦게 도착할 수 있어 바로 폴백하지 않습니다. */
        const val LOCAL_FALLBACK_DELAY_MS = 2_500L

        /** 로컬 값은 배터리 변화가 느리므로 30초면 충분합니다. */
        const val LOCAL_REFRESH_MS = 30_000L
    }

    private val repository: WearRepository by lazy { WearRepository(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ChargeSoundWearTheme {
                val remote by repository.snapshot.collectAsState()
                var local by remember { mutableStateOf<PhoneSnapshot?>(null) }

                // 휴대폰 값이 하나도 없을 때만 로컬 배터리로 폴백합니다.
                // remote 가 들어오면 이 효과가 재시작되면서 루프가 취소됩니다.
                LaunchedEffect(remote) {
                    if (remote != null) {
                        local = null
                        return@LaunchedEffect
                    }
                    delay(LOCAL_FALLBACK_DELAY_MS)
                    while (repository.snapshot.value == null) {
                        local = LocalBattery.read(this@MainActivity)
                        delay(LOCAL_REFRESH_MS)
                    }
                }

                WearScreen(
                    snapshot = remote ?: local,
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
