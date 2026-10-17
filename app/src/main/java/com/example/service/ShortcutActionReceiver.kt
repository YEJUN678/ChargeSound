package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.R
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 런처 아이콘 롱프레스 바로가기 실행기.
 *
 * `res/xml/shortcuts.xml` 에 정의된 바로가기의 action 을 받아 처리합니다.
 * 앱을 완전히 열지 않고 즉��로 상태를 바꿉니다.
 */
class ShortcutActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE_SERVICE -> toggleService(context)
            ACTION_PREVIEW_ANIMATION -> previewAnimation(context)
            ACTION_OPEN_ALARM -> openSettings(context)
        }
    }

    private fun toggleService(context: Context) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = SettingsRepository(context)
                val next = !repository.settingsFlow.first().isServiceEnabled
                repository.setServiceEnabled(next)
                // 알람 발화 기록을 초기화해 다음 충전 세션에서 다시 울리도록 합니다.
                repository.setAlarmFiredThisSession(false)

                if (next) ChargingMonitorService.start(context) else ChargingMonitorService.stop(context)

                // 스위치 상태를 런처 바로가기 아이콘에도 반영합니다.
                ShortcutsHelper.updateServiceShortcut(context, next)
            } catch (e: Exception) {
                Log.e(TAG, "toggle failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun previewAnimation(context: Context) {
        try {
            val intent = Intent(context, com.example.ui.ChargingAnimationActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra(com.example.ui.ChargingAnimationActivity.EXTRA_BATTERY_LEVEL, 75)
                putExtra(
                    com.example.ui.ChargingAnimationActivity.EXTRA_PLUG_NAME,
                    context.getString(R.string.plug_preview)
                )
                putExtra(
                    com.example.ui.ChargingAnimationActivity.EXTRA_ANIM_MODE,
                    com.example.data.AnimationMode.BUILTIN_LIGHTNING.name
                )
                putExtra(com.example.ui.ChargingAnimationActivity.EXTRA_DURATION_SEC, 5)
                putExtra(com.example.ui.ChargingAnimationActivity.EXTRA_LOOP, false)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "preview failed", e)
        }
    }

    private fun openSettings(context: Context) {
        try {
            val intent = Intent(context, com.example.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(ShortcutsHelper.EXTRA_OPEN_SHEET, ShortcutsHelper.SHEET_ALARM)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "open settings failed", e)
        }
    }

    companion object {
        private const val TAG = "ShortcutAction"
        const val ACTION_TOGGLE_SERVICE = "com.chargesound.app.action.TOGGLE_SERVICE"
        const val ACTION_PREVIEW_ANIMATION = "com.chargesound.app.action.PREVIEW_ANIMATION"
        const val ACTION_OPEN_ALARM = "com.chargesound.app.action.OPEN_ALARM"
    }
}