package com.example.service

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.SweepGradient
import android.graphics.Typeface
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.battery.PlugType
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 엣지 라이팅의 모양/속도 설정.
 *
 * @param opacity 전체 밝기(불투명도). 0.15 ~ 1.0
 * @param sweepMs 그라데이션이 한 바퀴 도는 시간(ms)
 * @param colorStart 사용자 지정 그라데이션 시작 색 (ARGB)
 * @param colorMid 사용자 지정 그라데이션 중간 색 (ARGB)
 * @param colorEnd 사용자 지정 그라데이션 끝 색 (ARGB)
 * @param autoColor true 면 잔량에 따라 자동 팔레트를 쓴다 (사용자 색상은 무시)
 * @param gauge true 면 테두리 전체가 아니라 잔량만큼만 채운다
 * @param breathing true 면 밝기가 천천히 오르내린다
 * @param comet true 면 회전하는 빛에 혜성 꼬리와 머리를 붙인다
 * @param sparks true 면 모서리에서 짧은 불꽃이 튄다
 * @param pill true 면 충전 시작 시 상단에 잔량 알약을 띄운다
 * @param liquid true 면 화면 아래에서 잔량만큼 차오르는 물결을 그린다
 */
data class EdgeGlowStyle(
    val opacity: Float = 0.9f,
    val sweepMs: Int = 5_500,
    val colorStart: Int = 0xFF22D3EE.toInt(),
    val colorMid: Int = 0xFF4ADE80.toInt(),
    val colorEnd: Int = 0xFFFBBF24.toInt(),
    val autoColor: Boolean = true,
    val gauge: Boolean = false,
    val breathing: Boolean = true,
    val comet: Boolean = true,
    val sparks: Boolean = true,
    val pill: Boolean = true,
    val liquid: Boolean = true
)

/**
 * 충전기가 꽂힐 때 화면 가장자리를 따라 네온 빛이 도는 효과.
 *
 * ## 2.0
 * - **잔량에 따라 색이 바뀝니다.** 20% 이하 = 빨강, 21~79% = 호박색, 80% 이상 = 청록.
 * - **그라데이션이 모서리를 따라 계속 회전합니다.** (상시 표시에서도 천천히 계속)
 * - **충전 중에는 은은하게 계속 남습니다.** (설정에서 끄면 잠깐만 표시)
 * - **무선 충전이면 코일 위치(화면 중앙)에서 펄스가 퍼집니다.**
 * - 상태바/노치까지 **화면 전체**를 덮도록 실제 디스플레이 크기로 그립니다.
 *
 * ## 왜 서비스인가
 * 다른 앱 위에 그리려면 ACTIVITY 가 아니라 오버레이 윈도우가 필요하고,
 * 오버레이는 백그라운드에서 유지되므로 포그라운드 서비스로 띄운다.
 * (SYSTEM_ALERT_WINDOW 권한이 없으면 조용히 아무것도 하지 않는다.)
 */
class EdgeGlowService : Service() {

    companion object {
        private const val TAG = "EdgeGlowService"
        private const val CHANNEL_ID = "edge_glow_channel"
        private const val NOTIFICATION_ID = 1004

        private const val EXTRA_LEVEL = "level"
        private const val EXTRA_PLUG = "plug"
        private const val EXTRA_PERSISTENT = "persistent"
        private const val EXTRA_PREVIEW = "preview"
        private const val EXTRA_OPACITY = "opacity"
        private const val EXTRA_SWEEP_MS = "sweep_ms"
        private const val EXTRA_COLOR_START = "color_start"
        private const val EXTRA_COLOR_MID = "color_mid"
        private const val EXTRA_COLOR_END = "color_end"
        private const val EXTRA_AUTO_COLOR = "auto_color"
        private const val EXTRA_GAUGE = "gauge"
        private const val EXTRA_BREATHING = "breathing"
        private const val EXTRA_COMET = "comet"
        private const val EXTRA_SPARKS = "sparks"
        private const val EXTRA_PILL = "pill"
        private const val EXTRA_LIQUID = "liquid"

        private const val FADE_IN_MS = 280L
        private const val BURST_HOLD_MS = 1_400L
        private const val PREVIEW_HOLD_MS = 3_200L
        private const val FADE_OUT_MS = 700L
        private const val PILL_HOLD_MS = 4_600L

        private val MILESTONES = intArrayOf(25, 50, 75, 100)

        /**
         * 충전기 연결 직후 엣지 라이팅을 띄운다.
         *
         * @param level 현재 배터리 잔량 (%) — 자동 색상 모드에서 사용
         * @param plug 충전 방식 — WIRELESS 이면 코일 펄스를 함께 그린다
         * @param persistent true 면 충전이 끝날 때까지 은은하게 유지
         * @param style 투명도·색상·회전 속도 설정
         */
        fun start(
            context: Context,
            level: Int,
            plug: PlugType,
            persistent: Boolean,
            style: EdgeGlowStyle = EdgeGlowStyle()
        ) {
            launch(
                context = context,
                level = level,
                plug = plug,
                persistent = persistent,
                preview = false,
                style = style
            )
        }

        /** 설정 화면에서 효과를 바로 확인할 때 쓴다. (잠깐 밝게 보여 주고 사라짐) */
        fun preview(
            context: Context,
            level: Int,
            plug: PlugType,
            style: EdgeGlowStyle = EdgeGlowStyle()
        ) {
            launch(
                context = context,
                level = level,
                plug = plug,
                persistent = false,
                preview = true,
                style = style
            )
        }

        private fun launch(
            context: Context,
            level: Int,
            plug: PlugType,
            persistent: Boolean,
            preview: Boolean,
            style: EdgeGlowStyle
        ) {
            val intent = Intent(context, EdgeGlowService::class.java).apply {
                putExtra(EXTRA_LEVEL, level)
                putExtra(EXTRA_PLUG, plug.name)
                putExtra(EXTRA_PERSISTENT, persistent && !preview)
                putExtra(EXTRA_PREVIEW, preview)
                putExtra(EXTRA_OPACITY, style.opacity)
                putExtra(EXTRA_SWEEP_MS, style.sweepMs)
                putExtra(EXTRA_COLOR_START, style.colorStart)
                putExtra(EXTRA_COLOR_MID, style.colorMid)
                putExtra(EXTRA_COLOR_END, style.colorEnd)
                putExtra(EXTRA_AUTO_COLOR, style.autoColor)
                putExtra(EXTRA_GAUGE, style.gauge)
                putExtra(EXTRA_BREATHING, style.breathing)
                putExtra(EXTRA_COMET, style.comet)
                putExtra(EXTRA_SPARKS, style.sparks)
                putExtra(EXTRA_PILL, style.pill)
                putExtra(EXTRA_LIQUID, style.liquid)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not start edge glow", e)
            }
        }

        /** 충전기가 뽑히면 상시 표시를 즉시 내린다. */
        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, EdgeGlowService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Could not stop edge glow", e)
            }
        }
    }

    private var overlayView: EdgeGlowView? = null
    private var pillView: ChargingPillView? = null
    private var windowManager: WindowManager? = null
    private var batteryReceiver: BroadcastReceiver? = null
    private var currentPlug: PlugType = PlugType.NONE
    private var previewMode: Boolean = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (!Settings.canDrawOverlays(this)) {
            Log.i(TAG, "Overlay permission missing - skipping edge glow")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val level = intent?.getIntExtra(EXTRA_LEVEL, -1) ?: -1
        val plug = intent?.getStringExtra(EXTRA_PLUG)
            ?.let { name -> runCatching { PlugType.valueOf(name) }.getOrNull() }
            ?: PlugType.NONE
        val preview = intent?.getBooleanExtra(EXTRA_PREVIEW, false) ?: false
        val persistent = intent?.getBooleanExtra(EXTRA_PERSISTENT, true) ?: true
        val style = EdgeGlowStyle(
            opacity = (intent?.getFloatExtra(EXTRA_OPACITY, 0.9f) ?: 0.9f).coerceIn(0.15f, 1f),
            sweepMs = (intent?.getIntExtra(EXTRA_SWEEP_MS, 5_500) ?: 5_500).coerceIn(1_500, 15_000),
            colorStart = intent?.getIntExtra(EXTRA_COLOR_START, 0xFF22D3EE.toInt())
                ?: 0xFF22D3EE.toInt(),
            colorMid = intent?.getIntExtra(EXTRA_COLOR_MID, 0xFF4ADE80.toInt())
                ?: 0xFF4ADE80.toInt(),
            colorEnd = intent?.getIntExtra(EXTRA_COLOR_END, 0xFFFBBF24.toInt())
                ?: 0xFFFBBF24.toInt(),
            autoColor = intent?.getBooleanExtra(EXTRA_AUTO_COLOR, true) ?: true,
            gauge = intent?.getBooleanExtra(EXTRA_GAUGE, false) ?: false,
            breathing = intent?.getBooleanExtra(EXTRA_BREATHING, true) ?: true,
            comet = intent?.getBooleanExtra(EXTRA_COMET, true) ?: true,
            sparks = intent?.getBooleanExtra(EXTRA_SPARKS, true) ?: true,
            pill = intent?.getBooleanExtra(EXTRA_PILL, true) ?: true,
            liquid = intent?.getBooleanExtra(EXTRA_LIQUID, true) ?: true
        )

        val existing = overlayView
        if (existing != null) {
            // 이미 떠 있으면 설정만 갱신한다. (연속 연결 · 충전 중 미리보기)
            if (level >= 0) existing.batteryLevel = level
            existing.isWireless = plug == PlugType.WIRELESS
            existing.style = style
            existing.invalidate()
            currentPlug = plug
            previewMode = preview
            pillView?.update(level.coerceAtLeast(0), plug, pillStatus(level, plug), accentFor(level, style))
            if (preview && style.pill && pillView == null) {
                showPill(level.coerceAtLeast(0), plug, style)
            }
            return START_NOT_STICKY
        }

        currentPlug = plug
        previewMode = preview
        showOverlay(level.coerceAtLeast(0), plug, persistent, preview, style)
        return START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // 화면 회전 시 오버레이가 잘리지 않도록 실제 화면 크기로 다시 맞춘다.
        val view = overlayView ?: return
        val wm = windowManager ?: return
        val params = view.layoutParams as? WindowManager.LayoutParams ?: return
        val size = screenSize(wm)
        if (params.width != size.x || params.height != size.y) {
            params.width = size.x
            params.height = size.y
            runCatching { wm.updateViewLayout(view, params) }
        }
    }

    override fun onDestroy() {
        batteryReceiver?.let {
            runCatching { unregisterReceiver(it) }
        }
        batteryReceiver = null
        // 서비스가 끝나면 알약은 애니메이션 없이 즉시 걷어낸다. (윈도우 누수 방지)
        removePillImmediately()
        removeOverlay()
        super.onDestroy()
    }

    private fun removePillImmediately() {
        val view = pillView ?: return
        pillView = null
        try {
            windowManager?.removeViewImmediate(view)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to remove charging pill immediately", e)
        }
    }

    private fun screenSize(wm: WindowManager): Point {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.currentWindowMetrics.bounds
            Point(bounds.width(), bounds.height())
        } else {
            val point = Point()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealSize(point)
            point
        }
    }

    private fun showOverlay(
        level: Int,
        plug: PlugType,
        persistent: Boolean,
        preview: Boolean,
        style: EdgeGlowStyle
    ) {
        val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        if (wm == null) {
            stopSelf()
            return
        }
        windowManager = wm

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val size = screenSize(wm)
        val params = WindowManager.LayoutParams(
            size.x,
            size.y,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // 노치/컷아웃 영역까지 그려서 화면 맨 위 가장자리까지 빛이 닿게 한다.
            params.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }

        val view = EdgeGlowView(this).apply {
            batteryLevel = level
            isWireless = plug == PlugType.WIRELESS
            this.style = style
            alpha = 0f
        }
        try {
            wm.addView(view, params)
            overlayView = view
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add edge glow overlay", e)
            stopSelf()
            return
        }

        // 잔량이 바뀌면 색을 다시 계산한다. (ACTION_BATTERY_CHANGED 는 sticky 라 즉시 한 번 온다)
        registerBatteryReceiver(view)

        if (style.pill) showPill(level, plug, style)

        val peakAlpha = style.opacity.coerceIn(0.15f, 1f)
        val persistentAlpha = peakAlpha * 0.9f

        view.startBurst()
        view.animate()
            .alpha(peakAlpha)
            .setDuration(FADE_IN_MS)
            .withEndAction {
                if (persistent) {
                    // 밝은 스윕이 끝난 뒤 은은한 상시 표시로 내려가고, 회전은 계속된다.
                    view.animate()
                        .alpha(persistentAlpha)
                        .setStartDelay(BURST_HOLD_MS)
                        .setDuration(FADE_OUT_MS)
                        .withEndAction {
                            view.settlePersistent()
                            updateNotification()
                        }
                        .start()
                } else {
                    view.animate()
                        .alpha(0f)
                        .setStartDelay(if (preview) PREVIEW_HOLD_MS else BURST_HOLD_MS)
                        .setDuration(FADE_OUT_MS)
                        .withEndAction {
                            // 알약이 남아 있으면 알약이 접힐 때까지 서비스를 유지한다.
                            removeOverlay(keepPill = true)
                            if (pillView != null) {
                                Handler(mainLooper).postDelayed(
                                    { stopSelf() },
                                    PILL_HOLD_MS + 300L
                                )
                            } else {
                                stopSelf()
                            }
                        }
                        .start()
                }
            }
            .start()
    }

    private fun registerBatteryReceiver(view: EdgeGlowView) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level < 0 || scale <= 0) return
                val newLevel = level * 100 / scale
                val previous = view.batteryLevel
                view.batteryLevel = newLevel

                // 25/50/75/100% 를 지날 때마다 모서리 불꽃으로 축하한다.
                if (MILESTONES.any { previous < it && newLevel >= it }) {
                    view.celebrate()
                }
                pillView?.update(
                    level = newLevel,
                    plug = currentPlug,
                    status = pillStatus(newLevel, currentPlug),
                    accent = accentFor(newLevel, view.style)
                )
            }
        }
        try {
            registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            batteryReceiver = receiver
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register battery receiver", e)
        }
    }

    // ───────────────────────── 충전 알약 (다이나믹 아일랜드) ─────────────────────────

    private fun showPill(level: Int, plug: PlugType, style: EdgeGlowStyle) {
        val wm = windowManager ?: return
        val existing = pillView
        if (existing != null) {
            existing.update(level, plug, pillStatus(level, plug), accentFor(level, style))
            return
        }
        if (Settings.canDrawOverlays(this).not()) return

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }
        val barHeight = statusBarHeight()
        val params = WindowManager.LayoutParams(
            dp(286),
            dp(78),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = barHeight + dp(6)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        val view = ChargingPillView(this).apply {
            update(level, plug, pillStatus(level, plug), accentFor(level, style))
            alpha = 0f
        }
        try {
            wm.addView(view, params)
            pillView = view
        } catch (e: Exception) {
            Log.w(TAG, "Failed to add charging pill", e)
            return
        }

        view.play()
        view.animate().alpha(1f).setDuration(200L).start()
        view.postDelayed({
            if (pillView === view) hidePill()
        }, PILL_HOLD_MS)
    }

    private fun hidePill() {
        val view = pillView ?: return
        pillView = null
        view.animate()
            .alpha(0f)
            .setDuration(240L)
            .withEndAction {
                try {
                    windowManager?.removeView(view)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to remove charging pill", e)
                }
            }
            .start()
    }

    /** 잔량과 충전 방식으로 알약의 부제목을 만든다. */
    private fun pillStatus(level: Int, plug: PlugType): String {
        if (previewMode) return getString(R.string.edge_glow_pill_preview)
        if (level >= 100) return getString(R.string.edge_glow_pill_full)
        val minutes = estimateMinutesToFull(level)
        if (minutes == null) {
            return if (plug == PlugType.WIRELESS) {
                getString(R.string.edge_glow_pill_wireless)
            } else {
                getString(R.string.edge_glow_pill_charging)
            }
        }
        return if (minutes >= 60) {
            getString(R.string.edge_glow_pill_hours, minutes / 60, minutes % 60)
        } else {
            getString(R.string.edge_glow_pill_minutes, minutes)
        }
    }

    /**
     * 남은 충전 시간(분)을 추정한다.
     *
     * 기기가 CURRENT_NOW / CHARGE_COUNTER 를 제공하지 않으면 null 을 돌려주고,
     * 알약에는 "충전 중"만 표시한다.
     */
    private fun estimateMinutesToFull(level: Int): Int? {
        if (level <= 0) return null
        return try {
            val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return null
            val currentMa =
                abs(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)).toFloat() / 1_000f
            val chargeMah =
                bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1_000f
            if (currentMa < 50f || chargeMah <= 0f) return null
            val capacityMah = chargeMah * 100f / level
            val remainingMah = (capacityMah - chargeMah).coerceAtLeast(0f)
            val minutes = (remainingMah / currentMa * 60f).toInt()
            minutes.takeIf { it in 1..900 }
        } catch (e: Exception) {
            null
        }
    }

    private fun accentFor(level: Int, style: EdgeGlowStyle): Int {
        if (!style.autoColor) return style.colorStart
        return when {
            level >= 80 -> 0xFF22D3EE.toInt()
            level <= 20 -> 0xFFFF3B30.toInt()
            else -> 0xFFFBBF24.toInt()
        }
    }

    private fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        val height = if (id > 0) resources.getDimensionPixelSize(id) else dp(28)
        return height
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun updateNotification() {
        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update edge glow notification", e)
        }
    }

    private fun removeOverlay(keepPill: Boolean = false) {
        if (!keepPill) hidePill()
        val view = overlayView ?: return
        overlayView = null
        try {
            windowManager?.removeView(view)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to remove edge glow overlay", e)
        }
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        getString(R.string.edge_glow_channel_name),
                        NotificationManager.IMPORTANCE_MIN
                    ).apply {
                        description = getString(R.string.edge_glow_channel_desc)
                        setShowBadge(false)
                    }
                )
            }
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.charge_sound_icon_1791457205401)
            .setContentTitle(getString(R.string.edge_glow_channel_name))
            .setContentText(getString(R.string.edge_glow_channel_desc))
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }
}

/**
 * 화면 가장자리를 따라 도는 네온 링.
 *
 * 블러(BlurMaskFilter)는 기기별 렌더링 차이가 커서, 폭이 다른 스트로크를
 * 겹쳐 그리는 방식으로 부드러운 빛 번짐을 만든다. 무선 충전일 때는
 * 화면 중앙(코일 위치)에서 동심원 펄스가 퍼진다.
 *
 * 상시 표시에서도 그라데이션 회전은 멈추지 않는다. (설정한 속도로)
 */
private class EdgeGlowView(context: Context) : View(context) {

    /** 0~100. 색상 팔레트를 고르는 데 쓴다. */
    var batteryLevel: Int = 50
        set(value) {
            field = value.coerceIn(0, 100)
            invalidate()
        }

    /** 무선 충전이면 중앙 코일 펄스를 함께 그린다. */
    var isWireless: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    /** 투명도 · 색상 · 회전 속도. 설정이 바뀌면 즉시 반영한다. */
    var style: EdgeGlowStyle = EdgeGlowStyle()
        set(value) {
            field = value
            sweepAnimator.duration = value.sweepMs.toLong().coerceIn(1_500L, 15_000L)
            if (value.breathing && !breathAnimator.isStarted) breathAnimator.start()
            if (value.liquid && !waveAnimator.isStarted) waveAnimator.start()
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private var phase = 0f
    private var pulse = 0f
    private var breath = 1f
    private var particleClock = 0f
    private var sparkT = 0f
    private var wavePhase = 0f

    /** 테두리를 따라가는 경로. 게이지/혜성 꼬리를 그릴 때 쓴다. */
    private val ringPath = Path()
    private val segmentPath = Path()
    private val pathMeasure = PathMeasure()
    private var ringLength = 0f
    private var ringInset = 0f
    private var sparkAnimator: ValueAnimator? = null

    private val sweepAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2_800L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            invalidate()
        }
    }

    private val pulseAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2_200L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            pulse = it.animatedValue as Float
            invalidate()
        }
    }

    /** 숨쉬기: 3.8초 주기로 밝기가 62% ~ 100% 사이를 오간다. */
    private val breathAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3_800L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            val t = it.animatedValue as Float
            breath = 0.62f + 0.38f * ((cos(t * 2.0 * Math.PI) + 1.0) / 2.0).toFloat()
            invalidate()
        }
    }

    /** 무선 충전 때 떠오르는 작은 빛 알갱이의 시간축. */
    private val particleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 6_000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            particleClock = it.animatedValue as Float
            invalidate()
        }
    }

    /** 하단 물결의 흐름. */
    private val waveAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 5_500L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            wavePhase = it.animatedValue as Float
            invalidate()
        }
    }

    fun startBurst() {
        if (!sweepAnimator.isStarted) sweepAnimator.start()
        if (isWireless && !pulseAnimator.isStarted) pulseAnimator.start()
        if (isWireless && !particleAnimator.isStarted) particleAnimator.start()
        if (style.breathing && !breathAnimator.isStarted) breathAnimator.start()
        if (style.liquid && !waveAnimator.isStarted) waveAnimator.start()
    }

    /** 25/50/75/100% 처럼 의미 있는 지점에서 모서리 불꽃을 한 번 터뜨린다. */
    fun celebrate() {
        if (!style.sparks) return
        sparkAnimator?.cancel()
        sparkAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1_150L
            addUpdateListener {
                sparkT = it.animatedValue as Float
                invalidate()
            }
        }
        sparkT = 0.001f
        sparkAnimator?.start()
    }

    /** 상시 표시 모드: 설정된 회전 속도 그대로 계속 유지한다. */
    fun settlePersistent() {
        if (!sweepAnimator.isStarted) {
            sweepAnimator.duration = style.sweepMs.toLong().coerceIn(1_500L, 15_000L)
            sweepAnimator.start()
        }
        if (style.breathing && !breathAnimator.isStarted) breathAnimator.start()
        if (style.liquid && !waveAnimator.isStarted) waveAnimator.start()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0 || h <= 0) return
        // 스트로크가 화면 밖으로 잘리지 않을 만큼만 안쪽으로 넣는다.
        ringInset = dp(9f)
        ringPath.reset()
        ringPath.addRoundRect(
            RectF(ringInset, ringInset, w - ringInset, h - ringInset),
            dp(30f),
            dp(30f),
            Path.Direction.CW
        )
        pathMeasure.setPath(ringPath, false)
        ringLength = pathMeasure.length
    }

    override fun onDetachedFromWindow() {
        sweepAnimator.cancel()
        pulseAnimator.cancel()
        breathAnimator.cancel()
        particleAnimator.cancel()
        waveAnimator.cancel()
        sparkAnimator?.cancel()
        sparkAnimator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        if (cx <= 1f || cy <= 1f || ringLength <= 0f) return

        val palette = palette()
        val shader = SweepGradient(cx, cy, palette, null)
        shader.setLocalMatrix(Matrix().apply { setRotate(phase * 360f, cx, cy) })
        paint.shader = shader
        paint.strokeCap = Paint.Cap.ROUND

        val ease = if (style.breathing) breath else 1f
        val frac = (batteryLevel / 100f).coerceIn(0.02f, 1f)

        // 잔량만큼 차오르는 하단 물결 (테두리보다 뒤에 깔린다)
        drawLiquid(canvas, palette[0])

        // 게이지 모드에서는 아직 채워지지 않은 부분을 아주 흐린 선으로 남겨
        // 테두리가 사라진 것처럼 보이지 않게 한다.
        if (style.gauge) {
            val trackPaint = Paint(paint).apply {
                strokeWidth = dp(6f)
                alpha = 22
            }
            canvas.drawPath(ringPath, trackPaint)
        }

        // 넓고 흐린 층 → 중간 → 얇고 밝은 코어
        drawRingLayer(canvas, frac, 34f, (58 * ease).toInt())
        drawRingLayer(canvas, frac, 16f, (140 * ease).toInt())
        drawRingLayer(canvas, frac, 5f, (255 * ease).toInt())

        drawComet(canvas, palette[0], frac)
        drawCornerSparks(canvas, palette[0])
        if (isWireless) {
            drawCoilPulses(canvas, cx, cy, palette[0])
            drawParticles(canvas, palette[0])
        }
    }

    /** 게이지 모드면 잔량만큼, 아니면 테두리 전체를 그린다. */
    private fun drawRingLayer(canvas: Canvas, frac: Float, widthDp: Float, alpha: Int) {
        if (alpha <= 0) return
        paint.strokeWidth = dp(widthDp)
        paint.alpha = alpha.coerceIn(0, 255)
        if (style.gauge) {
            drawSegment(0f, ringLength * frac, paint, canvas)
        } else {
            canvas.drawPath(ringPath, paint)
        }
    }

    /** 경로의 일부 구간을 그리고, 끝을 넘어가면 두 조각으로 나눠 이음새를 없앤다. */
    private fun drawSegment(startDistance: Float, endDistance: Float, paint: Paint, canvas: Canvas) {
        if (ringLength <= 0f) return
        val length = endDistance - startDistance
        if (length <= 0f) return
        var start = ((startDistance % ringLength) + ringLength) % ringLength
        var end = start + length
        segmentPath.reset()
        if (end <= ringLength) {
            pathMeasure.getSegment(start, end, segmentPath, true)
            canvas.drawPath(segmentPath, paint)
        } else {
            pathMeasure.getSegment(start, ringLength, segmentPath, true)
            canvas.drawPath(segmentPath, paint)
            segmentPath.reset()
            pathMeasure.getSegment(0f, end - ringLength, segmentPath, true)
            canvas.drawPath(segmentPath, paint)
        }
    }

    /** 혜성: 밝은 머리 + 뒤로 사라지는 꼬리. */
    private fun drawComet(canvas: Canvas, accent: Int, frac: Float) {
        if (!style.comet || ringLength <= 0f) return
        val headDistance = if (style.gauge) ringLength * frac else ringLength * phase
        val tailLength = ringLength * 0.16f
        val ease = if (style.breathing) breath else 1f

        val tailPaint = Paint(paint).apply {
            shader = null
            color = accent
            strokeWidth = dp(7f)
            alpha = (110 * ease).toInt()
        }
        drawSegment(headDistance - tailLength, headDistance, tailPaint, canvas)

        val corePaint = Paint(paint).apply {
            shader = null
            color = WHITE
            strokeWidth = dp(2.5f)
            alpha = (220 * ease).toInt()
        }
        drawSegment(headDistance - tailLength * 0.35f, headDistance, corePaint, canvas)

        val pos = FloatArray(2)
        val head = ((headDistance % ringLength) + ringLength) % ringLength
        if (pathMeasure.getPosTan(head, pos, null)) {
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
            glow.color = accent
            glow.alpha = (70 * ease).toInt()
            canvas.drawCircle(pos[0], pos[1], dp(18f), glow)
            glow.alpha = (120 * ease).toInt()
            canvas.drawCircle(pos[0], pos[1], dp(9f), glow)
            glow.color = WHITE
            glow.alpha = (235 * ease).toInt()
            canvas.drawCircle(pos[0], pos[1], dp(3.4f), glow)
        }
    }

    /** 모서리에서 사방으로 뻗는 짧은 불꽃. */
    private fun drawCornerSparks(canvas: Canvas, accent: Int) {
        if (!style.sparks || sparkT <= 0f || sparkT >= 1f || ringLength <= 0f) return

        val t = sparkT
        val fade = (1f - t) * (1f - t)
        val sparkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        val corners = arrayOf(
            floatArrayOf(ringInset, ringInset, 225f),
            floatArrayOf(width - ringInset, ringInset, 315f),
            floatArrayOf(width - ringInset, height - ringInset, 45f),
            floatArrayOf(ringInset, height - ringInset, 135f)
        )
        val length = dp(16f) + dp(86f) * t

        for ((index, corner) in corners.withIndex()) {
            val cx = corner[0]
            val cy = corner[1]
            val base = corner[2]
            sparkPaint.color = if (index % 2 == 0) accent else WHITE
            sparkPaint.alpha = (190 * fade).toInt()
            sparkPaint.strokeWidth = dp(2.2f)
            for (i in -2..2) {
                val angle = Math.toRadians((base + i * 15f).toDouble())
                val cosA = cos(angle).toFloat()
                val sinA = sin(angle).toFloat()
                val ray = length * (1f - kotlin.math.abs(i) * 0.16f)
                canvas.drawLine(
                    cx + cosA * dp(5f),
                    cy + sinA * dp(5f),
                    cx + cosA * ray,
                    cy + sinA * ray,
                    sparkPaint
                )
            }
            sparkPaint.style = Paint.Style.FILL
            sparkPaint.alpha = (130 * fade).toInt()
            canvas.drawCircle(cx, cy, dp(2.5f) + dp(4f) * t, sparkPaint)
            sparkPaint.style = Paint.Style.STROKE
        }
    }

    /** 무선 충전 때 화면 아래에서 천천히 떠오르는 작은 빛. */
    private fun drawParticles(canvas: Canvas, accent: Int) {
        val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        for (i in 0 until 12) {
            val seed = i * 0.083f
            val p = ((particleClock + seed * 3.7f) % 1f + 1f) % 1f
            val x = width * (0.12f + ((i * 37) % 76) / 100f)
            val y = height - p * height * 0.62f
            val alpha = (sin(p * Math.PI).toFloat() * 110f).toInt()
            if (alpha <= 0) continue
            particlePaint.color = if (i % 3 == 0) WHITE else accent
            particlePaint.alpha = alpha
            particlePaint.setShadowLayer(0f, 0f, 0f, 0)
            val radius = dp(1.5f) + dp(1.2f) * ((i % 4) / 3f)
            val wobble = sin((particleClock * 2.0 + i) * Math.PI).toFloat() * dp(6f)
            canvas.drawCircle(x + wobble, y, radius, particlePaint)
        }
    }

    /**
     * 화면 아래에서 잔량만큼 차오르는 은은한 물결.
     *
     * 콘텐츠를 가리지 않도록 최대 높이를 화면의 32%(최대 260dp)로 제한하고,
     * 물 높이만 잔량에 비례시킨다. 물결선은 계속 흐른다.
     */
    private fun drawLiquid(canvas: Canvas, accent: Int) {
        if (!style.liquid || batteryLevel <= 0) return
        val band = min(height * 0.32f, dp(260f))
        val waterHeight = band * (batteryLevel / 100f)
        if (waterHeight < dp(2f)) return

        val topY = height - waterHeight
        val amplitude = dp(4f)
        val step = dp(6f).coerceAtLeast(1f)

        val fillPath = Path()
        fillPath.moveTo(0f, height.toFloat())
        fillPath.lineTo(0f, topY)
        var x = 0f
        while (x <= width.toFloat()) {
            val y = topY + sin(
                (x / width) * Math.PI * 3.0 + wavePhase * 2.0 * Math.PI
            ).toFloat() * amplitude
            fillPath.lineTo(x, y)
            x += step
        }
        fillPath.lineTo(width.toFloat(), height.toFloat())
        fillPath.close()

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f, topY, 0f, height.toFloat(),
                intArrayOf(withAlpha(accent, 44), withAlpha(accent, 12), 0x00000000),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(fillPath, fillPaint)

        val linePath = Path()
        var lineX = 0f
        var first = true
        while (lineX <= width.toFloat()) {
            val y = topY + sin(
                (lineX / width) * Math.PI * 3.0 + wavePhase * 2.0 * Math.PI
            ).toFloat() * amplitude
            if (first) {
                linePath.moveTo(lineX, y)
                first = false
            } else {
                linePath.lineTo(lineX, y)
            }
            lineX += step
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = dp(2.2f)
            strokeCap = Paint.Cap.ROUND
            color = accent
            alpha = 125
        }
        canvas.drawPath(linePath, linePaint)

        // 오른쪽 끝에서 빛나는 물방울
        val headPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        headPaint.color = accent
        headPaint.alpha = 65
        canvas.drawCircle(width.toFloat(), topY, dp(11f), headPaint)
        headPaint.color = WHITE
        headPaint.alpha = 205
        canvas.drawCircle(width.toFloat(), topY, dp(2.6f), headPaint)
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha.coerceIn(0, 255)) shl 24)

    /**
     * 무선 충전 코일은 기기 뒷면 중앙에 있으므로, 화면 중앙에서 퍼지는
     * 동심원으로 "충전 중"임을 표현한다. (기기별 정확한 좌표는 알 수 없다)
     */
    private fun drawCoilPulses(canvas: Canvas, cx: Float, cy: Float, accent: Int) {
        paint.shader = null
        paint.color = accent
        paint.strokeWidth = dp(3f)

        val base = min(width, height) * 0.10f
        val span = min(width, height) * 0.34f

        for (i in 0..1) {
            val p = (pulse + i * 0.5f) % 1f
            paint.alpha = ((1f - p) * 190f).toInt()
            canvas.drawCircle(cx, cy, base + span * p, paint)
        }
    }

    /**
     * 현재 사용할 색상 배열.
     *
     * 자동 모드면 잔량 팔레트, 사용자 지정 모드면 시작/중간/끝 색을 쓴다.
     * (첫 색을 마지막에 한 번 더 두어 회전 이음새를 없앤다)
     */
    private fun palette(): IntArray {
        if (style.autoColor) return paletteFor(batteryLevel)
        return intArrayOf(style.colorStart, style.colorMid, style.colorEnd, style.colorStart)
    }

    /**
     * SweepGradient 색상 배열.
     *
     * 가운데에 밝은 하이라이트를 넣어 회전이 눈에 확실히 보이게 한다.
     * 첫 색과 마지막 색을 같게 해 이음새를 없앤다.
     */
    private fun paletteFor(level: Int): IntArray = when {
        level <= 20 -> intArrayOf(RED, BRIGHT_RED, WHITE, RED, DEEP_RED, RED)
        level >= 80 -> intArrayOf(CYAN, GREEN, WHITE, CYAN, TEAL, CYAN)
        else -> intArrayOf(AMBER, ORANGE, WHITE, AMBER, DEEP_ORANGE, AMBER)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private companion object {
        const val WHITE = 0xFFFFFFFF.toInt()
        const val CYAN = 0xFF22D3EE.toInt()
        const val TEAL = 0xFF06B6D4.toInt()
        const val GREEN = 0xFF4ADE80.toInt()
        const val AMBER = 0xFFFBBF24.toInt()
        const val ORANGE = 0xFFF97316.toInt()
        const val DEEP_ORANGE = 0xFFC2410C.toInt()
        const val RED = 0xFFFF3B30.toInt()
        const val BRIGHT_RED = 0xFFFF8A80.toInt()
        const val DEEP_RED = 0xFFB91C1C.toInt()
    }
}

/**
 * 충전 시작 시 상단에서 펼쳐지는 다이나믹 아일랜드형 알약.
 *
 * `⚡ 47%` 와 `충전 중 · 32분 남음` 을 보여주고 4.6초 뒤 스스로 접힌다.
 * 터치를 받지 않으므로 다른 앱을 쓰는 중에도 방해되지 않는다.
 */
private class ChargingPillView(context: Context) : View(context) {

    private var level: Int = 0
    private var plug: PlugType = PlugType.NONE
    private var accent: Int = 0xFF22D3EE.toInt()
    private var status: String = ""

    private var reveal = 0f

    private val revealAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 420L
        interpolator = OvershootInterpolator(1.05f)
        addUpdateListener {
            reveal = it.animatedValue as Float
            invalidate()
        }
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
    }
    private val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val boltPath = Path()
    private val pillRect = RectF()
    private val pillPath = Path()

    fun update(level: Int, plug: PlugType, status: String, accent: Int) {
        this.level = level.coerceIn(0, 100)
        this.plug = plug
        this.status = status
        this.accent = accent
        invalidate()
    }

    fun play() {
        if (!revealAnimator.isStarted) revealAnimator.start()
    }

    override fun onDetachedFromWindow() {
        revealAnimator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val fullWidth = width.toFloat()
        val pillHeight = dp(56f)
        val visibleWidth = fullWidth * reveal.coerceIn(0f, 1f)
        if (visibleWidth < dp(6f) || height <= 0) return

        val centerY = height / 2f
        val left = (fullWidth - visibleWidth) / 2f
        val top = centerY - pillHeight / 2f
        val radius = pillHeight / 2f

        pillRect.set(left, top, left + visibleWidth, top + pillHeight)
        pillPath.reset()
        pillPath.addRoundRect(pillRect, radius, radius, Path.Direction.CW)

        // 반투명 배경 + 강조색 테두리
        fillPaint.color = 0xF2101620.toInt()
        canvas.drawRoundRect(pillRect, radius, radius, fillPaint)
        strokePaint.color = accent
        strokePaint.strokeWidth = dp(1.2f)
        strokePaint.alpha = (150 * reveal).toInt().coerceIn(0, 255)
        canvas.drawRoundRect(pillRect, radius, radius, strokePaint)

        canvas.save()
        canvas.clipPath(pillPath)

        val iconCx = left + dp(29f)
        fillPaint.color = accent
        fillPaint.alpha = (46 * reveal).toInt().coerceIn(0, 255)
        canvas.drawCircle(iconCx, centerY, dp(17f), fillPaint)

        drawBolt(canvas, iconCx, centerY, dp(17f), accent)

        val contentAlpha = ((reveal - 0.45f) / 0.55f).coerceIn(0f, 1f)
        val textX = left + dp(53f)
        textPaint.textSize = dp(17f)
        textPaint.color = 0xFFFFFFFF.toInt()
        textPaint.alpha = (255 * contentAlpha).toInt()
        canvas.drawText("$level%", textX, centerY - dp(1f), textPaint)

        statusPaint.textSize = dp(10.5f)
        statusPaint.color = 0xFF9AA6B8.toInt()
        statusPaint.alpha = (255 * contentAlpha).toInt()
        canvas.drawText(status, textX, centerY + dp(14f), statusPaint)

        canvas.restore()
    }

    private fun drawBolt(canvas: Canvas, cx: Float, cy: Float, size: Float, color: Int) {
        boltPath.reset()
        boltPath.moveTo(cx - size * 0.12f, cy - size * 0.52f)
        boltPath.lineTo(cx + size * 0.30f, cy - size * 0.06f)
        boltPath.lineTo(cx + size * 0.04f, cy - size * 0.06f)
        boltPath.lineTo(cx + size * 0.14f, cy + size * 0.52f)
        boltPath.lineTo(cx - size * 0.30f, cy + size * 0.02f)
        boltPath.lineTo(cx - size * 0.02f, cy + size * 0.02f)
        boltPath.close()
        fillPaint.color = color
        fillPaint.alpha = 255
        canvas.drawPath(boltPath, fillPaint)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
