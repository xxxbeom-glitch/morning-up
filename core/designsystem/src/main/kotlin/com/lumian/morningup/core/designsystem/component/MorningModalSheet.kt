package com.lumian.morningup.core.designsystem.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.theme.MorningFixedColors
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import kotlin.math.roundToInt

/** 끌어 내려 이만큼(px) 넘기면 닫아요. */
private const val DISMISS_DRAG_PX = 160f

/**
 * 화면 위에 띄우는 바텀시트. 뒤 막(`Scrim`)은 페이드, 판은 아래에서 `spatialDefault` 스프링으로 올라와요.
 * 뒤 막을 누르거나, 손잡이 쪽을 끌어 내리거나, 뒤로 가기를 하면 [onDismiss]를 불러요.
 * 판은 [MorningSheetSurface]를 써요.
 */
@Composable
fun MorningModalSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tag: String = "modalSheet",
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduceMotion = MorningTheme.reduceMotion
    BackHandler(enabled = visible, onBack = onDismiss)
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(MorningMotion.effects(MorningMotion.EffectsDefault)),
            exit = fadeOut(MorningMotion.effects(MorningMotion.EffectsDefault)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MorningFixedColors.Scrim)
                    .clickable(
                        interactionSource = remember {
                            MutableInteractionSource()
                        },
                        indication = null,
                        onClick = onDismiss,
                    ),
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(MorningMotion.spatial<IntOffset>(MorningMotion.SpatialDefault, reduceMotion)) {
                it
            },
            exit = slideOutVertically(MorningMotion.spatial<IntOffset>(MorningMotion.SpatialDefault, reduceMotion)) {
                it
            },
        ) {
            var drag by remember { mutableFloatStateOf(0f) }
            MorningSheetSurface(
                modifier = Modifier
                    .testTag(tag)
                    .offset { IntOffset(0, drag.roundToInt()) }
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta -> drag = (drag + delta).coerceAtLeast(0f) },
                        onDragStopped = {
                            if (drag > DISMISS_DRAG_PX) onDismiss()
                            drag = 0f
                        },
                    )
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                content = content,
            )
        }
    }
}
