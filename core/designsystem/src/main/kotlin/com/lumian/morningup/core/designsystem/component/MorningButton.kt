package com.lumian.morningup.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.motion.pressScale
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme

/** 피그마 Button 크기. Large 56, Medium 48, Circle 112(원형). */
enum class MorningButtonSize { Large, Medium, Circle }

/** 칠 종류. Primary 파랑, Secondary 회색(다이얼로그 '취소'), Danger 빨강(다이얼로그 '삭제', Button Alt Danger). */
enum class MorningButtonStyle { Primary, Secondary, Danger }

private const val DISABLED_ALPHA = 0.45f

/** 피그마 Button. 기본 칠 `primary/default`, 누르는 동안 `primary/pressed`와 0.96배. */
@Composable
fun MorningButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: MorningButtonSize = MorningButtonSize.Large,
    enabled: Boolean = true,
    haptic: MorningHaptic = MorningHaptic.Press,
    style: MorningButtonStyle = MorningButtonStyle.Primary,
) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val (rest, down, onFill) = when (style) {
        MorningButtonStyle.Primary -> Triple(colors.primaryDefault, colors.primaryPressed, colors.primaryOn)
        MorningButtonStyle.Secondary -> Triple(colors.backgroundRaised, colors.controlTrackOff, colors.textBody)
        MorningButtonStyle.Danger -> Triple(colors.statusError, colors.statusError, colors.textInverse)
    }
    val fill by animateColorAsState(
        targetValue = if (pressed) down else rest,
        animationSpec = MorningMotion.effects(MorningMotion.EffectsFast),
        label = "buttonFill",
    )
    val shape = if (size == MorningButtonSize.Circle) CircleShape else RoundedCornerShape(MorningDimens.radiusButton)
    val sized = when (size) {
        MorningButtonSize.Large -> modifier.fillMaxWidth().height(MorningDimens.sizeButtonLg)
        MorningButtonSize.Medium -> modifier.fillMaxWidth().height(MorningDimens.sizeButton)
        MorningButtonSize.Circle -> modifier.size(MorningDimens.sizeButtonCircle)
    }
    Box(
        modifier = sized
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .pressScale(interactionSource)
            .background(fill, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
            ) {
                view.perform(haptic)
                onClick()
            }
            .padding(horizontal = MorningDimens.space16),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = if (size == MorningButtonSize.Medium) typography.bodyMediumStrong else typography.titleMedium,
            color = onFill,
            textAlign = TextAlign.Center,
        )
    }
}
