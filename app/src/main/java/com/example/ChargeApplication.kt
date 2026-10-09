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

        // AdMob SDK 초기화 (배너/보상형/앱 오픈 프리로드)
        com.example.ad.AdManager.initialize(this)

        // Initialize built-in sound presets on background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                SoundManager.initializePresets(this@ChargeApplication)
                val repo = SettingsRepository(this@ChargeApplication)
                val settings = repo.settingsFlow.first()
                if (settings.isServiceEnabled) {
                    ChargingMonitorService.start(this@ChargeApplication)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Init failed", e)
            }
        }
    }
}
