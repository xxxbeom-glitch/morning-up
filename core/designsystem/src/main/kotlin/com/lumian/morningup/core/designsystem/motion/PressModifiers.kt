package com.lumian.morningup.core.designsystem.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import com.lumian.morningup.core.designsystem.theme.LocalReduceMotion

/**
 * 누르는 동안 0.96배로 줄었다가 놓으면 스프링으로 돌아와요. 물결 효과는 쓰지 않아요.
 * '애니메이션 줄이기'면 크기는 그대로 둬요.
 */
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier = composed {
    val reduceMotion = LocalReduceMotion.current
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduceMotion) MorningMotion.PRESS_SCALE else 1f,
        animationSpec = MorningMotion.spatial(MorningMotion.SpatialFast, reduceMotion = false),
        label = "pressScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** 누름 크기 반응 + 진동 + 클릭. 앱의 누를 수 있는 부품은 이것을 써요. */
fun Modifier.morningClickable(
    haptic: MorningHaptic? = MorningHaptic.Press,
    enabled: Boolean = true,
    role: Role? = Role.Button,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    this
        .pressScale(interactionSource)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
        ) {
            haptic?.let(view::perform)
            onClick()
        }
}
