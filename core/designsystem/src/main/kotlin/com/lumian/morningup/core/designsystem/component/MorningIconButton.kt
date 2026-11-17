package com.lumian.morningup.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme

private val IconButtonSize = 48.dp

/** 피그마 Icon Button. 48 원형. Active는 `primary/subtle` 바탕에 파란 아이콘. */
@Composable
fun MorningIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    val colors = MorningTheme.colors
    Box(
        modifier = modifier
            .size(IconButtonSize)
            .semantics { this.contentDescription = contentDescription }
            .morningClickable(haptic = MorningHaptic.Tick, onClick = onClick)
            .background(if (active) colors.primarySubtle else Color.Transparent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) colors.primaryDefault else colors.textPrimary,
            modifier = Modifier.size(MorningDimens.iconLg),
        )
    }
}
