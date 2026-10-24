package com.example

import android.app.Application
import android.util.Log
import com.example.data.SettingsRepository
import com.example.service.ChargingMonitorService
import com.example.sound.SoundManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ChargeApplication : Application() {
    companion object {
        private const val TAG = "ChargeApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application onCreate")

        // 크래시 기록기는 가장 먼저 설치합니다 — 아래 초기화가 죽어도 원인이 남도록.
        com.example.diagnostics.CrashReporter.install(this)

        // AdMob SDK 초기화 (배너/네이티브 카드/보상형 광고 로드)
        com.example.ad.AdManager.initialize(this)

        // Initialize built-in sound presets on background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SoundManager.initializePresets(this@ChargeApplication)
                val repo = SettingsRepository(this@ChargeApplication)
                val settings = repo.settingsFlow.first()
                // 위젯 스피커 버튼이 읽을 수 있게 사운드 상태를 미러링합니다.
                com.example.service.SessionLive.writeSoundEnabled(
                    this@ChargeApplication,
                    settings.soundEnabled
                )
                if (settings.isServiceEnabled) {
                    ChargingMonitorService.start(this@ChargeApplication)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Init failed", e)
            }
        }
    }
}
