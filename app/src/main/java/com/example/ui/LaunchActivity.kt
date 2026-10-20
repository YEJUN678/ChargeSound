package com.example.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.data.SettingsRepository
import kotlinx.coroutines.launch

/**
 * 앱 진입점 — 온보딩 완료 여부에 따라 분기합니다.
 *
 * 이 액티비티는 UI 가 없습니다. 테마만 `android:theme="@style/Theme.Splash"`
 * 로 지정해 로딩 느낌을 주고, 즉시 온보딩 또는 메인으로 보냅니다.
 */
class LaunchActivity : AppCompatActivity() {

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