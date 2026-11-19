package com.lumian.morningup.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
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

/** 떠 있는 하단 메뉴 높이(48 + 위아래 8). 화면이 아래 여백과 토스트 자리를 잡을 때 써요. */
val MorningBottomNavHeight = 64.dp

/**
 * 피그마 Bottom Nav(`285:7033`). 가운데 떠 있는 알약(176x64)과 `shadow/float`.
 * 라이트 모드는 칠을 `surface/card`(흰색) 그대로 두고 그림자로만 구분해요(11-18 사용자 요청).
 * 다크 모드는 그림자가 잘 안 보여서 카드보다 한 단계 밝은 `background/raised`를 써요.
 * 48 원형 아이콘 버튼, 고른 탭 바탕 `primary/subtle`.
 */
@Composable
fun MorningBottomNav(
    items: List<MorningNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    alertIndex: Int? = null,
    alertLabel: String = "",
) {
    val colors = MorningTheme.colors
    val pill = RoundedCornerShape(MorningDimens.radiusFull)
    val base = navBase()
    Row(
        modifier = modifier
            .morningShadow(MorningShadows.float, pill)
            .background(base, pill)
            .padding(MorningDimens.space8)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, item ->
            NavButton(
                item = item,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                alert = alertLabel.takeIf { index == alertIndex },
            )
        }
    }
}

/** 알약 칠. 라이트는 흰 카드색, 다크는 한 단계 밝은 칠. */
@Composable
private fun navBase() = MorningTheme.colors.let { if (it.isDark) it.backgroundRaised else it.surfaceCard }

@Composable
private fun NavButton(item: MorningNavItem, selected: Boolean, onClick: () -> Unit, alert: String?) {
    val colors = MorningTheme.colors
    val fill by animateColorAsState(
        targetValue = if (selected) colors.primarySubtle else navBase(),
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
        if (alert != null) {
            AlertBubble(
                text = alert,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = -BubbleLift)
                    .wrapContentSize(unbounded = true),
            )
        }
    }
}

private val BubbleHeight = 24.dp
private val TailWidth = 10.dp
private val TailHeight = 6.dp

/** '확인 필요' 말풍선이 메뉴 위로 솟는 높이(메뉴 위 4 + 꼬리 6 + 알약 24). 토스트처럼 메뉴 위에 뜨는 것을 이만큼 더 올려요. */
val MorningBottomNavAlertRise = 34.dp

/** 말풍선 아래 끝이 메뉴 위 4에 오게 버튼 위에서 올리는 거리(메뉴 안 여백 8 + 4 + 꼬리 6 + 알약 24). */
private val BubbleLift = 42.dp

/**
 * '확인 필요' 말풍선(design-system 하단 메뉴 Alert). `primary/default` 알약(모서리 12, 안쪽 위아래 4 좌우 10)과
 * 꼬리 10x6, `primary/on` `Caption/Strong` 글자. 꼬리 끝이 아이콘 가운데를 가리켜요. 테두리와 그림자는 없어요.
 */
@Composable
private fun AlertBubble(text: String, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = text,
            style = MorningTheme.typography.captionStrong,
            color = colors.primaryOn,
            maxLines = 1,
            modifier = Modifier
                .height(BubbleHeight)
                .background(colors.primaryDefault, RoundedCornerShape(BubbleHeight / 2))
                .padding(horizontal = MorningDimens.space8 + 2.dp, vertical = MorningDimens.space4),
        )
        Canvas(Modifier.size(TailWidth, TailHeight)) {
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2, size.height)
                close()
            }
            drawPath(path, colors.primaryDefault)
        }
    }
}
