package com.lumian.morningup.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme

private val RadioSize = 20.dp
private val RadioDot = 8.dp
private val RadioOffStroke = 1.5.dp

/**
 * 피그마 Radio(`148:69`). 지름 20. 고르면 `primary/default` 칠 원 안에 흰 점, 안 고르면 `text/disabled` 선 원
 * (라디오 부품 모양 그대로. 줄 자체에는 배경 칠이 없어요).
 */
@Composable
fun MorningRadio(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    val fill by animateColorAsState(
        targetValue = if (selected) colors.primaryDefault else colors.primaryDefault.copy(alpha = 0f),
        animationSpec = MorningMotion.effects(MorningMotion.EffectsFast),
        label = "radioFill",
    )
    Box(
        modifier = modifier
            .size(RadioSize)
            .background(fill, CircleShape)
            .then(if (selected) Modifier else Modifier.border(RadioOffStroke, colors.textDisabled, CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.size(RadioDot).background(colors.primaryOn, CircleShape))
    }
}

/**
 * 피그마 Radio Row(`148:92`). 왼쪽 라디오, 사이 `space/8`, 이름. 고른 줄은 이름이 `primary/text` 굵은 글자예요
 * (설정 줄 값 글자의 파란색 금지와는 다른 부품). 카드 안에서는 줄 높이 `size/row` 36이에요.
 */
@Composable
fun MorningRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MorningDimens.sizeRow)
            .semantics { this.selected = selected }
            .morningClickable(haptic = MorningHaptic.Tick, role = Role.RadioButton, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space8),
    ) {
        MorningRadio(selected = selected)
        Text(
            text = label,
            style = if (selected) typography.bodyMediumStrong else typography.bodyMedium,
            color = if (selected) colors.primaryText else colors.textPrimary,
        )
    }
}
