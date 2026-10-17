package com.example.service

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.battery.BatteryInfoHelper
import com.example.battery.BatteryInfo

/**
 * 홈 화면 위젯.
 *
 * 배터리 잔량과 충전 상태를 항상 표시합니다.
 * 충전 감지 모니터링 서비스가 배터리 변화마다 갱신을 요청합니다.
 */
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
        if (intent.action == ACTION_REFRESH) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context.applicationContext, ChargingWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) onUpdate(context, manager, ids)
        }
    }

    private fun buildViews(context: Context, appWidgetId: Int): RemoteViews {
        val info: BatteryInfo = try {
            BatteryInfoHelper.getBatteryInfo(context)
        } catch (e: Exception) {
            BatteryInfo()
        }

        val views = RemoteViews(context.packageName, R.layout.widget_charging)

        views.setTextViewText(R.id.widget_level, context.getString(R.string.widget_level_fmt, info.level))
        views.setTextViewText(
            R.id.widget_status,
            if (info.isCharging) {
                context.getString(R.string.battery_charging_short)
            } else {
                context.getString(R.string.battery_discharging)
            }
        )
        views.setTextViewText(
            R.id.widget_plug,
            context.getString(
                R.string.widget_plug_fmt,
                context.getString(info.plugType.displayNameRes),
                info.temperatureC
            )
        )
        views.setProgressBar(
            R.id.widget_progress,
            100,
            info.level.coerceIn(0, 100),
            info.isCharging
        )

        // 충전 중일 때만 테마 색을 강조합니다.
        views.setInt(
            R.id.widget_root,
            "setBackgroundColor",
            if (info.isCharging) COLOR_CHARGING else COLOR_IDLE
        )

        val openApp = PendingIntent.getActivity(
            context,
            appWidgetId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, openApp)

        return views
    }

    companion object {
        private const val ACTION_REFRESH = "com.chargesound.app.widget.REFRESH"
        private const val COLOR_CHARGING = 0xFF062A33.toInt()
        private const val COLOR_IDLE = 0xFF0F1115.toInt()

        /** 모든 위젯 인스턴스를 갱신합니다. */
        fun requestUpdate(context: Context) {
            try {
                val intent = Intent(context.applicationContext, ChargingWidgetProvider::class.java)
                    .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                intent.putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_IDS,
                    AppWidgetManager.getInstance(context).getAppWidgetIds(
                        ComponentName(context.applicationContext, ChargingWidgetProvider::class.java)
                    )
                )
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                // 위젯이 하나도 없으면 무시합니다.
            }
        }
    }
}