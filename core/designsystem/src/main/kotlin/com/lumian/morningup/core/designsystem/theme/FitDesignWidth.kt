package com.lumian.morningup.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** 피그마 시안 프레임 폭(dp). */
const val DESIGN_WIDTH_DP = 360f

/**
 * 시안 폭 맞춤(데모와 같은 규칙). 폰 화면 폭을 360으로 보고 밀도를 다시 계산해서,
 * 모든 크기가 같은 비율로 커져 피그마와 같은 배치로 보여요. 예: 폭 411 폰이면 411/360 = 1.14배.
 * 기기 원래 밀도로 계산해서 여러 번 감싸도 두 번 커지지 않아요. 글꼴 배율은 그대로 넘겨요.
 */
@Composable
fun FitDesignWidth(content: @Composable () -> Unit) {
    val deviceDensity = LocalContext.current.resources.displayMetrics.density
    val widthPx = LocalConfiguration.current.screenWidthDp * deviceDensity
    val fitted = Density(density = widthPx / DESIGN_WIDTH_DP, fontScale = LocalDensity.current.fontScale)
    CompositionLocalProvider(LocalDensity provides fitted, content = content)
}
