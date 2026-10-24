package com.example.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.data.SettingsRepository
import kotlinx.coroutines.launch

/**
 * 앱 진입점 — 온보딩 완료 여부에 따라 분기합니다.
 *
 * 이 액티비티는 UI 가 없습니다. 테마만 `android:theme="@style/Theme.Splash"`
 * 로 지정해 로딩 느낌을 주고, 즉시 온보딩 또는 메인으로 보냅니다.
 *
 * ## AppCompatActivity 가 아니라 ComponentActivity 인 이유
 * `Theme.Splash` 는 `android:Theme.DeviceDefault.NoActionBar` 를 부모로 쓰는
 * 프레임워크 테마입니다. AppCompatActivity 는 AppCompat 계열 테마를 강제하므로
 * 이 조합에서는 onCreate 직후 아래 예외로 앱이 죽습니다.
 *
 *   IllegalStateException: You need to use a Theme.AppCompat theme
 *   (or descendant) with this activity.
 *
 * Compose 앱이라 AppCompat 이 필요 없어서 ComponentActivity 로 내렸고,
 * appcompat 의존성도 제거했습니다.
 */
class LaunchActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val repo = SettingsRepository(this)
        lifecycleScope.launch {
            val intent = if (repo.onboardingDone()) {
                Intent(this@LaunchActivity, MainActivity::class.java)
            } else {
                Intent(this@LaunchActivity, OnboardingActivity::class.java)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }
}