package com.lumian.morningup.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow
import com.lumian.morningup.core.model.TrickSpeed
import kotlin.math.roundToInt

const val LAB_TRICK_SPEED_TAG = "lab_trick_speed"

private val KnobSize = 24.dp
private val TrackHeight = 6.dp
private val DotSize = 4.dp
private const val PASSED_DOT_ALPHA = 0.7f

/**
 * T3-1 버튼 속이기 빠르기 1~10단계 슬라이더(결정 174, 피그마 `slider/속도 10단계`, `1070:14319`).
 * 위 줄 왼쪽 '속도', 오른쪽 지금 단계 '6단계'(`primary/text`). S6 볼륨과 같은 트랙과 손잡이에
 * 단계 점 10개(지난 단계 `primary/on` 70%, 남은 단계 `text/disabled`). 아래 양끝 '느리게', '빠르게'.
 * 끌면 단계마다 끊겨 붙어요.
 */
@Composable
internal fun TrickSpeedSlider(speed: TrickSpeed, onSpeed: (TrickSpeed) -> Unit, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val levelText = stringResource(R.string.settings_lab_speed_level, speed.level)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(MorningDimens.space8)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.settings_lab_speed),
                style = typography.bodyMediumStrong,
                color = colors.textPrimary,
            )
            Text(text = levelText, style = typography.bodyMediumStrong, color = colors.primaryText)
        }
        SpeedTrack(speed = speed, onSpeed = onSpeed, description = levelText)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.settings_lab_speed_slow),
                style = typography.captionMedium,
                color = colors.textSecondary,
            )
            Text(
                text = stringResource(R.string.settings_lab_speed_fast),
                style = typography.captionMedium,
                color = colors.textSecondary,
            )
        }
        Text(
            text = stringResource(R.string.settings_lab_speed_desc),
            style = typography.bodyMedium,
            color = colors.textBody,
            modifier = Modifier.padding(top = MorningDimens.space4),
        )
    }
}

@Composable
private fun SpeedTrack(speed: TrickSpeed, onSpeed: (TrickSpeed) -> Unit, description: String) {
    val colors = MorningTheme.colors
    val min = TrickSpeed.MIN_LEVEL
    val max = TrickSpeed.MAX_LEVEL
    val span = (max - min).toFloat()
    val fraction = (speed.level - min) / span
    val latestOnSpeed by rememberUpdatedState(onSpeed)
    val latestLevel by rememberUpdatedState(speed.level)
    fun pickLevel(level: Int) {
        val target = level.coerceIn(min, max)
        if (target != latestLevel) latestOnSpeed(TrickSpeed(target))
    }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(KnobSize)
            .testTag(LAB_TRICK_SPEED_TAG)
            .pointerInput(Unit) {
                fun pick(x: Float) {
                    val knobPx = KnobSize.toPx()
                    val usable = (size.width - knobPx).coerceAtLeast(1f)
                    val f = ((x - knobPx / 2) / usable).coerceIn(0f, 1f)
                    pickLevel(min + (f * span).roundToInt())
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    pick(down.position.x)
                    horizontalDrag(down.id) { change ->
                        change.consume()
                        pick(change.position.x)
                    }
                }
            }
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(
                    current = speed.level.toFloat(),
                    range = min.toFloat()..max.toFloat(),
                    steps = max - min - 1,
                )
                setProgress { target ->
                    pickLevel(target.roundToInt())
                    true
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val travel = maxWidth - KnobSize
        val knobX = travel * fraction
        Box(
            Modifier
                .fillMaxWidth()
                .height(TrackHeight)
                .background(colors.controlTrackOff, CircleShape),
        )
        Box(
            Modifier
                .width(knobX + KnobSize / 2)
                .height(TrackHeight)
                .background(colors.primaryDefault, CircleShape),
        )
        for (level in min..max) {
            if (level == speed.level) continue
            val dotX = travel * ((level - min) / span) + KnobSize / 2 - DotSize / 2
            val dotColor = if (level < speed.level) {
                colors.primaryOn.copy(alpha = PASSED_DOT_ALPHA)
            } else {
                colors.textDisabled
            }
            Box(
                Modifier
                    .offset(x = dotX)
                    .size(DotSize)
                    .background(dotColor, CircleShape),
            )
        }
        Box(
            Modifier
                .offset(x = knobX)
                .size(KnobSize)
                .morningShadow(MorningShadows.knob, CircleShape)
                .background(colors.controlKnob, CircleShape),
        )
    }
}
