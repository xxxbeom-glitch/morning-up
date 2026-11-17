package com.lumian.morningup.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme

private val CheckIconSize = 14.dp
private val OffStroke = 1.5.dp

/**
 * 피그마 Select Mark(`189:1410`). 20 둥근 사각(모서리 6).
 * On은 `primary/default` 칠에 흰 체크 14, Off는 `text/disabled` 1.5 테두리 빈 사각
 * (칠로만 고르는 규칙의 예외, 11-16 사용자 결정).
 */
@Composable
fun MorningSelectMark(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    val shape = RoundedCornerShape(MorningDimens.radiusCheckbox)
    Box(
        modifier = modifier
            .size(MorningDimens.sizeCheckbox)
            .then(
                if (selected) {
                    Modifier.background(colors.primaryDefault, shape)
                } else {
                    Modifier.border(OffStroke, colors.textDisabled, shape)
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Lucide.Check,
                contentDescription = null,
                tint = colors.textInverse,
                modifier = Modifier.size(CheckIconSize),
            )
        }
    }
}
