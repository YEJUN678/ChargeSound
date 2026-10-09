package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileOutputStream

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "charge_settings")

class SettingsRepository(private val context: Context) {
    companion object {
        private const val TAG = "SettingsRepository"

        val KEY_SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val KEY_SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_VOLUME = floatPreferencesKey("volume")
        val KEY_SOUND_MODE = stringPreferencesKey("sound_mode")
        val KEY_PRESET_SOUND_ID = stringPreferencesKey("preset_sound_id")
        val KEY_CUSTOM_SOUND_NAME = stringPreferencesKey("custom_sound_name")
        val KEY_CUSTOM_SOUND_PATH = stringPreferencesKey("custom_sound_path")
        val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")

        val KEY_ANIMATION_ENABLED = booleanPreferencesKey("animation_enabled")
        val KEY_ANIMATION_MODE = stringPreferencesKey("animation_mode")
        val KEY_CUSTOM_VIDEO_NAME = stringPreferencesKey("custom_video_name")
        val KEY_CUSTOM_VIDEO_PATH = stringPreferencesKey("custom_video_path")
        val KEY_VIDEO_DURATION = intPreferencesKey("video_duration")
        val KEY_VIDEO_LOOP = booleanPreferencesKey("video_loop")
        val KEY_PLAY_ON_DISCONNECT = booleanPreferencesKey("play_on_disconnect")

        val KEY_UNLOCKED_PREMIUM = stringSetPreferencesKey("unlocked_premium")
        val KEY_REWARD_PROGRESS = stringPreferencesKey("reward_progress")
        val KEY_AD_FREE_UNTIL = longPreferencesKey("ad_free_until")
        val KEY_OVERLAY_GRANTED = booleanPreferencesKey("overlay_granted")
        val KEY_OVERLAY_PROMPT_SEEN = booleanPreferencesKey("overlay_prompt_seen")
        val KEY_PREMIUM_UNLOCKED = booleanPreferencesKey("premium_unlocked")
        val KEY_FREE_UNLOCK_CREDITS = intPreferencesKey("free_unlock_credits")
    }

    /** "id:count,id:count" 형태의 직렬화 */
    private fun encodeProgress(map: Map<String, Int>): String =
        map.entries.joinToString(",") { "${it.key}:${it.value}" }

    private fun decodeProgress(raw: String?): Map<String, Int> {
        if (raw.isNullOrBlank()) return emptyMap()
        return raw.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size != 2) return@mapNotNull null
            val count = parts[1].toIntOrNull() ?: return@mapNotNull null
            parts[0] to count
        }.toMap()
    }

    val settingsFlow: Flow<ChargeSettings> = context.dataStore.data.map { prefs ->
        val soundModeStr = prefs[KEY_SOUND_MODE] ?: SoundMode.PRESET.name
        val soundMode = try {
            SoundMode.valueOf(soundModeStr)
        } catch (e: Exception) {
            SoundMode.PRESET
        }

        val animModeStr = prefs[KEY_ANIMATION_MODE] ?: AnimationMode.BUILTIN_LIGHTNING.name
        val animMode = try {
            AnimationMode.valueOf(animModeStr)
        } catch (e: Exception) {
            AnimationMode.BUILTIN_LIGHTNING
        }

        ChargeSettings(
            isServiceEnabled = prefs[KEY_SERVICE_ENABLED] ?: true,
            soundEnabled = prefs[KEY_SOUND_ENABLED] ?: true,
            volume = prefs[KEY_VOLUME] ?: 0.85f,
            soundMode = soundMode,
            presetSoundId = prefs[KEY_PRESET_SOUND_ID] ?: "cyber_chime",
            customSoundName = prefs[KEY_CUSTOM_SOUND_NAME],
            customSoundPath = prefs[KEY_CUSTOM_SOUND_PATH],
            vibrationEnabled = prefs[KEY_VIBRATION_ENABLED] ?: true,
            animationEnabled = prefs[KEY_ANIMATION_ENABLED] ?: true,
            animationMode = animMode,
            customVideoName = prefs[KEY_CUSTOM_VIDEO_NAME],
            customVideoPath = prefs[KEY_CUSTOM_VIDEO_PATH],
            videoDurationSeconds = prefs[KEY_VIDEO_DURATION] ?: 5,
            videoLoop = prefs[KEY_VIDEO_LOOP] ?: true,
            playOnDisconnect = prefs[KEY_PLAY_ON_DISCONNECT] ?: false,
            unlockedPremiumIds = prefs[KEY_UNLOCKED_PREMIUM] ?: emptySet(),
            rewardProgress = decodeProgress(prefs[KEY_REWARD_PROGRESS]),
            adFreeUntil = prefs[KEY_AD_FREE_UNTIL] ?: 0L,
            overlayPermissionGranted = prefs[KEY_OVERLAY_GRANTED] ?: false,
            overlayPromptSeen = prefs[KEY_OVERLAY_PROMPT_SEEN] ?: false,
            premiumUnlocked = prefs[KEY_PREMIUM_UNLOCKED] ?: false,
            freeUnlockCredits = prefs[KEY_FREE_UNLOCK_CREDITS] ?: 0
        )
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SERVICE_ENABLED] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SOUND_ENABLED] = enabled }
    }

    suspend fun setVolume(volume: Float) {
        context.dataStore.edit { it[KEY_VOLUME] = volume.coerceIn(0f, 1f) }
    }

    suspend fun setSoundMode(mode: SoundMode) {
        context.dataStore.edit { it[KEY_SOUND_MODE] = mode.name }
    }

    suspend fun setPresetSoundId(id: String) {
        context.dataStore.edit {
            it[KEY_PRESET_SOUND_ID] = id
            it[KEY_SOUND_MODE] = SoundMode.PRESET.name
        }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATION_ENABLED] = enabled }
    }

    suspend fun setAnimationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ANIMATION_ENABLED] = enabled }
    }

    suspend fun setAnimationMode(mode: AnimationMode) {
        context.dataStore.edit { it[KEY_ANIMATION_MODE] = mode.name }
    }

    suspend fun setVideoDuration(seconds: Int) {
        context.dataStore.edit { it[KEY_VIDEO_DURATION] = seconds }
    }

    suspend fun setVideoLoop(loop: Boolean) {
        context.dataStore.edit { it[KEY_VIDEO_LOOP] = loop }
    }

    suspend fun setPlayOnDisconnect(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PLAY_ON_DISCONNECT] = enabled }
    }

    suspend fun setUnlockedPremiumIds(ids: Set<String>) {
        context.dataStore.edit { it[KEY_UNLOCKED_PREMIUM] = ids }
    }

    suspend fun setRewardProgress(progress: Map<String, Int>) {
        context.dataStore.edit {
            if (progress.isEmpty()) it.remove(KEY_REWARD_PROGRESS)
            else it[KEY_REWARD_PROGRESS] = encodeProgress(progress)
        }
    }

    suspend fun setAdFreeUntil(epochMillis: Long) {
        context.dataStore.edit { it[KEY_AD_FREE_UNTIL] = epochMillis }
    }

    suspend fun setOverlayPermissionGranted(granted: Boolean) {
        context.dataStore.edit { it[KEY_OVERLAY_GRANTED] = granted }
    }

    suspend fun setOverlayPromptSeen(seen: Boolean) {
        context.dataStore.edit { it[KEY_OVERLAY_PROMPT_SEEN] = seen }
    }

    suspend fun setPremiumUnlocked(unlocked: Boolean) {
        context.dataStore.edit { it[KEY_PREMIUM_UNLOCKED] = unlocked }
    }

    /**
     * 보상형 광고 1회 시청분(1 credit)을 적립합니다.
     *
     * 정책: 광고가 미노출(fill 실패)되어도 기능이 막히지 않아야 하므로,
     * 이 함수는 **광고를 끝까지 시청했을 때만** 호출됩니다.
     *
     * 누적 [KEY_FREE_UNLOCK_CREDITS] 크레딧은 광고 1회와 동일한 가치로 취급하며,
     * 잠금 해제 시 1개 소비됩니다.
     *
     * 완료 횟수에 도달하면 잠금을 해제하고 진행도를 초기화합니다.
     * 초기화하지 않으면 이미 해제된 항목을 무한정 반복 시청할 수 있어
     * 보상형 광고 정책 위반이 됩니다.
     *
     * @return 적립 직후의 누적 시청 횟수 (해제 완료 시 required 값으로 보고됨)
     */
    suspend fun addRewardCredit(premiumId: String): Int {
        var watchCount = 0
        val required = PremiumCatalog.ADS_REQUIRED_PER_UNLOCK
        context.dataStore.edit { prefs ->
            if ((prefs[KEY_UNLOCKED_PREMIUM] ?: emptySet()).contains(premiumId)) return@edit
            val current = decodeProgress(prefs[KEY_REWARD_PROGRESS])
            val next = (current[premiumId] ?: 0) + 1
            watchCount = next

            val freeCredits = prefs[KEY_FREE_UNLOCK_CREDITS] ?: 0
            if (next + freeCredits >= required) {
                prefs[KEY_UNLOCKED_PREMIUM] = (prefs[KEY_UNLOCKED_PREMIUM] ?: emptySet()) + premiumId
                prefs[KEY_PREMIUM_UNLOCKED] = true
                // 광고 시청분을 소모했으므로 무료 크레딧은 그대로 두고 진행도만 초기화
                val remaining = current.toMutableMap()
                remaining.remove(premiumId)
                if (remaining.isEmpty()) prefs.remove(KEY_REWARD_PROGRESS)
                else prefs[KEY_REWARD_PROGRESS] = encodeProgress(remaining)
                watchCount = required
            } else {
                prefs[KEY_REWARD_PROGRESS] = encodeProgress(current + (premiumId to next))
            }
        }
        return watchCount
    }

    /**
     * 무료 크레딧으로 즉시 잠금을 해제합니다.
     *
     * 누적 광고 시청 횟수 + 보유 크레딧이 필요한 횟수 이상이면 광고를 보지 않고
     * 바로 해제합니다. 크레딧은 1개 소비됩니다.
     *
     * @return 해제되었으면 true, 크레딧이 부족하면 false (광고를 보여줘야 함)
     */
    suspend fun tryUnlockWithFreeCredit(premiumId: String): Boolean {
        var unlocked = false
        val required = PremiumCatalog.ADS_REQUIRED_PER_UNLOCK
        context.dataStore.edit { prefs ->
            if ((prefs[KEY_UNLOCKED_PREMIUM] ?: emptySet()).contains(premiumId)) {
                unlocked = true
                return@edit
            }
            val freeCredits = prefs[KEY_FREE_UNLOCK_CREDITS] ?: 0
            val current = decodeProgress(prefs[KEY_REWARD_PROGRESS])
            val seen = current[premiumId] ?: 0
            if (freeCredits <= 0 || seen + freeCredits < required) return@edit

            prefs[KEY_UNLOCKED_PREMIUM] = (prefs[KEY_UNLOCKED_PREMIUM] ?: emptySet()) + premiumId
            prefs[KEY_PREMIUM_UNLOCKED] = true
            prefs[KEY_FREE_UNLOCK_CREDITS] = freeCredits - 1
            val remaining = current.toMutableMap()
            remaining.remove(premiumId)
            if (remaining.isEmpty()) prefs.remove(KEY_REWARD_PROGRESS)
            else prefs[KEY_REWARD_PROGRESS] = encodeProgress(remaining)
            unlocked = true
        }
        return unlocked
    }

    /**
     * 무료 잠금 해제 크레딧을 1개 지급합니다.
     * 커스텀 오디오/영상 적용 후 보상형 광고를 시청했을 때 호출됩니다.
     */
    suspend fun grantFreeUnlockCredit() {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_FREE_UNLOCK_CREDITS] ?: 0
            // 무한 축적을 막기 위해 상한을 둡니다
            prefs[KEY_FREE_UNLOCK_CREDITS] = (current + 1)
                .coerceAtMost(PremiumCatalog.ADS_REQUIRED_PER_UNLOCK)
        }
    }

    /**
     * 광고를 N분 동안 숨깁니다 (배너/네이티브 비표시).
     */
    suspend fun startAdFreePeriod(minutes: Int) {
        val until = System.currentTimeMillis() + minutes * 60_000L
        context.dataStore.edit { it[KEY_AD_FREE_UNTIL] = until }
    }

    suspend fun saveCustomAudio(uri: Uri): Result<String> {
        return try {
            val fileName = queryFileName(uri) ?: "custom_audio.mp3"
            val extension = fileName.substringAfterLast('.', "mp3")
            val targetFile = File(context.filesDir, "custom_charge_sound.$extension")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(Exception("Cannot read audio URI"))

            context.dataStore.edit {
                it[KEY_CUSTOM_SOUND_NAME] = fileName
                it[KEY_CUSTOM_SOUND_PATH] = targetFile.absolutePath
                it[KEY_SOUND_MODE] = SoundMode.CUSTOM.name
            }
            Result.success(fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom audio", e)
            Result.failure(e)
        }
    }

    suspend fun clearCustomAudio() {
        context.dataStore.edit {
            it.remove(KEY_CUSTOM_SOUND_NAME)
            it.remove(KEY_CUSTOM_SOUND_PATH)
            it[KEY_SOUND_MODE] = SoundMode.PRESET.name
        }
    }

    suspend fun saveCustomVideo(uri: Uri): Result<String> {
        return try {
            val fileName = queryFileName(uri) ?: "custom_video.mp4"
            val extension = fileName.substringAfterLast('.', "mp4")
            val targetFile = File(context.filesDir, "custom_charge_anim.$extension")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(Exception("Cannot read video URI"))

            context.dataStore.edit {
                it[KEY_CUSTOM_VIDEO_NAME] = fileName
                it[KEY_CUSTOM_VIDEO_PATH] = targetFile.absolutePath
                it[KEY_ANIMATION_MODE] = AnimationMode.CUSTOM_VIDEO.name
            }
            Result.success(fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom video", e)
            Result.failure(e)
        }
    }

    suspend fun clearCustomVideo() {
        context.dataStore.edit {
            it.remove(KEY_CUSTOM_VIDEO_NAME)
            it.remove(KEY_CUSTOM_VIDEO_PATH)
            it[KEY_ANIMATION_MODE] = AnimationMode.BUILTIN_LIGHTNING.name
        }
    }

    private fun queryFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to query file name for URI: $uri", e)
            }
        }
        return name ?: uri.lastPathSegment
    }
}
