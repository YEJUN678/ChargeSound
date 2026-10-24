package com.example.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.ad.AdManager
import com.example.ad.AdPolicy
import com.example.battery.BatteryInfo
import com.example.battery.BatteryInfoHelper
import com.example.battery.PlugType
import com.example.data.AnimationMode
import com.example.data.ChargeRecord
import com.example.data.ChargeSettings
import com.example.data.PremiumCatalog
import com.example.data.SettingsRepository
import com.example.data.SoundMode
import com.example.service.ChargingMonitorService
import com.example.sound.SoundManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val settings: StateFlow<ChargeSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChargeSettings()
    )

    /** 완료된 충전 기록 (통계 화면). 서버로 나가지 않고 기기에만 남습니다. */
    val chargeHistory: StateFlow<List<ChargeRecord>> = repository.historyFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val batteryInfo: StateFlow<BatteryInfo> = BatteryInfoHelper.batteryFlow(application).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BatteryInfoHelper.getBatteryInfo(application)
    )

    private val _isPlayingPreview = MutableStateFlow(false)
    val isPlayingPreview: StateFlow<Boolean> = _isPlayingPreview.asStateFlow()

    private val _activePlayingId = MutableStateFlow<String?>(null)
    val activePlayingId: StateFlow<String?> = _activePlayingId.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    /** 스낵바에 띄울 다국어 메시지를 만듭니다. */
    private fun msg(resId: Int, vararg args: Any): String =
        getApplication<Application>().getString(resId, *args)

    /** "다른 앱 위에 표시" 권한 상태. 설정 화면에서 돌아올 때마다 갱신합니다. */
    private val _overlayGranted = MutableStateFlow(false)
    val overlayGranted: StateFlow<Boolean> = _overlayGranted.asStateFlow()

    

    fun refreshOverlayPermission() {
        val context = getApplication<Application>()
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
        _overlayGranted.value = granted
        if (granted != settings.value.overlayPermissionGranted) {
            viewModelScope.launch {
                repository.setOverlayPermissionGranted(granted)
            }
        }
    }

    fun markOverlayPromptSeen() {
        viewModelScope.launch { repository.setOverlayPromptSeen(true) }
    }

    // ─────────────────────── 보상형 광고 ───────────────────────

    /** "광고 없이 N분" 카드에서 광고를 눌렀을 때. Activity가 없으면 무시합니다. */
    fun watchAdForAdFree(activity: Activity?) {
        val act = activity ?: return
        AdManager.showRewarded(
            activity = act,
            onReward = { startAdFreePeriod(AdPolicy.DEFAULT_AD_FREE_MINUTES) },
            onDismiss = { },
            onNotAvailable = {
                viewModelScope.launch {
                    _userMessage.value = msg(R.string.msg_ad_unavailable)
                }
            }
        )
    }

    /**
     * 특정 프리미엄 항목의 잠금을 광고로 해제합니다.
     *
     * 정책: 광고를 거부하거나 광고가 실패해도 사용자의 다른 선택은 막히지 않습니다.
     */
    fun watchAdToUnlock(activity: Activity?, premiumId: String) {
        val act = activity ?: return
        if (settings.value.isUnlocked(premiumId)) {
            viewModelScope.launch { _userMessage.value = msg(R.string.msg_already_unlocked) }
            return
        }
        // 보유한 무료 크레딧으로 먼저 해제를 시도합니다 (광고 불필요)
        viewModelScope.launch {
            if (repository.tryUnlockWithFreeCredit(premiumId)) {
                _userMessage.value = msg(R.string.msg_unlocked, displayNameOf(premiumId))
                SoundManager.ensurePremiumSound(getApplication(), premiumId)
                return@launch
            }
            with(act) {
                AdManager.showRewarded(
                    activity = this,
                    onReward = { onRewardEarned(premiumId) },
                    onDismiss = { },
                    onNotAvailable = {
                        _userMessage.value = msg(R.string.msg_ad_load_fail)
                    }
                )
            }
        }
    }

    /** 프리미엄 항목의 현재 언어 표시 이름 (스낵바 메시지용) */
    private fun displayNameOf(premiumId: String): String {
        val context = getApplication<Application>()
        PremiumCatalog.soundById(premiumId)?.let { return context.getString(it.nameRes) }
        PremiumCatalog.animationById(premiumId)?.let { return context.getString(it.nameRes) }
        return context.getString(R.string.premium_generic_name)
    }

    // ─────────────────────── 프리미엄 콘텐츠 ───────────────────────

    /**
     * 프리미엄 콘텐츠 ID의 해제 상태.
     * 이미 해제되었다면 true, 아니면 광고가 필요하므로 false를 반환합니다.
     */
    fun isPremiumUnlocked(premiumId: String): Boolean = settings.value.isUnlocked(premiumId)

    /**
     * 프리미엄 사운드를 선택합니다.
     *
     * 잠긴 콘텐츠면 광고를 거치지 않고 **무료 프리셋으로 되돌립니다** (정책 준수).
     * 광고를 강제로 보여주거나 기능을 막지 않습니다.
     */
    fun selectPresetSound(presetId: String) {
        viewModelScope.launch {
            val current = settings.value
            if (PremiumCatalog.isPremiumSoundId(presetId)) {
                val unlocked = current.isUnlocked(presetId)
                if (!unlocked) {
                    // 광고를 거부해도 사용할 수 있는 경로가 항상 존재해야 합니다.
                    repository.setPresetSoundId("cyber_chime")
                    val itemName = PremiumCatalog.soundById(presetId)?.nameRes
                        ?.let { getApplication<Application>().getString(it) }
                        ?: getApplication<Application>().getString(R.string.premium_generic_name)
                    _userMessage.value = msg(R.string.msg_locked_sound, itemName)
                    return@launch
                }
                // 해제된 사운드: WAV 파일이 필요하면 생성
                val file = SoundManager.ensurePremiumSound(getApplication(), presetId)
                if (file == null) {
                    repository.setPresetSoundId("cyber_chime")
                    _userMessage.value = msg(R.string.msg_premium_sound_fallback)
                    return@launch
                }
            }
            repository.setPresetSoundId(presetId)
            _userMessage.value = msg(R.string.msg_sound_applied)
            previewPlaySound(presetId)
        }
    }

    /**
     * 프리미엄 애니메이션 테마를 선택합니다.
     * 잠겨 있으면 기본 테마로 되돌립니다 (기능을 막지 않음).
     */
    fun selectAnimationMode(mode: AnimationMode) {
        viewModelScope.launch {
            val premiumId = com.example.data.PremiumAnimationModes.premiumIdOf(mode)
            if (premiumId != null) {
                if (!settings.value.isUnlocked(premiumId)) {
                    repository.setAnimationMode(AnimationMode.BUILTIN_LIGHTNING)
                    val nameRes = PremiumCatalog.animationById(premiumId)?.nameRes ?: mode.nameRes
                    val name = getApplication<Application>().getString(nameRes)
                    _userMessage.value = msg(R.string.msg_locked_anim, name)
                    return@launch
                }
            }
            repository.setAnimationMode(mode)
        }
    }

    /**
     * 보상형 광고 1회 시청분을 [premiumId]에 적립합니다.
     * [AdManager]가 광고를 **끝까지 시청했을 때만** 호출되어야 합니다.
     *
     * [SettingsRepository.addRewardCredit]가 완료 횟수에 도달하면 자동으로 잠금을 해제하고
     * 진행도를 초기화하므로, 이미 해제된 항목에 광고를 반복해서 볼 수 없습니다.
     */
    fun onRewardEarned(premiumId: String) {
        viewModelScope.launch {
            if (settings.value.isUnlocked(premiumId)) {
                _userMessage.value = msg(R.string.msg_already_unlocked)
                return@launch
            }
            val watchCount = repository.addRewardCredit(premiumId)
            val required = PremiumCatalog.ADS_REQUIRED_PER_UNLOCK
            val itemName = displayNameOf(premiumId)

            if (watchCount >= required) {
                // addRewardCredit가 잠금을 해제하고 진행도를 초기화했습니다.
                _userMessage.value = msg(R.string.msg_unlocked, itemName)
                SoundManager.ensurePremiumSound(getApplication(), premiumId)
            } else {
                _userMessage.value =
                    msg(R.string.msg_unlock_progress, itemName, watchCount, required)
            }
        }
    }

    /**
     * "광고 없이 N분" 기능을 시작합니다.
     * 광고를 완전히 끄는 유일한 합법적 경로입니다.
     */
    fun startAdFreePeriod(minutes: Int) {
        viewModelScope.launch {
            repository.startAdFreePeriod(minutes)
            _userMessage.value = msg(R.string.msg_adfree_started, minutes)
        }
    }

    fun adFreeRemainingMinutes(): Int = settings.value.adFreeRemainingMinutes(System.currentTimeMillis())

    fun isAdFreeActive(): Boolean =
        settings.value.adFreeUntil > System.currentTimeMillis()

    // ─────────────────────── 오디오 / 영상 파일 선택 ───────────────────────

    /**
     * 커스텀 오디오를 적용하고, 보상형 광고 *제안*을 띄웁니다.
     *
     * 정책: 파일 적용 자체는 광고와 무관하게 즉시 완료됩니다.
     * 광고는 사용자가 선택하는 추가 보상(무료 잠금 해제 크레딧)이며,
     * 거부해도 기능에 아무 영향이 없습니다.
     */
    fun selectCustomAudio(uri: Uri) {
        viewModelScope.launch {
            val result = repository.saveCustomAudio(uri)
            if (result.isSuccess) {
                _userMessage.value = msg(R.string.msg_custom_audio_applied)
                previewPlayCurrentSound()
                _offerFreeUnlockCredit.value = getApplication<Application>()
                    .getString(R.string.offer_label_custom_audio)
            } else {
                _userMessage.value = msg(
                    R.string.msg_audio_load_fail,
                    result.exceptionOrNull()?.message ?: ""
                )
            }
        }
    }

    /** 크스텀 영상 적용 후 보상형 광고 제안 트리거 */
    private val _offerFreeUnlockCredit = MutableStateFlow<String?>(null)
    val offerFreeUnlockCredit: StateFlow<String?> = _offerFreeUnlockCredit.asStateFlow()

    fun dismissFreeUnlockOffer() {
        _offerFreeUnlockCredit.value = null
    }

    /**
     * 보상형 광고를 시청하고 무료 잠금 해제 크레딧을 받습니다.
     * 광고를 거부하면 아무것도 일어나지 않습니다(기능에 영향 없음).
     */
    fun watchAdForFreeCredit(activity: Activity?) {
        val act = activity ?: run { dismissFreeUnlockOffer(); return }
        val label = _offerFreeUnlockCredit.value
        AdManager.showRewarded(
            activity = act,
            onReward = {
                viewModelScope.launch {
                    repository.grantFreeUnlockCredit()
                    _userMessage.value = msg(
                        R.string.msg_credit_gained,
                        repository.settingsFlow.first().freeUnlockCredits
                    )
                    _offerFreeUnlockCredit.value = null
                }
            },
            onDismiss = { _offerFreeUnlockCredit.value = null },
            onNotAvailable = {
                viewModelScope.launch {
                    _userMessage.value = msg(R.string.msg_ad_retry_later)
                    _offerFreeUnlockCredit.value = null
                }
            }
        )
    }

    fun toggleServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setServiceEnabled(enabled)
            val context = getApplication<Application>()
            if (enabled) {
                ChargingMonitorService.start(context)
                _userMessage.value = msg(R.string.msg_monitoring_started)
            } else {
                ChargingMonitorService.stop(context)
                _userMessage.value = msg(R.string.msg_monitoring_stopped)
            }
        }
    }

    fun toggleSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSoundEnabled(enabled)
        }
    }

    fun setVoiceAnnounce(enabled: Boolean) {
        viewModelScope.launch {
            repository.setVoiceAnnounceEnabled(enabled)
        }
    }

    fun setVolume(volume: Float) {
        viewModelScope.launch {
            repository.setVolume(volume)
        }
    }

    fun clearCustomAudio() {
        viewModelScope.launch {
            repository.clearCustomAudio()
            _userMessage.value = msg(R.string.msg_preset_restored)
        }
    }

    fun toggleAnimationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAnimationEnabled(enabled)
        }
    }

    fun setAnimationMode(mode: AnimationMode) {
        viewModelScope.launch {
            repository.setAnimationMode(mode)
        }
    }

    fun handleSelectedVideo(uri: Uri) {
        viewModelScope.launch {
            val result = repository.saveCustomVideo(uri)
            if (result.isSuccess) {
                _userMessage.value =
                    msg(R.string.msg_custom_video_registered, result.getOrNull() ?: "")
                _offerFreeUnlockCredit.value = getApplication<Application>()
                    .getString(R.string.offer_label_custom_video)
            } else {
                _userMessage.value = msg(
                    R.string.msg_video_load_fail,
                    result.exceptionOrNull()?.message ?: ""
                )
            }
        }
    }

    fun clearCustomVideo() {
        viewModelScope.launch {
            repository.clearCustomVideo()
            _userMessage.value = msg(R.string.msg_anim_restored)
        }
    }

    fun setVideoDuration(seconds: Int) {
        viewModelScope.launch {
            repository.setVideoDuration(seconds)
        }
    }

    fun setVideoLoop(loop: Boolean) {
        viewModelScope.launch {
            repository.setVideoLoop(loop)
        }
    }

    fun toggleVibration(enabled: Boolean) {
        viewModelScope.launch {
            repository.setVibrationEnabled(enabled)
        }
    }

    // ─────────────────────── 충전 알람 ───────────────────────

    fun setAlarmEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAlarmEnabled(enabled)
            _userMessage.value = msg(
                if (enabled) R.string.msg_alarm_on else R.string.msg_alarm_off
            )
        }
    }

    fun setAlarmTargets(targets: List<Int>) {
        viewModelScope.launch { repository.setAlarmTargets(targets) }
    }

    /** 알람 임계값 하나를 토글합니다. 목록이 비면 아무것도 하지 않습니다. */
    fun toggleAlarmTarget(percent: Int) {
        val current = settings.value.alarmTargets.toMutableSet()
        if (!current.remove(percent)) current.add(percent)
        // 최소 1개는 남겨야 합니다 — 0개면 알람을 켜도 아무 일도 일어나지 않습니다.
        if (current.isEmpty()) return
        setAlarmTargets(current.toList())
    }

    fun setAlarmSnoozeMinutes(minutes: Int) {
        viewModelScope.launch { repository.setAlarmSnoozeMinutes(minutes) }
    }

    fun setChargeLimitEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setChargeLimitEnabled(enabled) }
    }

    fun setChargeLimitPercent(percent: Int) {
        viewModelScope.launch { repository.setChargeLimitPercent(percent) }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch { repository.setFontScale(scale) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.setDynamicColor(enabled) }
    }

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch { repository.setHighContrast(enabled) }
    }

    /** 커스텀 오디오 재생 구간 저장 (0L = 전체) */
    fun setCustomSoundRange(startMs: Long, endMs: Long) {
        viewModelScope.launch { repository.setCustomSoundRange(startMs, endMs) }
    }

    /** 커스텀 영상 재생 구간 저장 (0L = 전체) */
    fun setCustomVideoRange(startMs: Long, endMs: Long) {
        viewModelScope.launch { repository.setCustomVideoRange(startMs, endMs) }
    }

    fun clearChargeHistory() {
        viewModelScope.launch { repository.writeHistory(emptyList()) }
    }

    fun setAlarmWithSound(enabled: Boolean) {
        viewModelScope.launch { repository.setAlarmWithSound(enabled) }
    }

    // ─────────────────────── 배터리 경고 ───────────────────────

    fun setTempAlertEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setTempAlertEnabled(enabled) }
    }

    fun setTempAlertThreshold(celsius: Float) {
        viewModelScope.launch { repository.setTempAlertThreshold(celsius) }
    }

    fun setHealthAlertEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setHealthAlertEnabled(enabled) }
    }

    // ─────────────────────── 잠금 화면 ───────────────────────

    fun setShowOnLockScreen(enabled: Boolean) {
        viewModelScope.launch { repository.setShowOnLockScreen(enabled) }
    }

    // ─────────────────────── 충전 방식별 사운드 ───────────────────────

    fun setPlugSpecificSounds(enabled: Boolean) {
        viewModelScope.launch { repository.setPlugSpecificSounds(enabled) }
    }

    fun setPlugPreset(plugType: PlugType, presetId: String) {
        viewModelScope.launch { repository.setPlugPreset(plugType, presetId) }
    }

    // ─────────────────────── 저전력 · 급방전 ───────────────────────

    fun setLowBatteryAlertEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setLowBatteryAlertEnabled(enabled) }
    }

    fun setLowBatteryThreshold(percent: Int) {
        viewModelScope.launch { repository.setLowBatteryThreshold(percent) }
    }

    fun setRapidDischargeAlertEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setRapidDischargeAlertEnabled(enabled) }
    }

    fun setRapidDischargeWindow(minutes: Int) {
        viewModelScope.launch { repository.setRapidDischargeWindow(minutes) }
    }

    fun setRapidDischargeDrop(percent: Int) {
        viewModelScope.launch { repository.setRapidDischargeDrop(percent) }
    }

    // ─────────────────────── 조용한 시간 ───────────────────────

    fun setAccentKey(key: String) {
        viewModelScope.launch { repository.setAccentKey(key) }
    }

    fun setQuietHoursEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setQuietHoursEnabled(enabled) }
    }

    fun setQuietHours(startMinute: Int, endMinute: Int) {
        viewModelScope.launch { repository.setQuietHours(startMinute, endMinute) }
    }

    fun previewPlaySound(presetId: String? = null) {
        val context = getApplication<Application>()
        val currentSettings = settings.value
        // 미리듣기는 앱 핵심 기능이므로 전면류 광고를 차단합니다.
        AdManager.setCoreFeatureBusy(true)

        val targetPath = if (presetId != null) {
            SoundManager.getPresetFile(context, presetId).absolutePath
        } else {
            when (currentSettings.soundMode) {
                SoundMode.CUSTOM -> currentSettings.customSoundPath
                SoundMode.PRESET -> SoundManager.getPresetFile(context, currentSettings.presetSoundId).absolutePath
            }
        }

        _isPlayingPreview.value = true
        _activePlayingId.value = presetId ?: (if (currentSettings.soundMode == SoundMode.CUSTOM) "custom" else currentSettings.presetSoundId)

        SoundManager.playSound(
            context = context,
            filePath = targetPath,
            volume = currentSettings.volume,
            durationSeconds = currentSettings.videoDurationSeconds,
            // 구간 선택은 커스텀 오디오에만 의미가 있습니다.
            // 프리셋은 이미 원하는 길이로 합성되어 있습니다.
            startMs = if (currentSettings.soundMode == SoundMode.CUSTOM) currentSettings.customSoundStartMs else 0L,
            endMs = if (currentSettings.soundMode == SoundMode.CUSTOM) currentSettings.customSoundEndMs else 0L,
            onCompletion = {
                _isPlayingPreview.value = false
                _activePlayingId.value = null
                AdManager.setCoreFeatureBusy(false)
            }
        )
    }

    fun previewPlayCurrentSound() {
        previewPlaySound(null)
    }

    fun stopPreviewSound() {
        SoundManager.fadeOutAndStop(300L)
        _isPlayingPreview.value = false
        _activePlayingId.value = null
        AdManager.setCoreFeatureBusy(false)
    }

    fun simulateCharging(context: Context) {
        val currentSettings = settings.value
        val battery = batteryInfo.value
        AdManager.setCoreFeatureBusy(true)

        // Haptic feedback
        if (currentSettings.vibrationEnabled) {
            triggerVibration(context)
        }

        // Sound with duration sync and fade-out
        if (currentSettings.soundEnabled) {
            val soundPath = when (currentSettings.soundMode) {
                SoundMode.CUSTOM -> currentSettings.customSoundPath
                SoundMode.PRESET -> SoundManager.getPresetFile(context, currentSettings.presetSoundId).absolutePath
            }
            SoundManager.playSound(
                context = context,
                filePath = soundPath,
                volume = currentSettings.volume,
                durationSeconds = currentSettings.videoDurationSeconds,
                startMs = if (currentSettings.soundMode == SoundMode.CUSTOM) currentSettings.customSoundStartMs else 0L,
                endMs = if (currentSettings.soundMode == SoundMode.CUSTOM) currentSettings.customSoundEndMs else 0L
            )
        }

        // Animation
        val intent = Intent(context, ChargingAnimationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(ChargingAnimationActivity.EXTRA_BATTERY_LEVEL, battery.level)
            putExtra(ChargingAnimationActivity.EXTRA_PLUG_NAME, msg(R.string.plug_test_sim))
            putExtra(ChargingAnimationActivity.EXTRA_ANIM_MODE, currentSettings.animationMode.name)
            putExtra(ChargingAnimationActivity.EXTRA_VIDEO_PATH, currentSettings.customVideoPath)
            putExtra(ChargingAnimationActivity.EXTRA_DURATION_SEC, currentSettings.videoDurationSeconds)
            putExtra(ChargingAnimationActivity.EXTRA_LOOP, currentSettings.videoLoop)
            putExtra(ChargingAnimationActivity.EXTRA_VIDEO_START_MS, currentSettings.customVideoStartMs)
            putExtra(ChargingAnimationActivity.EXTRA_VIDEO_END_MS, currentSettings.customVideoEndMs)
        }
        context.startActivity(intent)
    }

    fun previewFullScreenAnimation(context: Context) {
        val currentSettings = settings.value
        val battery = batteryInfo.value
        AdManager.setCoreFeatureBusy(true)

        val intent = Intent(context, ChargingAnimationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(ChargingAnimationActivity.EXTRA_BATTERY_LEVEL, battery.level)
            putExtra(ChargingAnimationActivity.EXTRA_PLUG_NAME, msg(R.string.plug_preview))
            putExtra(ChargingAnimationActivity.EXTRA_ANIM_MODE, currentSettings.animationMode.name)
            putExtra(ChargingAnimationActivity.EXTRA_VIDEO_PATH, currentSettings.customVideoPath)
            putExtra(ChargingAnimationActivity.EXTRA_DURATION_SEC, currentSettings.videoDurationSeconds)
            putExtra(ChargingAnimationActivity.EXTRA_LOOP, currentSettings.videoLoop)
            putExtra(ChargingAnimationActivity.EXTRA_VIDEO_START_MS, currentSettings.customVideoStartMs)
            putExtra(ChargingAnimationActivity.EXTRA_VIDEO_END_MS, currentSettings.customVideoEndMs)
        }
        context.startActivity(intent)
    }

    /** "다른 앱 위에 표시" 권한 요청 인텐트 */
    fun overlaySettingsIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${getApplication<Application>().packageName}")
        )

    private fun triggerVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val effect = VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 150), intArrayOf(0, 180, 0, 255), -1)
                mgr?.defaultVibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 150), -1)
                    vib?.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vib?.vibrate(200)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration error
        }
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        SoundManager.stopSound()
    }
}
