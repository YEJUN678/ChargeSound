package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.RemoteViews
import com.example.R
import com.example.MainActivity
import com.example.battery.BatteryInfo
import com.example.battery.BatteryInfoHelper
import com.example.battery.PowerTracker
import java.util.Date
import kotlin.math.abs

/**
 * 홈 화면 위젯 (RemoteViews 기반, 3 크기 지원).
 *
 * ## 크기 3종
 * - 2×1 (COMPACT): 링 게이지 + 충전 상태 + 전력
 * - 4×1 (STANDARD): 링 게이지 + 상태 + 충전기·온도·전력 칩
 * - 4×2 (DETAILED): 위에다 전압·갱신 시각·새로고침 버튼
 *
 * ## 링 게이지
 * RemoteViews 로는 원형 아크를 그릴 수 없어서 [WidgetRingRenderer] 가 비트맵으로
 * 그려 ImageView 에 넣습니다. 가로 ProgressBar 만 있던 예전 위젯보다 정보 밀도와
 * 가독성이 훨씬 좋아집니다.
 *
 * ## 다크/라이트 자동 — 시스템 테마 따름
 * ## 탭 액션 — 위젯 전체: 앱 열기 / 새로고침 아이콘(DETAILED): 즉시 갱신
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
        newOptions: Bundle
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
        val palette = WidgetPalette.of(isSystemInDarkTheme(context))

        val isCharging = info.isCharging
        val plugName = context.getString(info.plugType.displayNameRes)

        val layoutRes = when (sizeMode) {
            WidgetSize.COMPACT -> R.layout.widget_compact
            WidgetSize.STANDARD -> R.layout.widget_standard
            WidgetSize.DETAILED -> R.layout.widget_detailed
        }
        val views = RemoteViews(context.packageName, layoutRes)

        // ── 배경 ──────────────────────────────────────────────
        views.setInt(R.id.widget_root, "setBackgroundResource", palette.background)

        // ── 링 게이지 ─────────────────────────────────────────
        val ringDp = when (sizeMode) {
            WidgetSize.COMPACT -> 60
            WidgetSize.STANDARD -> 68
            WidgetSize.DETAILED -> 92
        }
        val ringPx = (ringDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(24)
        views.setImageViewBitmap(
            R.id.widget_ring,
            WidgetRingRenderer.render(
                sizePx = ringPx,
                level = info.level,
                accent = palette.accent,
                track = palette.track,
                charging = isCharging
            )
        )

        // ── 잔량 % ────────────────────────────────────────────
        views.setTextViewText(
            R.id.widget_level,
            context.getString(R.string.widget_level_fmt, info.level)
        )
        views.setTextColor(R.id.widget_level, palette.textPrimary)

        // ── 상태 ──────────────────────────────────────────────
        val statusRes = when {
            isCharging -> R.string.widget_status_charging_short
            info.level >= 100 -> R.string.widget_status_full_short
            else -> R.string.widget_status_discharging
        }
        // 충전 중이면 완충 예상 시각을 상태 줄에 덧붙입니다.
        val etaAt = if (isCharging) com.example.service.SessionLive.etaAt(context) else 0L
        val etaLabel = if (etaAt > 0L) DateFormat.getTimeFormat(context).format(Date(etaAt)) else null
        val statusText = if (etaLabel != null) {
            context.getString(statusRes) + " · " + context.getString(R.string.widget_eta_fmt, etaLabel)
        } else {
            context.getString(statusRes)
        }
        views.setTextViewText(R.id.widget_status, statusText)
        views.setTextColor(
            R.id.widget_status,
            if (isCharging) palette.accent else palette.textSecondary
        )

        val wattsLabel = wattsLabel(context, isCharging)
        val tempLabel = context.getString(R.string.widget_temp_fmt, info.temperatureC)
        val voltLabel = context.getString(R.string.widget_volt_fmt, info.voltageV)

        when (sizeMode) {
            WidgetSize.COMPACT -> {
                // 컴팩트는 한 줄뿐이라 ETA 가 있으면 ETA 를, 없으면 전력을 보여줍니다.
                views.setTextViewText(
                    R.id.widget_speed,
                    if (etaLabel != null) {
                        context.getString(R.string.widget_eta_fmt, etaLabel)
                    } else {
                        wattsLabel
                    }
                )
                views.setTextColor(R.id.widget_speed, palette.textSecondary)
            }

            WidgetSize.STANDARD -> {
                views.setInt(R.id.widget_pill_plug, "setBackgroundResource", palette.pill)
                views.setInt(R.id.widget_pill_temp, "setBackgroundResource", palette.pill)
                views.setInt(R.id.widget_pill_watts, "setBackgroundResource", palette.pill)

                views.setTextViewText(R.id.widget_pill_plug, plugName)
                views.setTextColor(R.id.widget_pill_plug, palette.textSecondary)

                views.setTextViewText(R.id.widget_pill_temp, tempLabel)
                views.setTextColor(R.id.widget_pill_temp, palette.textSecondary)

                views.setTextViewText(R.id.widget_pill_watts, wattsLabel)
                views.setTextColor(
                    R.id.widget_pill_watts,
                    if (isCharging) palette.accent else palette.textSecondary
                )
            }

            WidgetSize.DETAILED -> {
                views.setInt(R.id.widget_pill_watts, "setBackgroundResource", palette.pill)
                views.setInt(R.id.widget_pill_volt, "setBackgroundResource", palette.pill)
                views.setInt(R.id.widget_pill_temp, "setBackgroundResource", palette.pill)

                views.setTextViewText(R.id.widget_plug, plugName)
                views.setTextColor(R.id.widget_plug, palette.textSecondary)

                views.setTextViewText(R.id.widget_pill_watts, wattsLabel)
                views.setTextColor(
                    R.id.widget_pill_watts,
                    if (isCharging) palette.accent else palette.textSecondary
                )

                views.setTextViewText(R.id.widget_pill_volt, voltLabel)
                views.setTextColor(R.id.widget_pill_volt, palette.textSecondary)

                views.setTextViewText(R.id.widget_pill_temp, tempLabel)
                views.setTextColor(R.id.widget_pill_temp, palette.textSecondary)

                val timeLabel = DateFormat.getTimeFormat(context).format(Date())
                views.setTextViewText(R.id.widget_hint, context.getString(R.string.widget_hint))
                views.setTextColor(R.id.widget_hint, palette.textDim)

                views.setTextViewText(
                    R.id.widget_updated,
                    context.getString(R.string.widget_updated_fmt, timeLabel)
                )
                views.setTextColor(R.id.widget_updated, palette.textDim)

                views.setInt(R.id.widget_refresh, "setColorFilter", palette.textDim)
                val refresh = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 31 + 7,
                    Intent(context, ChargingWidgetProvider::class.java).setAction(ACTION_REFRESH),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_refresh, refresh)
            }
        }

        val openApp = PendingIntent.getActivity(
            context,
            appWidgetId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openApp)

        return views
    }

    /**
     * 전력 표시 문자열.
     *
     * 측정 자체가 불가능한 기기에서 "0W" 를 보여주면 "충전기가 안 물려 있다"는
     * 잘못된 결론을 유도합니다. 그래서 불가/측정 중을 구분해 표시합니다.
     */
    private fun wattsLabel(context: Context, isCharging: Boolean): String {
        if (!isCharging) return context.getString(R.string.widget_unavailable)
        if (!PowerTracker.isSupported(context)) return context.getString(R.string.widget_unavailable)

        val tracker = PowerTracker.get(context)
        if (!tracker.hasEverMeasured) return context.getString(R.string.widget_measuring)

        val watts = tracker.sample() ?: return context.getString(R.string.widget_measuring)
        if (!tracker.isMeaningful(watts)) return context.getString(R.string.widget_unavailable)

        return context.getString(R.string.widget_watts_fmt, abs(watts))
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
        val nightModeFlags = context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
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

/**
 * 위젯 색 팔레트 — 다크/라이트 두 벌만 있으면 됩니다.
 *
 * 강조색은 라이트에서 시안(#00E5FF)을 그대로 쓰면 흰 배경에서 읽히지 않아
 * 한 단계 진한 청록(#0891B2)으로 낮춥니다.
 */
private class WidgetPalette(
    val background: Int,
    val pill: Int,
    val accent: Int,
    val track: Int,
    val textPrimary: Int,
    val textSecondary: Int,
    val textDim: Int
) {
    companion object {
        fun of(isDark: Boolean): WidgetPalette = if (isDark) {
            WidgetPalette(
                background = R.drawable.widget_bg_dark,
                pill = R.drawable.widget_pill_dark,
                accent = 0xFF00E5FF.toInt(),
                track = 0xFF1E293B.toInt(),
                textPrimary = 0xFFFFFFFF.toInt(),
                textSecondary = 0xFF94A3B8.toInt(),
                textDim = 0xFF64748B.toInt()
            )
        } else {
            WidgetPalette(
                background = R.drawable.widget_bg_light,
                pill = R.drawable.widget_pill_light,
                accent = 0xFF0891B2.toInt(),
                track = 0xFFDDE6F2.toInt(),
                textPrimary = 0xFF0F172A.toInt(),
                textSecondary = 0xFF64748B.toInt(),
                textDim = 0xFF94A3B8.toInt()
            )
        }
    }
}

/** 위젯 크기 설정 액티비티 (앱 설정에서 호출) */
class WidgetSizeConfigActivity : android.app.Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
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
