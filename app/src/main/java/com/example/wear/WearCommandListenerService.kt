package com.example.wear

import android.content.Intent
import android.util.Log
import com.example.MainActivity
import com.example.data.SettingsRepository
import com.example.service.ChargingMonitorService
import com.example.service.ShortcutsHelper
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 워치가 보내는 명령을 받습니다.
 *
 * 워치는 배터리를 읽을 수만 있고 제어할 수는 없으므로, "감지 꺼줘" 같은
 * 요청은 이 서비스가 휴대폰에서 대신 실행합니다.
 *
 * ## 왜 포그라운드 서비스가 아닌가
 * WearableListenerService 는 Play 서비스가 필요할 때만 바인딩하는 서비스라
 * 상시 알림이 필요 없습니다. 몇 초 실행되고 종료됩니다.
 */
class WearCommandListenerService : WearableListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != PhoneWearSync.PATH_COMMAND) return

        val command = try {
            String(messageEvent.data, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.w(TAG, "malformed command payload", e)
            return
        }

        Log.d(TAG, "command: $command")
        when (command) {
            CMD_TOGGLE -> toggleService()
            CMD_OPEN_APP -> openApp()
            CMD_REFRESH -> PhoneWearSync.requestPush(this, force = true)
            else -> Log.i(TAG, "unknown command: $command")
        }
    }

    private fun toggleService() {
        scope.launch {
            try {
                val repository = SettingsRepository(applicationContext)
                val next = !repository.settingsFlow.first().isServiceEnabled
                repository.setServiceEnabled(next)
                // 충전 세션이 바뀌었으므로 알람 발화 기록을 초기화합니다.
                repository.resetAlarmFired()

                if (next) ChargingMonitorService.start(applicationContext)
                else ChargingMonitorService.stop(applicationContext)

                ShortcutsHelper.updateServiceShortcut(applicationContext, next)
                // 토글 결과를 워치 화면에 즉시 반영합니다.
                PhoneWearSync.requestPush(applicationContext, force = true)
            } catch (e: Exception) {
                Log.e(TAG, "toggle from watch failed", e)
            }
        }
    }

    private fun openApp() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // 백그라운드에서 액티비티 시작이 막힌 기기(Android 10+)에서는 실패할 수 있습니다.
            // 그 경우 사용자는 워치 알림만 보게 되지만 앱이 죽지는 않습니다.
            Log.w(TAG, "openApp blocked", e)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "WearCommand"

        private const val CMD_TOGGLE = "toggle_service"
        private const val CMD_OPEN_APP = "open_app"
        private const val CMD_REFRESH = "refresh"
    }
}
