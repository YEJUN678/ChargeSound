package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 재부팅 및 앱 업데이트 시 충전 감지 서비스를 다시 시작합니다.
 *
 * ## Android 15+ 주의사항 (중요)
 * targetSdk 35 이상을 타겟팅하는 앱은 `BOOT_COMPLETED` 리시버에서 대부분의
 * 포그라운드 서비스 타입을 시작할 수 없습니다. 제한된 타입 목록:
 * dataSync, camera, mediaPlayback, phoneCall, mediaProjection, microphone
 *
 * `specialUse` 타입은 공식 제한 목록에는 없으나, AOSP 구현과 Android 16 환경에서
 * 차단 사례가 보고되고 있습니다. 아래 코드는 예외를 안전하게 처리하고
 * 사용자에게 알림을 띄워 "충전 감지가 꺼져 있음"을 인지하도록 합니다.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        Log.d(TAG, "Boot completed or package replaced: $action")

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = SettingsRepository(context)
                val settings = repo.settingsFlow.first()
                if (!settings.isServiceEnabled) return@launch

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
                    action == Intent.ACTION_BOOT_COMPLETED
                ) {
                    // Android 15+ 에서는 재부팅 직후 FGS 시작이 차단될 수 있습니다.
                    // 실패하더라도 앱이 죽지 않도록 예외만 기록하고 사용자에게 알립니다.
                    try {
                        ChargingMonitorService.start(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "FGS start blocked at boot: ${e.message}")
                        ChargingMonitorService.notifyServiceUnavailable(context)
                    }
                } else {
                    try {
                        ChargingMonitorService.start(context)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start service on boot", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "BootReceiver failure", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}