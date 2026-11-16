package com.lumian.morningup.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow

enum class MorningToggleSize(val width: Dp, val height: Dp) {
    Default(MorningDimens.sizeSwitchW, MorningDimens.sizeSwitchH),
    Small(MorningDimens.sizeSwitchSmW, MorningDimens.sizeSwitchSmH),
}

/** 손잡이가 누르는 동안 옆으로 늘어나는 길이. */
private val KnobStretch = 4.dp

/** 비활성 토글 투명도(design-system 설정 줄 규칙). */
private const val DISABLED_ALPHA = 0.45f

/**
 * 피그마 Toggle. 켜짐 `primary/default`, 꺼짐 `control/track-off`, 손잡이 `control/knob` + `shadow/knob`.
 * 손잡이는 스프링으로 미끄러지고, 누르는 동안 살짝 늘어나요. 켜고 끌 때 TOGGLE 진동을 울려요.
 */
@Composable
fun MorningToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: MorningToggleSize = MorningToggleSize.Default,
    enabled: Boolean = true,
) {
    val colors = MorningTheme.colors
    val reduceMotion = MorningTheme.reduceMotion
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val inset = MorningDimens.sizeSwitchInset
    val knob = size.height - inset * 2
    val knobWidth by animateDpAsState(
        targetValue = if (pressed && enabled && !reduceMotion) knob + KnobStretch else knob,
        animationSpec = MorningMotion.spatial(MorningMotion.SpatialFast, reduceMotion),
        label = "knobWidth",
    )
    val travel = size.width - inset * 2 - knobWidth
    val knobOffset by animateDpAsState(
        targetValue = if (checked) travel else 0.dp,
        animationSpec = MorningMotion.spatial(MorningMotion.SpatialDefault, reduceMotion),
        label = "knobOffset",
    )
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.primaryDefault else colors.controlTrackOff,
        animationSpec = MorningMotion.effects(MorningMotion.EffectsDefault),
        label = "trackColor",
    )

    Box(
        modifier = modifier
            .size(size.width, size.height)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .background(trackColor, RoundedCornerShape(MorningDimens.radiusFull))
            .toggleable(
                value = checked,
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Switch,
            ) { next ->
                view.perform(if (next) MorningHaptic.ToggleOn else MorningHaptic.ToggleOff)
                onCheckedChange(next)
            }
            .padding(inset),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset { IntOffset(knobOffset.roundToPx(), 0) }
                .size(knobWidth, knob)
                .morningShadow(MorningShadows.knob, CircleShape)
                .background(colors.controlKnob, CircleShape),
        )
    }
}
