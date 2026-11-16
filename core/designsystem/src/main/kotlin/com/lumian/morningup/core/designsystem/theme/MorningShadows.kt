package com.lumian.morningup.core.designsystem.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

private const val PERCENT = 100f

private fun shadow(name: String): Shadow {
    val token = ShadowTokens.getValue(name)
    return Shadow(
        radius = token.blur.dp,
        spread = token.spread.dp,
        color = Color(token.color),
        alpha = token.alphaPercent / PERCENT,
        offset = DpOffset(token.offsetX.dp, token.offsetY.dp),
    )
}

/** 피그마 그림자 스타일 3개. */
object MorningShadows {
    val card: Shadow = shadow("shadow/card")
    val float: Shadow = shadow("shadow/float")
    val knob: Shadow = shadow("shadow/knob")
}

/** 피그마 그림자를 그대로 그려요. 모양(shape)은 그 부품의 모서리와 같게 넘겨요. */
fun Modifier.morningShadow(shadow: Shadow, shape: Shape): Modifier = dropShadow(shape, shadow)
