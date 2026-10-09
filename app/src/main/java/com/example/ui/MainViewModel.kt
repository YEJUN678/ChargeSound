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
import com.example.ad.AdManager
import com.example.ad.AdPolicy
import com.example.battery.BatteryInfo
import com.example.battery.BatteryInfoHelper
import com.example.data.AnimationMode
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val settings: StateFlow<ChargeSettings> = repository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChargeSettings()
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
                    _userMessage.value = "지금은 광고를 불러올 수 없습니다. 잠시 후 다시 시도해 주세요."
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
            viewModelScope.launch { _userMessage.value = "이미 해제된 콘텐츠입니다." }
            return
        }
        AdManager.showRewarded(
            activity = act,
            onReward = { onRewardEarned(premiumId) },
            onDismiss = { },
            onNotAvailable = {
                viewModelScope.launch {
                    val name = PremiumCatalog.soundById(premiumId)?.name
                        ?: PremiumCatalog.animationById(premiumId)?.name
                        ?: "해당 콘텐츠"
                    _userMessage.value = "광고를 불러오지 못했습니다. 다른 프리셋은 광고 없이 사용하실 수 있습니다."
                }
            }
        )
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
                    _userMessage.value = "'${PremiumCatalog.soundById(presetId)?.name}'은 광고 시청으로 잠금을 해제할 수 있습니다."
                    return@launch
                }
                // 해제된 사운드: WAV 파일이 필요하면 생성
                val file = SoundManager.ensurePremiumSound(getApplication(), presetId)
                if (file == null) {
                    repository.setPresetSoundId("cyber_chime")
                    _userMessage.value = "프리미엄 사운드를 불러오지 못해 기본 사운드로 돌아갑니다."
                    return@launch
                }
            }
            repository.setPresetSoundId(presetId)
            _userMessage.value = "효과음이 적용되었습니다."
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
                    val name = PremiumCatalog.animationById(premiumId)?.name ?: mode.displayName
                    _userMessage.value = "'$name' 테마는 광고 시청으로 잠금을 해제할 수 있습니다."
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
                _userMessage.value = "이미 해제된 콘텐츠입니다."
                return@launch
            }
            val watchCount = repository.addRewardCredit(premiumId)
            val required = PremiumCatalog.ADS_REQUIRED_PER_UNLOCK
            val itemName = PremiumCatalog.soundById(premiumId)?.name
                ?: PremiumCatalog.animationById(premiumId)?.name
                ?: "프리미엄 콘텐츠"

            if (watchCount >= required) {
                // addRewardCredit가 잠금을 해제하고 진행도를 초기화했습니다.
                _userMessage.value = "'$itemName' 잠금이 해제되었습니다!"
                SoundManager.ensurePremiumSound(getApplication(), premiumId)
            } else {
                _userMessage.value = "'$itemName' 해제 진행도 $watchCount/$required"
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
            _userMessage.value = "${minutes}분 동안 광고 없이 사용할 수 있습니다."
        }
    }

    fun adFreeRemainingMinutes(): Int = settings.value.adFreeRemainingMinutes(System.currentTimeMillis())

    fun isAdFreeActive(): Boolean =
        settings.value.adFreeUntil > System.currentTimeMillis()

    // ─────────────────────── 오디오 / 영상 파일 선택 (광고 강제 없음) ───────────────────────

    fun selectCustomAudio(uri: Uri) {
        viewModelScope.launch {
            val result = repository.saveCustomAudio(uri)
            if (result.isSuccess) {
                _userMessage.value = "커스텀 MP3가 적용되었습니다."
                previewPlayCurrentSound()
            } else {
                _userMessage.value = "오디오 파일을 불러오지 못했습니다: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun toggleServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setServiceEnabled(enabled)
            val context = getApplication<Application>()
            if (enabled) {
                ChargingMonitorService.start(context)
                _userMessage.value = "충전 감지 모니터링이 시작되었습니다."
            } else {
                ChargingMonitorService.stop(context)
                _userMessage.value = "충전 감지 모니터링이 중지되었습니다."
            }
        }
    }

    fun toggleSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSoundEnabled(enabled)
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
            _userMessage.value = "기본 프리셋 사운드로 변경되었습니다."
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
                _userMessage.value = "커스텀 영상이 등록되었습니다: ${result.getOrNull()}"
            } else {
                _userMessage.value = "비디오 파일을 불러오지 못했습니다: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun clearCustomVideo() {
        viewModelScope.launch {
            repository.clearCustomVideo()
            _userMessage.value = "기본 애니메이션으로 복원되었습니다."
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
                durationSeconds = currentSettings.videoDurationSeconds
            )
        }

        // Animation
        val intent = Intent(context, ChargingAnimationActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(ChargingAnimationActivity.EXTRA_BATTERY_LEVEL, battery.level)
            putExtra(ChargingAnimationActivity.EXTRA_PLUG_NAME, "테스트 시뮬레이션 충전 중")
            putExtra(ChargingAnimationActivity.EXTRA_ANIM_MODE, currentSettings.animationMode.name)
            putExtra(ChargingAnimationActivity.EXTRA_VIDEO_PATH, currentSettings.customVideoPath)
            putExtra(ChargingAnimationActivity.EXTRA_DURATION_SEC, currentSettings.videoDurationSeconds)
            putExtra(ChargingAnimationActivity.EXTRA_LOOP, currentSettings.videoLoop)
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
            putExtra(ChargingAnimationActivity.EXTRA_PLUG_NAME, "전체화면 미리보기")
            putExtra(ChargingAnimationActivity.EXTRA_ANIM_MODE, currentSettings.animationMode.name)
            putExtra(ChargingAnimationActivity.EXTRA_VIDEO_PATH, currentSettings.customVideoPath)
            putExtra(ChargingAnimationActivity.EXTRA_DURATION_SEC, currentSettings.videoDurationSeconds)
            putExtra(ChargingAnimationActivity.EXTRA_LOOP, currentSettings.videoLoop)
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
