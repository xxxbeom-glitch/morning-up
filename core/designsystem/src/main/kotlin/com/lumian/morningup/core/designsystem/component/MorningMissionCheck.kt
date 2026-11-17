package com.lumian.morningup.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.theme.MorningTheme

private val CheckSize = 24.dp
private val CheckIcon = 16.dp
private val OffStroke = 1.5.dp

/**
 * S1-2 미션 카드 오른쪽 체크 원(24). 고르면 `primary/default` 칠에 체크 16,
 * 안 고르면 `text/disabled` 1.5 테두리 빈 원(체크박스 예외 규칙과 같은 모양).
 */
@Composable
fun MorningMissionCheck(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    val fill by animateColorAsState(
        targetValue = if (selected) colors.primaryDefault else Color.Transparent,
        animationSpec = MorningMotion.effects(MorningMotion.EffectsFast),
        label = "missionCheck",
    )
    Box(
        modifier = modifier
            .size(CheckSize)
            .background(fill, CircleShape)
            .then(if (selected) Modifier else Modifier.border(OffStroke, colors.textDisabled, CircleShape)),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Lucide.Check,
                contentDescription = null,
                tint = colors.primaryOn,
                modifier = Modifier.size(CheckIcon),
            )
        }
    }
}
