package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * 간격 · 모서리 · 여백 토큰.
 *
 * ## 왜 4dp 격자로 묶는가
 * 화면마다 `Spacer(Modifier.height(14.dp))`, `padding(18.dp)` 처럼
 * 임의 숫자가 흩어지면 정렬이 어긋나 보입니다. 아래 스케일에서만 값을 고르면
 * 모든 여백이 자연스럽게 맞아떨어집니다.
 *
 * (4dp 격자인 이유: 1dp = 1/160인치. 안드로이드가 표준으로 쓰는 단위입니다.)
 */
object Spacing {
    /** 아이콘과 텍스트 사이 같은 아주 세밀한 간격 */
    val xxs = 2.dp
    /** 뱃지 내부 패딩, 인접 아이콘 */
    val xs = 4.dp
    /** 컴포넌트 내부 여백 (칩, 작은 버튼) */
    val sm = 8.dp
    /** 리스트 행 안 여백, 카드 내부 여백 */
    val md = 12.dp
    /** 카드 사이, 섹션 안 여백 — 화면의 기본 단위 */
    val lg = 16.dp
    /** 섹션 사이 */
    val xl = 24.dp
    /** 큰 블록 사이 (시트 최상단 등) */
    val xxl = 32.dp

    /** 화면 좌우 표준 여백 */
    val screen = 16.dp
}

/**
 * 모서리 반경.
 *
 * Material 3 의 조언을 따릅니다: 배지가 작을수록 각지게, 클수록 둥글게.
 * 아래 값들은 그 규칙에 맞춘 것입니다.
 */
object AppShapes {
    /** 아주 작은 요소 (태그, 인디케이터 바) */
    val xs = RoundedCornerShape(6.dp)
    /** 칩, 작은 버튼 */
    val sm = RoundedCornerShape(10.dp)
    /** 리스트 행, 인라인 카드 */
    val md = RoundedCornerShape(14.dp)
    /** 표준 카드 — 화면에서 가장 많이 쓰입니다 */
    val lg = RoundedCornerShape(20.dp)
    /** 대형 카드, 히어로 영역 */
    val xl = RoundedCornerShape(28.dp)
    /** 완전한 원, 배지, 태그형 칩 */
    val pill = RoundedCornerShape(999.dp)
}

/** MaterialTheme 에 넘길 Shapes. */
val AppShapeScheme = Shapes(
    extraSmall = AppShapes.xs,
    small = AppShapes.sm,
    medium = AppShapes.md,
    large = AppShapes.lg,
    extraLarge = AppShapes.xl,
)

/**
 * 모션 토큰.
 *
 * 화면 전환이 "즉시 바뀐다"가 아니라 "부드럽게 바뀐다"를 만드는 값들입니다.
 * 진입/이탈에 같은 이징을 쓰면 전체가 하나로 뭉쳐 보여요.
 */
object Motion {
    /**
     * 기본 스프링.
     *
     * `dampingRatio = 0.8` 이 약간의 오버슈트를 주는데, 이것이 현대적인 인상을 줍니다.
     * 1.0 에 가까울수록 overshoot 이 없어 오히려 평평하게 느껴집니다.
     */
    const val SPRING_STIFFNESS = 380f
    const val SPRING_DAMPING = 26f

    /** 카드/칩 눌림 등 짧은 반응 */
    const val FAST_MILLIS = 120

    /** 탭 전환, 시트 등장 */
    const val MEDIUM_MILLIS = 240

    /** 화면 전체 전환 */
    const val SLOW_MILLIS = 380
}