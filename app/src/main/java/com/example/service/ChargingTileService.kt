package com.example.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.example.MainActivity
import com.example.data.SettingsRepository
import com.example.ui.theme.ElectricCyan
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 알림바 빠른 설정 타일.
 *
 * 앱을 열지 않고 충전 감지를 바로 켜고 끌 수 있습니다.
 */
@RequiresApi(Build.VERSION_CODES.N)
class ChargingTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onStartListening() {
        super.onStartListening()
        refresh()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        scope.launch {
            val repository = SettingsRepository(applicationContext)
            val current = repository.settingsFlow.first().isServiceEnabled
            val next = !current

            repository.setServiceEnabled(next)
            if (next) {
                ChargingMonitorService.start(applicationContext)
            } else {
                ChargingMonitorService.stop(applicationContext)
            }

            // 알람 발화 기록을 초기화해 다음 충전 세션에서 다시 울리도록 합니다.
            repository.resetAlarmFired()

            updateTile(tile, next)
        }
    }

    private fun refresh() {
        val tile = qsTile ?: return
        scope.launch {
            val enabled = SettingsRepository(applicationContext).settingsFlow.first().isServiceEnabled
            updateTile(tile, enabled)
        }
    }

    private fun updateTile(tile: Tile, enabled: Boolean) {
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(
            if (enabled) com.example.R.string.tile_label_on
            else com.example.R.string.tile_label_off
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                if (enabled) com.example.R.string.status_monitoring_active
                else com.example.R.string.status_monitoring_idle
            )
        }
        try {
            tile.updateTile()
        } catch (e: Exception) {
            // 일부 기기에서는 qsTile 이 아직 준비되지 않았을 수 있습니다.
        }
    }
}