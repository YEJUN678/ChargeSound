package com.example

import androidx.appcompat.app.AppCompatActivity
import com.example.ui.LaunchActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 실행 스모크 테스트 — "앱이 뜨는가"만 확인합니다.
 *
 * ## 왜 이 테스트가 필요한가
 * v1.6.0(vc6) 은 `LaunchActivity` 가 AppCompat 테마를 요구하는데 프레임워크 테마를
 * 쓰고 있어서 **켜자마자 죽었고**, 그 상태로 스토어에 올라갔습니다. 컴파일은
 * 통과했기 때문에 빌드만으로는 잡히지 않습니다.
 *
 * 여기서는 Robolectric 으로 기기 없이 LaunchActivity 를 실제로 생성합니다.
 * 테마 불일치, 매니페스트 누락, Application 초기화 예외처럼 "실행 직후" 나는
 * 문제는 이 한 줄에서 걸립니다.
 *
 * ## 범위
 * UI 동작/화면 내용은 검사하지 않습니다. 그건 계측 테스트나 Compose 테스트의
 * 몫이고, 여기서는 **크래시 없이 액티비티가 살아 있는지**만 봅니다.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LaunchSmokeTest {

    @Test
    fun launchActivity_startsWithoutCrashing() {
        // onCreate 안에서 테마/초기화 예외가 나면 여기서 바로 실패합니다.
        // (v1.6.0(vc6)의 "Theme.AppCompat theme을 써야 합니다" 크래시가
        //  이 지점에서 잡히는 형태입니다)
        val controller = Robolectric.buildActivity(LaunchActivity::class.java).create()
        val activity = controller.get()

        assertNotNull("onCreate 이후 액티비티 인스턴스가 없습니다", activity)
        assertNotNull("LaunchActivity 에 창(Window)이 붙지 않았습니다", activity.window)

        // LaunchActivity 는 온보딩/메인으로 넘기고 자기 자신을 finish() 합니다.
        // 그래서 isFinishing 은 정상일 수 있고, 소멸(isDestroyed)만 봅니다.
        assertFalse(
            "LaunchActivity 가 onCreate 도중 소멸했습니다",
            activity.isDestroyed
        )

        controller.destroy()
    }

    /**
     * v1.6.0(vc6) 크래시 재발 방지.
     *
     * `LaunchActivity` 는 프레임워크 테마 `Theme.Splash` 를 씁니다. 거기에
     * `AppCompatActivity` 를 붙이면 onCreate 직후 아래 예외로 앱이 죽습니다
     * (그 상태로 스토어에 올라갔었습니다):
     *
     *   IllegalStateException: You need to use a Theme.AppCompat theme
     *   (or descendant) with this activity.
     */
    @Test
    fun launchActivity_isNotAppCompatActivity() {
        assertFalse(
            "LaunchActivity 가 AppCompatActivity 입니다. Theme.Splash 와 충돌해 다시 크래시합니다",
            AppCompatActivity::class.java.isAssignableFrom(LaunchActivity::class.java)
        )
    }
}
