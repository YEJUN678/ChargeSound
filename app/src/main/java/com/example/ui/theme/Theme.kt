package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DefaultDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Slate950,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = ElectricCyan,
    secondary = ElectricAmber,
    onSecondary = Slate950,
    secondaryContainer = ElectricAmberContainer,
    onSecondaryContainer = ElectricAmber,
    tertiary = ElectricGreen,
    onTertiary = Slate950,
    background = Slate950,
    onBackground = Slate50,
    surface = Slate900,
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate200,
    outline = Slate700,
    outlineVariant = Slate800
)

private val DefaultLightColorScheme = lightColorScheme(
    primary = Color(0xFF007A8C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8EAFF),
    onPrimaryContainer = Color(0xFF001F28),
    secondary = Color(0xFF805600),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDF9E),
    onSecondaryContainer = Color(0xFF2B1700),
    tertiary = Color(0xFF006D37),
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

/**
 * 강조색 팔레트로 색상 스킴을 만듭니다.
 *
 * 스킴도 함수로 만들어야 합니다 — 팔레트가 바뀔 때 스킴이 갱신되도록.
 */
private fun darkSchemeOf(palette: AccentPalette) = darkColorScheme(
    primary = palette.cyan,
    onPrimary = Slate950,
    primaryContainer = palette.cyanContainer,
    onPrimaryContainer = palette.cyan,
    secondary = palette.amber,
    onSecondary = Slate950,
    secondaryContainer = palette.amberContainer,
    onSecondaryContainer = palette.amber,
    tertiary = palette.green,
    onTertiary = Slate950,
    background = Slate950,
    onBackground = Slate50,
    surface = Slate900,
    onSurface = Slate50,
    surfaceVariant = Slate800,
    onSurfaceVariant = Slate200,
    outline = Slate700,
    outlineVariant = Slate800
)

private fun lightSchemeOf(palette: AccentPalette) = lightColorScheme(
    primary = palette.cyanDark,
    onPrimary = Color.White,
    primaryContainer = palette.cyanContainer,
    onPrimaryContainer = Color(0xFF001F28),
    secondary = palette.amberDark,
    onSecondary = Color.White,
    secondaryContainer = palette.amberContainer,
    onSecondaryContainer = Color(0xFF2B1700),
    tertiary = palette.greenDark,
    onTertiary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

/**
 * 앱 테마.
 *
 * [accent] 를 바꾸면 전역 색 상수와 MaterialTheme 스킴이 함께 갱신됩니다.
 * 호출부(MainActivity)가 설정 값을 상태로 읽어 넘겨주므로, 강조색을 바꾸면
 * 이 컴포저블과 그 아래 전부가 다시 그려집니다.
 *
 * @param dynamicColor Android 12(Material You) 배경색을 쓸지 여부.
 *   켜면 브랜드 색이 기기 월피퍼 색으로 바뀝니다. 앱 정체성을 유지하려면 꺼 두세요.
 * @param highContrast 대비를 올립니다. 저视力 사용자와 밝은 햇빛 아래를 위한 옵션입니다.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    highContrast: Boolean = false,
    accent: AccentPalette = AccentPalette.Default,
    content: @Composable () -> Unit,
) {
    // 전역 색 상수에 사용자 선택을 반영합니다.
    // 하위 168개 참조 지점이 이 값을 그대로 읽습니다.
    accent.apply()

    val context = LocalContext.current
    val supportsDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val baseScheme = when {
        dynamicColor && supportsDynamic ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> darkSchemeOf(accent)
        else -> lightSchemeOf(accent)
    }

    // 고대비는 색 자체를 어둡게/밝게 조정하는 게 아니라, 대비가 낮은
    // 보조 역할 색만 단계적으로 분리해 줍니다. 그러면 어떤 팔레트에서도
    // 텍스트가 배경에 묻히지 않습니다.
    val colorScheme = if (highContrast) baseScheme.boostContrast(darkTheme) else baseScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapeScheme,
        content = content
    )
}

/**
 * 표면 역할 색을 단계적으로 분리합니다.
 *
 * Material 3 의 `surfaceContainer*` 역할은 카드가 층층이 쌓인 화면에서
 * 깊이감을 만듭니다. 이 앱은 다크 배경 위의 네온 UI 라 카드가 전부 같은
 * 회색이라 평평해 보였습니다. 여기서 역할을 나눠서 카드·시트가
 * 실제로 "떠 있는" 것처럼 보이게 합니다.
 *
 * 다크에서는 밝은 방향으로, 라이트에서는 어두운 방향으로 단계화합니다.
 */
private fun ColorScheme.boostContrast(dark: Boolean): ColorScheme = if (dark) {
    copy(
        surface = Slate900,
        surfaceContainerLowest = Slate950,
        surfaceContainerLow = Slate800,
        surfaceContainer = Slate800,
        surfaceContainerHigh = Slate700,
        surfaceContainerHighest = Slate700,
        surfaceBright = Slate700,
        surfaceDim = Slate950,
    )
} else {
    copy(
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color(0xFFF1F5F9),
        surfaceContainer = Color(0xFFEDF2F7),
        surfaceContainerHigh = Color(0xFFE2E8F0),
        surfaceContainerHighest = Color(0xFFCBD5E1),
    )
}