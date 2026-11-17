package com.lumian.morningup.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** 기기 글꼴 크기를 아무리 키워도 앱 글자는 1.3배까지만 커져요(10단계 큰 글꼴 규칙). */
const val MAX_FONT_SCALE = 1.3f

/** 기기 글꼴 배율을 [MAX_FONT_SCALE]까지로 묶어요. [MorningTheme]이 앱 전체에 걸어요. */
@Composable
internal fun CappedFontScale(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    if (density.fontScale <= MAX_FONT_SCALE) {
        content()
        return
    }
    CompositionLocalProvider(LocalDensity provides Density(density.density, MAX_FONT_SCALE), content = content)
}

/**
 * 안쪽 글자를 기기 글꼴 크기와 상관없이 디자인 크기 그대로 둬요.
 * 큰 시각(울림 화면, 드럼롤)과 미션 키패드처럼 칸 크기가 정해진 곳에만 써요. 키우면 칸을 넘쳐요.
 */
@Composable
fun FixedFontScale(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, 1f), content = content)
}
