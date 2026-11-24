package com.lumian.morningup.feature.ring

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import kotlin.math.ceil
import kotlinx.coroutines.delay

/** 다 차는 데 걸리는 시간(10초, 결정 145 9-1). */
const val HOLD_MILLIS = 10_000

/** 손을 뗀 뒤 비우기 전에 기다리는 여유. 이 안에 다시 누르면 이어서 차요. */
const val HOLD_GRACE_MILLIS = 500L

/** 꽉 찬 고리가 다 비워지는 시간. */
private const val HOLD_DRAIN_MILLIS = 1_000

private const val FULL_CIRCLE = 360f
private const val START_ANGLE = -90f
private const val MILLIS_PER_SECOND = 1_000f
private val RingStroke = 6.dp
private val RingGap = 10.dp

/**
 * 길게 누르는 원형 버튼과 진행 고리(결정 145 9-1). 누르고 있으면 10초에 걸쳐 고리가 차고, 다 차면 진동과 함께
 * [onComplete]를 불러요. 손을 떼면 0.5초 기다렸다가 1초에 걸쳐 비워지고, 그때 [onReleasedEarly]를 불러요.
 * 누르는 동안 안 글은 남은 초예요. 톡백에서는 두 번 탭하면 10초 동안 저절로 차요(손을 대고 있기 어렵기 때문이에요).
 */
@Composable
fun HoldButton(
    label: String,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onReleasedEarly: () -> Unit = {},
    onPressed: () -> Unit = {},
) {
    val colors = MorningTheme.colors
    val view = LocalView.current
    val progress = remember { Animatable(0f) }
    var pressed by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    val complete by rememberUpdatedState(onComplete)
    val releasedEarly by rememberUpdatedState(onReleasedEarly)
    val pressStarted by rememberUpdatedState(onPressed)
    LaunchedEffect(pressed, done) {
        if (done) return@LaunchedEffect
        if (pressed) {
            pressStarted()
            val left = ((1f - progress.value) * HOLD_MILLIS).toInt()
            progress.animateTo(1f, tween(left, easing = LinearEasing))
            done = true
            view.perform(MorningHaptic.Confirm)
            complete()
        } else if (progress.value > 0f) {
            delay(HOLD_GRACE_MILLIS)
            releasedEarly()
            val drain = (progress.value * HOLD_DRAIN_MILLIS).toInt()
            progress.animateTo(0f, tween(drain, easing = LinearEasing))
        }
    }
    val holdLabel = stringResource(R.string.ring_hold_action)
    val secondsLeft = ceil((1f - progress.value) * HOLD_MILLIS / MILLIS_PER_SECOND).toInt()
    val holding = progress.value > 0f && !done
    val stateText = if (holding) stringResource(R.string.ring_hold_seconds_left, secondsLeft) else ""
    val track = colors.primaryDefault.copy(alpha = TRACK_ALPHA)
    val fill = colors.primaryDefault
    Box(
        modifier = modifier
            .size(MorningDimens.sizeButtonCircle + (RingGap + RingStroke) * 2)
            .testTag(HOLD_BUTTON_TAG)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = label
                stateDescription = stateText
                onClick(label = holdLabel) {
                    if (enabled && !done) pressed = true
                    enabled && !done
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().padding(RingStroke / 2)) {
            val stroke = RingStroke.toPx()
            val arcSize = Size(size.width, size.height)
            drawArc(track, 0f, FULL_CIRCLE, useCenter = false, topLeft = Offset.Zero, size = arcSize,
                style = Stroke(stroke))
            drawArc(fill, START_ANGLE, FULL_CIRCLE * progress.value, useCenter = false, topLeft = Offset.Zero,
                size = arcSize, style = Stroke(stroke, cap = StrokeCap.Butt))
        }
        Box(
            modifier = Modifier
                .size(MorningDimens.sizeButtonCircle)
                .background(colors.primaryDefault, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (holding) secondsLeft.toString() else label,
                style = MorningTheme.typography.titleMedium,
                color = colors.primaryOn,
            )
        }
    }
}

const val HOLD_BUTTON_TAG = "hold_button"
private const val TRACK_ALPHA = 0.3f
