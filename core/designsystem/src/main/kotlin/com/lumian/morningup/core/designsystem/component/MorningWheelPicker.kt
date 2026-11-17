package com.lumian.morningup.core.designsystem.component

import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter

/** 드럼롤 한 칸 높이. 가운데 흰 띠(64)와 같아요. */
val WheelItemHeight: Dp = 64.dp

/** 드럼롤에 보이는 줄 수(위, 가운데, 아래). */
const val WHEEL_VISIBLE_ROWS = 3

/** 끝없이 도는 드럼롤이 실제로 갖는 반복 횟수. 손으로는 끝에 닿을 수 없을 만큼이에요. */
private const val LOOP_REPEAT = 1000

/**
 * 시간 드럼롤 한 칸 묶음(design-system '알람 편집 (B)', dev-plan '모션과 인터랙션').
 * 세 줄 모두 같은 크기(`Display/Medium` 40)이고 위아래 줄은 `Display/Medium Muted`과 `text/disabled`로 색만 연해요.
 * 손으로 튕기면 관성으로 돌다 가장 가까운 칸에 붙고, 한 칸 지날 때마다 틱 진동을 울려요.
 * 멈췄을 때만 [onSelect]로 알려요. [looping]이면 끝없이 돌아요(시, 분).
 */
@Composable
fun MorningWheelPicker(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = Dp.Unspecified,
    looping: Boolean = false,
    textAlign: TextAlign = TextAlign.Center,
    contentDescription: String? = null,
) {
    val size = items.size
    val count = if (looping) size * LOOP_REPEAT else size + 2
    val state = rememberLazyListState(initialFirstVisibleItemIndex = topIndexFor(selectedIndex, size, looping))
    val itemPx = with(LocalDensity.current) { WheelItemHeight.toPx() }
    val top by remember(state, itemPx) {
        derivedStateOf {
            state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
        }
    }
    val currentOnSelect by rememberUpdatedState(onSelect)
    val view = LocalView.current

    LaunchedEffect(state) {
        snapshotFlow { top }.distinctUntilChanged().drop(1).collect { view.perform(MorningHaptic.SegmentTick) }
    }
    LaunchedEffect(state, size) {
        snapshotFlow { state.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .drop(1)
            .collect { currentOnSelect(valueAt(state.firstVisibleItemIndex, size, looping)) }
    }
    // 밖에서 값이 바뀌면(불러오기 등) 그 칸으로 옮겨요. 손으로 돌리는 중에는 건드리지 않아요.
    LaunchedEffect(selectedIndex, size) {
        if (!state.isScrollInProgress && valueAt(top, size, looping) != selectedIndex) {
            state.scrollToItem(nearestTopIndex(state, selectedIndex, size, looping))
        }
    }

    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val label = items.getOrNull(selectedIndex).orEmpty()
    Box(
        modifier = modifier
            .then(if (width != Dp.Unspecified) Modifier.width(width) else Modifier)
            .height(WheelItemHeight * WHEEL_VISIBLE_ROWS)
            .semantics {
                stateDescription = label
                if (contentDescription != null) this.contentDescription = contentDescription
            },
    ) {
        LazyColumn(
            state = state,
            flingBehavior = rememberSnapFlingBehavior(state, SnapPosition.Start),
        ) {
            items(count) { index ->
                val value = itemValue(index, size, looping)
                val centered = index == top + 1
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WheelItemHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    if (value != null) {
                        Text(
                            text = items[value],
                            style = if (centered) typography.displayMedium else typography.displayMediumMuted,
                            color = if (centered) colors.textPrimary else colors.textDisabled,
                            textAlign = textAlign,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

/** 목록 칸 [index]에 그릴 값. 끝없이 돌지 않는 목록은 위아래 빈 칸이 하나씩 있어요. */
private fun itemValue(index: Int, size: Int, looping: Boolean): Int? = when {
    looping -> index % size
    index == 0 || index == size + 1 -> null
    else -> index - 1
}

/** 맨 위 줄이 [top]일 때 가운데 줄의 값. */
private fun valueAt(top: Int, size: Int, looping: Boolean): Int =
    if (looping) (top + 1) % size else top.coerceIn(0, size - 1)

private fun topIndexFor(value: Int, size: Int, looping: Boolean): Int =
    if (looping) (LOOP_REPEAT / 2) * size + value - 1 else value

/** 지금 자리에서 가장 가까운, [value]가 가운데 오는 맨 위 칸. */
private fun nearestTopIndex(state: LazyListState, value: Int, size: Int, looping: Boolean): Int {
    if (!looping) return value
    val center = state.firstVisibleItemIndex + 1
    val base = center - center % size
    val candidates = listOf(base - size + value, base + value, base + size + value)
    return candidates.minBy { kotlin.math.abs(it - center) } - 1
}
