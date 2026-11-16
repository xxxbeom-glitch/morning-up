package com.lumian.morningup.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow

/** 하단 메뉴 탭 하나. 글자는 보이지 않고 화면 읽기 프로그램용 이름으로만 써요. */
data class MorningNavItem(val icon: ImageVector, val label: String)

private val NavItemSize = 48.dp

/**
 * 피그마 Bottom Nav(`285:7033`). 가운데 떠 있는 알약(176x64), 칠 `surface/card` + `shadow/float`,
 * 48 원형 아이콘 버튼, 고른 탭 바탕 `primary/subtle`.
 */
@Composable
fun MorningBottomNav(
    items: List<MorningNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MorningTheme.colors
    val pill = RoundedCornerShape(MorningDimens.radiusFull)
    Row(
        modifier = modifier
            .morningShadow(MorningShadows.float, pill)
            .background(colors.surfaceCard, pill)
            .padding(MorningDimens.space8)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            NavButton(item = item, selected = index == selectedIndex, onClick = { onSelect(index) })
        }
    }
}

@Composable
private fun NavButton(item: MorningNavItem, selected: Boolean, onClick: () -> Unit) {
    val colors = MorningTheme.colors
    val fill by animateColorAsState(
        targetValue = if (selected) colors.primarySubtle else colors.surfaceCard,
        animationSpec = MorningMotion.effects(MorningMotion.EffectsDefault),
        label = "navFill",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) colors.primaryDefault else colors.textTertiary,
        animationSpec = MorningMotion.effects(MorningMotion.EffectsDefault),
        label = "navTint",
    )
    Box(
        modifier = Modifier
            .size(NavItemSize)
            .background(fill, CircleShape)
            .semantics {
                this.selected = selected
                contentDescription = item.label
            }
            .morningClickable(haptic = MorningHaptic.Tick, role = Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(MorningDimens.iconLg),
        )
    }
}
