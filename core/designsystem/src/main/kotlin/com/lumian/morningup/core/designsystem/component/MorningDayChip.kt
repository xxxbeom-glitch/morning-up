package com.lumian.morningup.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme

/**
 * 피그마 Day Chip. 36 정사각, 테두리 없이 칠로만 고름을 보여요.
 * 고름: `primary/subtle` 바탕 + `Body/Small Strong` `primary/text`.
 * 안 고름: `background/raised` 바탕 + `Body/Small` `text/tertiary`.
 */
@Composable
fun MorningDayChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = label,
) {
    val colors = MorningTheme.colors
    val fill by animateColorAsState(
        targetValue = if (selected) colors.primarySubtle else colors.backgroundRaised,
        animationSpec = MorningMotion.effects(MorningMotion.EffectsFast),
        label = "dayChipFill",
    )
    val text by animateColorAsState(
        targetValue = if (selected) colors.primaryText else colors.textTertiary,
        animationSpec = MorningMotion.effects(MorningMotion.EffectsFast),
        label = "dayChipText",
    )
    Box(
        modifier = modifier
            .size(MorningDimens.sizeChip)
            .semantics {
                this.selected = selected
                this.contentDescription = contentDescription
            }
            .morningClickable(haptic = MorningHaptic.Tick, role = Role.Checkbox, onClick = onClick)
            .background(fill, RoundedCornerShape(MorningDimens.radiusChip)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = if (selected) MorningTheme.typography.bodySmallStrong else MorningTheme.typography.bodySmall,
            color = text,
        )
    }
}
