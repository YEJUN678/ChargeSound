package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
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
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    accent: AccentPalette = AccentPalette.Default,
    content: @Composable () -> Unit,
) {
    // 전역 색 상수에 사용자 선택을 반영합니다.
    // 하위 168개 참조 지점이 이 값을 그대로 읽습니다.
    accent.apply()

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkSchemeOf(accent)
        else -> lightSchemeOf(accent)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}