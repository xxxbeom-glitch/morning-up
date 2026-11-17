package com.lumian.morningup.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val MessageMinHeight = 48.dp

/** 이만큼 옆으로 밀면 스낵바를 닫아요. */
private val SwipeDismissDistance = 96.dp

/** 저장 토스트(H6). 짙은 바탕(`text/primary`)에 흰 글자(`text/inverse`). */
@Composable
fun MorningToast(text: String, modifier: Modifier = Modifier) {
    MessageBody(modifier) {
        Text(
            text = text,
            style = MorningTheme.typography.bodySmall,
            color = MorningTheme.colors.textInverse,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 삭제 스낵바(H7). 토스트와 같은 몸체에 오른쪽 '되돌리기'. 옆으로 밀면 닫혀요. */
@Composable
fun MorningSnackbar(
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
    onSwipeDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val offset = remember { Animatable(0f) }
    val reduceMotion = MorningTheme.reduceMotion
    val dismissPx = with(LocalDensity.current) { SwipeDismissDistance.toPx() }
    MessageBody(
        modifier = modifier
            .offset { IntOffset(offset.value.roundToInt(), 0) }
            .graphicsLayer { alpha = 1f - (abs(offset.value) / (dismissPx * 2)).coerceIn(0f, 1f) }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta -> scope.launch { offset.snapTo(offset.value + delta) } },
                onDragStopped = {
                    if (abs(offset.value) > dismissPx) {
                        onSwipeDismiss()
                    } else {
                        offset.animateTo(0f, MorningMotion.spatial(MorningMotion.SpatialDefault, reduceMotion))
                    }
                },
            ),
    ) {
        Text(
            text = text,
            style = MorningTheme.typography.bodySmall,
            color = MorningTheme.colors.textInverse,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = actionLabel,
            style = MorningTheme.typography.bodySmallStrong,
            color = MorningTheme.colors.primaryIconSubtle,
            modifier = Modifier
                .morningClickable(haptic = MorningHaptic.Press, onClick = onAction)
                .padding(vertical = MorningDimens.space8, horizontal = MorningDimens.space4),
        )
    }
}

@Composable
private fun MessageBody(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val shape = RoundedCornerShape(MorningDimens.radiusChip)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MessageMinHeight)
            .morningShadow(MorningShadows.float, shape)
            .background(MorningTheme.colors.textPrimary, shape)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(horizontal = MorningDimens.space16, vertical = MorningDimens.space8),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * 토스트와 스낵바를 띄우는 자리. 아래에서 스프링(`spatialDefault`)으로 올라와 머물다 아래로 사라져요.
 * [message]가 null이면 숨어요. 바뀐 메시지는 [content]로 그려요.
 */
@Composable
fun <T : Any> MorningMessageHost(
    message: T?,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val reduceMotion = MorningTheme.reduceMotion
    // 사라지는 동안에도 마지막 메시지를 그대로 그려요.
    val last = remember { arrayOfNulls<Any>(1) }
    if (message != null) last[0] = message
    Box(modifier = modifier.fillMaxWidth()) {
        AnimatedVisibility(
            visible = message != null,
            enter = slideInVertically(MorningMotion.spatial(MorningMotion.SpatialDefault, reduceMotion)) { it } +
                fadeIn(MorningMotion.effects(MorningMotion.EffectsDefault)),
            exit = slideOutVertically(MorningMotion.spatial(MorningMotion.SpatialDefault, reduceMotion)) { it } +
                fadeOut(MorningMotion.effects(MorningMotion.EffectsDefault)),
        ) {
            @Suppress("UNCHECKED_CAST")
            (last[0] as? T)?.let { content(it) }
        }
    }
}
