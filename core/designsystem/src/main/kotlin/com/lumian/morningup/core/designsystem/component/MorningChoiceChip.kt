package com.lumian.morningup.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.motion.morningClickable

private val ChoiceChipHeight = 36.dp
private val ChoiceChipRadius = 12.dp
private const val UNSELECTED_ALPHA = 0.5f

/**
 * 피그마 Choice Chip(S6 볼륨 시트, 다시 울림 시트). 높이 36, 모서리 12, 테두리 없이 칠로만 고름을 보여요.
 * 고름: `primary/subtle` + `Body/Small Strong` `primary/text`. 안 고름: `background/raised` + `Body/Small` `text/tertiary`,
 * 칸 전체 투명도 50%. 반복 요일 칸(Day Chip, 결정 76)과 같게 맞췄어요(11-19 사용자 지적 3).
 * 너비는 쓰는 쪽이 정해요(줄 너비에 꽉 차게 나누면 `Modifier.weight(1f)`).
 */
@Composable
fun MorningChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MorningTheme.colors
    val fill by animateColorAsState(
        targetValue = if (selected) colors.primarySubtle else colors.backgroundRaised,
        animationSpec = MorningMotion.effects(MorningMotion.EffectsFast),
        label = "choiceChipFill",
    )
    Box(
        modifier = modifier
            .height(ChoiceChipHeight)
            .alpha(if (selected) 1f else UNSELECTED_ALPHA)
            .semantics { this.selected = selected }
            .morningClickable(haptic = MorningHaptic.Tick, role = Role.RadioButton, onClick = onClick)
            .background(fill, RoundedCornerShape(ChoiceChipRadius)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = if (selected) MorningTheme.typography.bodySmallStrong else MorningTheme.typography.bodySmall,
            color = if (selected) colors.primaryText else colors.textTertiary,
        )
    }
}

/** 시트 머리(피그마 Sheet Header): 제목 `Title/Medium` `text/primary`, 사이 4, 설명 `Body/Small` `text/secondary`. */
@Composable
fun MorningSheetHeader(title: String, description: String? = null, modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Column(modifier = modifier) {
        Text(text = title, style = MorningTheme.typography.titleMedium, color = MorningTheme.colors.textPrimary)
        if (description != null) {
            androidx.compose.foundation.layout.Spacer(Modifier.height(MorningDimens.space4))
            Text(
                text = description,
                style = MorningTheme.typography.bodySmall,
                color = MorningTheme.colors.textSecondary,
            )
        }
    }
}
