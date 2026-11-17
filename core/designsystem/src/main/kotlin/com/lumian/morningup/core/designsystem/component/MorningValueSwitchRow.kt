package com.lumian.morningup.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Minus
import com.composables.icons.lucide.Plus
import com.lumian.morningup.core.designsystem.motion.MorningHaptic
import com.lumian.morningup.core.designsystem.motion.MorningMotion
import com.lumian.morningup.core.designsystem.motion.perform
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 피그마 Value Switch Row의 Type. Level은 < >(난이도, 속도), Count는 − +(개수). */
enum class ValueSwitchType { Level, Count }

private val RowMinHeight = 56.dp
private val BoxSize = 32.dp
private val BoxRadius = 8.dp
private val BoxIcon = 20.dp
private val BoxTouch = 48.dp
private val ValueWidth = 64.dp
private val BoxGap = 8.dp

/** 꾹 누르면 처음 한 번 바뀌고, 0.4초 뒤부터 점점 빨라지며 계속 바뀌어요. */
private const val REPEAT_START_MILLIS = 400L
private const val REPEAT_FIRST_MILLIS = 150L
private const val REPEAT_FASTEST_MILLIS = 40L
private const val REPEAT_SPEED_UP = 0.85

/** 테스트에서 줄 안 버튼을 찾는 이름표. `"$tag-dec"`, `"$tag-inc"`. */
const val VALUE_DEC_SUFFIX = "-dec"
const val VALUE_INC_SUFFIX = "-inc"

/**
 * 피그마 Value Switch Row(`341:1047`). 왼쪽 이름(`Body/Medium`)과 그 아래 서브(`Caption/Medium` `text/secondary`),
 * 오른쪽 박스 버튼 2개(32, 모서리 8, `background/raised`)와 가운데 값(폭 64, `Body/Medium Strong`).
 * - 값은 늘면 위로, 줄면 아래로 굴러 바뀌어요. 바뀔 때마다 틱, 끝 값에 닿으면 `REJECT` 한 번.
 * - 끝 값에서는 그쪽 버튼이 Disabled(아이콘만 `text/disabled`)이고 눌러도 아무 일 없어요.
 * - 꾹 누르면 점점 빨라지며 계속 바뀌어요. 박스는 32로 보이지만 누르는 곳은 48이에요.
 */
@Composable
fun MorningValueSwitchRow(
    label: String,
    value: String,
    type: ValueSwitchType,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    decreaseDescription: String,
    increaseDescription: String,
    modifier: Modifier = Modifier,
    sub: String? = null,
    tag: String = "valueRow",
) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val view = LocalView.current
    var lastDirection by remember { mutableIntStateOf(0) }
    LaunchedEffect(canDecrease, canIncrease) {
        val hitEnd = (lastDirection < 0 && !canDecrease) || (lastDirection > 0 && !canIncrease)
        if (hitEnd) {
            view.perform(MorningHaptic.Reject)
            lastDirection = 0
        }
    }
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = RowMinHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = typography.bodyMedium, color = colors.textPrimary)
            if (sub != null) {
                Text(text = sub, style = typography.captionMedium, color = colors.textSecondary)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(BoxGap)) {
            BoxButton(
                icon = if (type == ValueSwitchType.Level) Lucide.ChevronLeft else Lucide.Minus,
                enabled = canDecrease,
                description = decreaseDescription,
                tag = tag + VALUE_DEC_SUFFIX,
                onStep = {
                    lastDirection = -1
                    view.perform(MorningHaptic.Tick)
                    onDecrease()
                },
            )
            RollingText(
                text = value,
                style = typography.bodyMediumStrong.copy(textAlign = TextAlign.Center),
                color = colors.textPrimary,
                direction = RollDirection.ByValue,
                modifier = Modifier.width(ValueWidth),
            )
            BoxButton(
                icon = if (type == ValueSwitchType.Level) Lucide.ChevronRight else Lucide.Plus,
                enabled = canIncrease,
                description = increaseDescription,
                tag = tag + VALUE_INC_SUFFIX,
                onStep = {
                    lastDirection = 1
                    view.perform(MorningHaptic.Tick)
                    onIncrease()
                },
            )
        }
    }
}

/** 피그마 Box Button(`343:7433`). 누르는 동안 `border/default` 칠과 0.96배, 꾹 누르면 반복. */
@Composable
private fun BoxButton(icon: ImageVector, enabled: Boolean, description: String, tag: String, onStep: () -> Unit) {
    val colors = MorningTheme.colors
    val reduceMotion = MorningTheme.reduceMotion
    val currentEnabled by rememberUpdatedState(enabled)
    val step by rememberUpdatedState(onStep)
    var pressed by remember { mutableStateOf(false) }
    val fill by animateColorAsState(
        targetValue = if (pressed && enabled) colors.controlTrackOff else colors.backgroundRaised,
        animationSpec = MorningMotion.effects(MorningMotion.EffectsFast),
        label = "boxFill",
    )
    Box(
        modifier = Modifier
            .size(BoxTouch)
            .testTag(tag)
            .semantics {
                role = Role.Button
                contentDescription = description
                if (!enabled) disabled()
                onClick {
                    if (currentEnabled) step()
                    currentEnabled
                }
            }
            .pointerInput(Unit) {
                coroutineScope {
                    val scope = this
                    awaitEachGesture {
                        awaitFirstDown()
                        if (!currentEnabled) return@awaitEachGesture
                        pressed = true
                        step()
                        val repeater = scope.launch {
                            delay(REPEAT_START_MILLIS)
                            var interval = REPEAT_FIRST_MILLIS.toDouble()
                            while (currentEnabled) {
                                step()
                                delay(interval.toLong())
                                interval = (interval * REPEAT_SPEED_UP).coerceAtLeast(REPEAT_FASTEST_MILLIS.toDouble())
                            }
                        }
                        waitForUpOrCancellation()
                        repeater.cancel()
                        pressed = false
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(BoxSize)
                .graphicsLayer {
                    val scale = if (pressed && enabled && !reduceMotion) MorningMotion.PRESS_SCALE else 1f
                    scaleX = scale
                    scaleY = scale
                }
                .background(fill, RoundedCornerShape(BoxRadius)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) colors.textPrimary else colors.textDisabled,
                modifier = Modifier.size(BoxIcon),
            )
        }
    }
}
