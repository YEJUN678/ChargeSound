package com.example.service

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.R

/**
 * 런처 바로가기 관리.
 *
 * 홈 화면 아이콘을 길게 누르면 나타나는 바로가기 목록을 정의합니다.
 * 사용자가 별도로 설치할 필요가 없고, 앱을 한 번이라도 실행하면 자동으로 생깁니다.
 */
object ShortcutsHelper {

    const val EXTRA_OPEN_SHEET = "open_sheet"
    const val SHEET_ALARM = "alarm"
    const val SHEET_ALERTS = "alerts"
    const val SHEET_PREMIUM = "premium"

    private const val ID_TOGGLE = "chargesound_toggle"
    private const val ID_PREVIEW = "chargesound_preview"
    private const val ID_ALARM = "chargesound_alarm"

    private const val SHORTCUT_CATEGORY = "assistant"

    /** 앱 실행 시 바로가기를 등록/갱신합니다. */
    fun install(context: Context) {
        val icon = IconCompat.createWithResource(context, R.mipmap.ic_launcher)

        val toggle = ShortcutInfoCompat.Builder(context, ID_TOGGLE)
            .setShortLabel(context.getString(R.string.shortcut_toggle_short))
            .setLongLabel(context.getString(R.string.shortcut_toggle_long))
            .setIcon(icon)
            .setIntent(
                Intent(ShortcutActionReceiver.ACTION_TOGGLE_SERVICE)
                    .setClass(context, ShortcutActionReceiver::class.java)
            )
            .setCategories(setOf(SHORTCUT_CATEGORY))
            .build()

        val preview = ShortcutInfoCompat.Builder(context, ID_PREVIEW)
            .setShortLabel(context.getString(R.string.shortcut_preview_short))
            .setLongLabel(context.getString(R.string.shortcut_preview_long))
            .setIcon(icon)
            .setIntent(
                Intent(ShortcutActionReceiver.ACTION_PREVIEW_ANIMATION)
                    .setClass(context, ShortcutActionReceiver::class.java)
            )
            .setCategories(setOf(SHORTCUT_CATEGORY))
            .build()

        val alarm = ShortcutInfoCompat.Builder(context, ID_ALARM)
            .setShortLabel(context.getString(R.string.shortcut_alarm_short))
            .setLongLabel(context.getString(R.string.shortcut_alarm_long))
            .setIcon(icon)
            .setIntent(
                Intent(ShortcutActionReceiver.ACTION_OPEN_ALARM)
                    .setClass(context, ShortcutActionReceiver::class.java)
            )
            .setCategories(setOf(SHORTCUT_CATEGORY))
            .build()

        try {
            ShortcutManagerCompat.addDynamicShortcuts(context, listOf(toggle, preview, alarm))
        } catch (e: Exception) {
            // 일부 런처는 동적 바로가기를 지원하지 않습니다. 무시하고 진행합니다.
        }
    }

    /** 충전 감지 상태가 바뀌면 라벨을 갱신합니다. */
    fun updateServiceShortcut(context: Context, enabled: Boolean) {
        val icon = IconCompat.createWithResource(context, R.mipmap.ic_launcher)
        val updated = ShortcutInfoCompat.Builder(context, ID_TOGGLE)
            .setShortLabel(
                context.getString(
                    if (enabled) R.string.shortcut_toggle_short_off
                    else R.string.shortcut_toggle_short_on
                )
            )
            .setLongLabel(context.getString(R.string.shortcut_toggle_long))
            .setIcon(icon)
            .setIntent(
                Intent(ShortcutActionReceiver.ACTION_TOGGLE_SERVICE)
                    .setClass(context, ShortcutActionReceiver::class.java)
            )
            .setCategories(setOf(SHORTCUT_CATEGORY))
            .build()

        try {
            ShortcutManagerCompat.updateShortcuts(context, listOf(updated))
        } catch (e: Exception) {
            // 지원하지 않는 런처는 무시합니다.
        }
    }
}