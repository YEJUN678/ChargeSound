package com.example

import com.example.ui.LaunchActivity
import org.junit.Assert.assertFalse
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
        val controller = Robolectric.buildActivity(LaunchActivity::class.java).setup()
        val activity = controller.get()

        assertFalse(
            "LaunchActivity 가 onCreate 직후 스스로 종료되었습니다",
            activity.isFinishing
        )

        controller.pause().stop().destroy()
    }
}
