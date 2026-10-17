package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────
// 강조색(accent) 테마
//
// 앱 전체가 네온 팔레트를 쓰기 때문에 색 상수 9종이 UI 파일 6개에서
// 168회 참조됩니다. 사용자 강조색을 적용하려면 이 값들이 런타임에
// 교체되어야 하므로 val 대신 var 를 사용합니다.
//
// 적용 방식은 MyApplicationTheme 이 진입점에서 AccentPalette.apply() 로
// 값을 덮어쓴 뒤, 하위 모든 컴포저블이 이 값을 읽는 구조입니다.
// MyApplicationTheme 이 recompose 되면 하위도 전부 다시 그려집니다.
//
// ⚠️ 단점: 전역 가변 상태라 Compose 가 값 변화를 직접 관찰하지는 못합니다.
//    설정 변경 시 테마가 재조합되도록 MainScreen 이 설정 흐름을 상태로 읽고
//    MyApplicationTheme 에 전달합니다 (그래야 안전합니다).
// ─────────────────────────────────────────────────────────────────

/** 강조색 프리셋. 사용자가 이 중 하나를 고릅니다. */
data class AccentPalette(
    val cyan: Color,
    val cyanDark: Color,
    val cyanContainer: Color,
    val amber: Color,
    val amberDark: Color,
    val amberContainer: Color,
    val green: Color,
    val greenDark: Color,
    val purple: Color
) {
    companion object {
        /** 기본 네온 시안 */
        val Default = AccentPalette(
            cyan = Color(0xFF00E5FF),
            cyanDark = Color(0xFF00B0FF),
            cyanContainer = Color(0xFF004D61),
            amber = Color(0xFFFFD600),
            amberDark = Color(0xFFFFAB00),
            amberContainer = Color(0xFF664D00),
            green = Color(0xFF00E676),
            greenDark = Color(0xFF00C853),
            purple = Color(0xFFD500F9)
        )

        /** 마젠타/핑크 — 복잡한 앱 아이콘 배경 위에서 대비가 큽니다. */
        val Magenta = AccentPalette(
            cyan = Color(0xFFFF2D92),
            cyanDark = Color(0xFFE5006E),
            cyanContainer = Color(0xFF5A0025),
            amber = Color(0xFFFFB300),
            amberDark = Color(0xFFE09600),
            amberContainer = Color(0xFF5A3D00),
            green = Color(0xFF00E5A0),
            greenDark = Color(0xFF00C484),
            purple = Color(0xFFB388FF)
        )

        /** 보라/인디고 — 차분한 톤을 선호하는 경우 */
        val Violet = AccentPalette(
            cyan = Color(0xFF9D7BFF),
            cyanDark = Color(0xFF7E5BFF),
            cyanContainer = Color(0xFF2E1A5E),
            amber = Color(0xFFFFC94D),
            amberDark = Color(0xFFE8B230),
            amberContainer = Color(0xFF5C4300),
            green = Color(0xFF4ADE80),
            greenDark = Color(0xFF22C55E),
            purple = Color(0xFFE879F9)
        )

        /** 라임/민트 — 밝은 배경에서도 잘 보입니다. */
        val Lime = AccentPalette(
            cyan = Color(0xFF76FF03),
            cyanDark = Color(0xFF5BD600),
            cyanContainer = Color(0xFF274800),
            amber = Color(0xFFFFE082),
            amberDark = Color(0xFFEFC55B),
            amberContainer = Color(0xFF5A4A00),
            green = Color(0xFF00E676),
            greenDark = Color(0xFF00C853),
            purple = Color(0xFFEA80FC)
        )

        /** 오렌지/웜톤 */
        val Ember = AccentPalette(
            cyan = Color(0xFFFF8A3D),
            cyanDark = Color(0xFFF26D1B),
            cyanContainer = Color(0xFF5C2A00),
            amber = Color(0xFFFFD54F),
            amberDark = Color(0xFFFFC107),
            amberContainer = Color(0xFF5E4A00),
            green = Color(0xFF6EE7A8),
            greenDark = Color(0xFF34C77B),
            purple = Color(0xFFC77DFF)
        )

        val ALL = listOf(Default, Magenta, Violet, Lime, Ember)

        /** 프리셋 이름을 안정적으로 저장하기 위한 키 */
        fun keyOf(palette: AccentPalette): String = when (palette) {
            Default -> "cyan"
            Magenta -> "magenta"
            Violet -> "violet"
            Lime -> "lime"
            Ember -> "ember"
            else -> "cyan"
        }

        fun fromKey(key: String?): AccentPalette = when (key) {
            "magenta" -> Magenta
            "violet" -> Violet
            "lime" -> Lime
            "ember" -> Ember
            else -> Default
        }
    }
}

// Electric High-Tech Palette
// (앱 시작 시 AccentPalette.apply() 로 사용자 선택이 덮어씁니다)
var ElectricCyan = AccentPalette.Default.cyan
var ElectricCyanDark = AccentPalette.Default.cyanDark
var ElectricCyanContainer = AccentPalette.Default.cyanContainer

var ElectricAmber = AccentPalette.Default.amber
var ElectricAmberDark = AccentPalette.Default.amberDark
var ElectricAmberContainer = AccentPalette.Default.amberContainer

var ElectricGreen = AccentPalette.Default.green
var ElectricGreenDark = AccentPalette.Default.greenDark

var ElectricPurple = AccentPalette.Default.purple

// 중립 색은 강조색과 무관하므로 고정입니다.
val Slate950 = Color(0xFF0A0F1D)
val Slate900 = Color(0xFF0F172A)
val Slate800 = Color(0xFF1E293B)
val Slate700 = Color(0xFF334155)
val Slate600 = Color(0xFF475569)
val Slate400 = Color(0xFF94A3B8)
val Slate200 = Color(0xFFE2E8F0)
val Slate50 = Color(0xFFF8FAFC)

/** 선택된 팔레트를 전역 색 상수에 반영합니다. */
fun AccentPalette.apply() {
    ElectricCyan = cyan
    ElectricCyanDark = cyanDark
    ElectricCyanContainer = cyanContainer
    ElectricAmber = amber
    ElectricAmberDark = amberDark
    ElectricAmberContainer = amberContainer
    ElectricGreen = green
    ElectricGreenDark = greenDark
    ElectricPurple = purple
}