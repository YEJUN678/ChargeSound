package com.example.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.widget.RemoteViews
import com.example.R
import com.example.battery.BatteryInfo
import com.example.battery.BatteryInfoHelper
import com.example.battery.PowerTracker
import com.example.data.SettingsRepository
import com.example.MainActivity
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

/**
 * 홈 화면 위젯 (RemoteViews 기반, 3 크기 지원).
 *
 * ## 크기 3종
 * - 2×1 (COMPACT): 배터리% + 충전 상태만
 * - 4×1 (STANDARD): 배터리% + 상태 + 충전기 + 진행바
 * - 4×2 (DETAILED): 위에다 충전 속도(W) + 온도 + 힌트
 *
 * ## 다크/라이트 자동 — 시스템 테마 따름
 * ## 탭 액션 — 위젯 전체가 MainActivity 열기
 */
enum class WidgetSize { COMPACT, STANDARD, DETAILED }

class ChargingWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildViews(context, id))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context.applicationContext, ChargingWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) onUpdate(context, manager, ids)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, appWidgetId))
    }

    private fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val info: BatteryInfo = try {
            BatteryInfoHelper.getBatteryInfo(context)
        } catch (e: Exception) {
            BatteryInfo()
        }

        val sizeMode = getWidgetSizeMode(context, appWidgetId)

        // 색상 — 다크/라이트 자동 (Color.value 는 ULong 이므로 Int 로 변환)
        val isDark = isSystemInDarkTheme(context)
        val bgColor = if (isDark) Slate950.value.toInt() else Slate800.value.toInt()
        val textPrimary = if (isDark) Color.WHITE else Color.BLACK
        val textSecondary = if (isDark) Slate400.value.toInt() else Slate600.value.toInt()
        val accentColor = if (isDark) ElectricCyan.value.toInt() else ElectricCyan.value.toInt()
        val chargingBg = if (isDark) Color.parseColor("#062A33") else Color.parseColor("#E0F2F1")
        val idleBg = if (isDark) Slate900.value.toInt() else Slate800.value.toInt()

        val plugName = context.getString(info.plugType.displayNameRes)
        val isCharging = info.isCharging

        // 충전 속도 (DETAILED 모드에서만)
        val powerTracker = PowerTracker.get(context)
        val watts = powerTracker.sample()
        val speedLabel = when {
            !powerTracker.hasEverMeasured -> "측정 불가"
            watts == null -> "측정 중…"
            watts >= 20f -> String.format("고속 (%.0fW)", watts)
            watts >= 8f -> String.format("일반 (%.0fW)", watts)
            else -> String.format("저속 (%.1fW)", watts)
        }

        val layoutRes = when (sizeMode) {
            WidgetSize.COMPACT -> R.layout.widget_compact
            WidgetSize.STANDARD -> R.layout.widget_standard
            WidgetSize.DETAILED -> R.layout.widget_detailed
        }

        val views = RemoteViews(context.packageName, layoutRes)

        // 공통: 배경색
        val rootBg = if (isCharging) chargingBg else idleBg
        views.setInt(R.id.widget_root, "setBackgroundColor", rootBg)

        // 공통: 배터리 %
        views.setTextViewText(R.id.widget_level, "${info.level}%")
        views.setTextColor(R.id.widget_level, textPrimary)

        // 공통: 충전 상태
        val statusText = if (isCharging) "⚡ 충전 중" else "방전 중"
        views.setTextViewText(R.id.widget_status, statusText)
        views.setTextColor(R.id.widget_status, if (isCharging) accentColor else textSecondary)

        when (sizeMode) {
            WidgetSize.STANDARD, WidgetSize.DETAILED -> {
                views.setTextViewText(R.id.widget_plug, "$plugName · ${info.temperatureC}°C")
                views.setTextColor(R.id.widget_plug, textSecondary)
                views.setProgressBar(
                    R.id.widget_progress,
                    100,
                    info.level.coerceIn(0, 100),
                    isCharging
                )
            }
            WidgetSize.COMPACT -> {
            }
        }

        if (sizeMode == WidgetSize.DETAILED) {
            views.setTextViewText(R.id.widget_speed, speedLabel)
            views.setTextColor(R.id.widget_speed, if (isCharging) accentColor else textSecondary)
            views.setViewVisibility(R.id.widget_speed, android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_hint, android.view.View.VISIBLE)
            views.setTextViewText(R.id.widget_hint, "탭해서 애니메이션 열기")
            views.setTextColor(R.id.widget_hint, textSecondary)
        }

        val openApp = android.app.PendingIntent.getActivity(
            context,
            appWidgetId,
            Intent(context, MainActivity::class.java),
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openApp)

        return views
    }

    private fun getWidgetSizeMode(context: Context, appWidgetId: Int): WidgetSize {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        val key = "widget_size_$appWidgetId"
        return when (prefs.getString(key, "standard")) {
            "compact" -> WidgetSize.COMPACT
            "detailed" -> WidgetSize.DETAILED
            else -> WidgetSize.STANDARD
        }
    }

    private fun isSystemInDarkTheme(context: Context): Boolean {
        val nightModeFlags = context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    companion object {
        private const val ACTION_REFRESH = "com.chargesound.app.widget.REFRESH"

        fun requestUpdate(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(
                    ComponentName(context.applicationContext, ChargingWidgetProvider::class.java)
                )
                if (ids.isNotEmpty()) {
                    val intent = Intent(context, ChargingWidgetProvider::class.java)
                        .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    context.sendBroadcast(intent)
                }
            } catch (e: Exception) {
            }
        }

        fun requestUpdateWithSize(context: Context, size: WidgetSize, appWidgetId: Int) {
            val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString("widget_size_$appWidgetId", size.name.lowercase()).apply()
            requestUpdate(context)
        }
    }
}

/** 위젯 크기 설정 액티비티 (앱 설정에서 호출) */
class WidgetSizeConfigActivity : android.app.Activity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setContentView(R.layout.widget_size_config)
        findViewById<android.widget.RadioGroup>(R.id.size_radio_group).setOnCheckedChangeListener { _, checkedId ->
            val size = when (checkedId) {
                R.id.rb_compact -> WidgetSize.COMPACT
                R.id.rb_detailed -> WidgetSize.DETAILED
                else -> WidgetSize.STANDARD
            }
            ChargingWidgetProvider.requestUpdateWithSize(this, size, appWidgetId)
            val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, result)
            finish()
        }
    }
}