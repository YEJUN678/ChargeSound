package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 앱 타이포그래피 스케일.
 *
 * ## 왜 직접 정의하는가
 * Material 3 의 기본 타이포그래피는 그대로 두면 화면마다 `fontSize = 13.sp` 처럼
 * 숫자를 하드코딩하게 됩니다. 그러면 "제목이면서 설명이면서 캡션"처럼
 * 역할이 섞여서, 나중에 스케일을 바꿔도 화면마다 따로 손봐야 합니다.
 *
 * 여기서 **역할**을 정의하고 UI 는 `MaterialTheme.typography.xxx` 로 참조합니다.
 * 그러면 여기 한 곳만 고치면 앱 전체가 함께 바뀝니다.
 *
 * ## 숫자를 고른 기준
 * - 헤더는 SemiBold 이상. 얇은 글자는 다크 배경에서 흐려집니다.
 * - 설명·캡션은 Normal + 낮은 대비 색. 본문과 구분되되 시선을 뺏지 않아야 합니다.
 * - 자간(letterSpacing)은 숫자·캡션에서만 살짝 좁혔습니다. 이 앱은
 *   배터리 수치와 온도 같은 수치 UI가 많기 때문입니다.
 */
val AppTypography = Typography(
    // ── 표시 (배터리 % 같은 단일 숫자 강조) ──
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 52.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 42.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.25).sp,
    ),

    // ── 제목 ──
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
    ),

    // ── 큰 타이틀 (카드·섹션 헤더) ──
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    // ── 본문 ──
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.15.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),

    // ── 레이블 (버튼 · 탭 · 칩) ──
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
    ),
)

/**
 * 앱 전용 의미 이름.
 *
 * Material 스케일 이름(displayLarge, bodyMedium…)만으로는 이 앱의 맥락이
 * 보이지 않습니다. 여기서 한 번 더 이름을 붙여서, "무엇을 위한 텍스트인가"를
 * 코드에서 드러냅니다.
 */
object AppText {
    /** 온도·전압 같은 단위 표기. 좁은 자간으로 수치 밀도를 높입니다. */
    val Metric = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    )

    /** 배지·칩 안의 아주 작은 글자. 최소 11sp — 이보다 작으면 고령자가 못 읽습니다. */
    val Badge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.2.sp,
    )

    /** 그래프 축 라벨. 정보 전달이 목적이 아니라 맥락 제공이 목적입니다. */
    val Axis = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 10.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.2.sp,
    )

    /** 앱 타이틀 로고처럼 무게감을 줘야 하는 자리 */
    val Brand = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp,
    )
}

/** 레거시 별칭. Theme.kt 의 기존 참조를 유지합니다. */
val Typography = AppTypography