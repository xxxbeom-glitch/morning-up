package com.lumian.morningup.feature.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Volume2
import com.lumian.morningup.core.designsystem.component.MorningRadioRow
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningShadows
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.morningShadow
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.RingAgain
import com.lumian.morningup.core.model.VibrationPattern
import com.lumian.morningup.core.ui.AlarmText
import com.lumian.morningup.core.ui.alarmResources
import kotlin.math.roundToInt

private const val OFF_ALPHA = 0.45f
private const val VOLUME_STEP = 5
private val KnobSize = 24.dp
private val TrackHeight = 6.dp
private val VolumeIconSize = 20.dp
private val PercentWidth = 32.dp

/**
 * 편집 화면 위에 덮는 전체 화면 하위 페이지 틀(S4 모양 그대로). 카드는 [content]에 세로로 쌓고,
 * 맨 아래 [help]가 있으면 작은 도움말을 붙여요.
 */
@Composable
internal fun DetailPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    help: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MorningTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBase)
            // 아래에 깔린 S0이 눌리지 않게 막아요.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding(),
    ) {
        MorningTopBar(title = title, onBack = onBack, backContentDescription = stringResource(R.string.edit_back))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MorningDimens.layoutScreenMargin),
            verticalArrangement = Arrangement.spacedBy(MorningDimens.layoutCardGap),
        ) {
            content()
            if (help != null) {
                Text(
                    text = help,
                    style = MorningTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = MorningDimens.space4),
                )
            }
        }
    }
}

/** S3 다시 울림(피그마 `170:1623`). '간격' 3, 5, 10, 15분, '횟수' 3번, 5번, 끌 때까지. */
@Composable
fun RingAgainPage(
    ringAgain: RingAgain,
    onBack: () -> Unit,
    onInterval: (Int) -> Unit,
    onCount: (Int?) -> Unit,
    onOff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 꺼짐(횟수 0)이면 '사용 안 함'만 고른 모양이고, 간격과 횟수는 아무것도 고르지 않은 채 흐리게 보여요.
    val on = ringAgain.count != 0
    DetailPage(
        title = stringResource(R.string.edit_ring_again),
        onBack = onBack,
        help = stringResource(R.string.edit_ring_again_help),
        modifier = modifier,
    ) {
        MorningSettingsCard(title = stringResource(R.string.edit_ring_again_interval_title)) {
            MorningRadioRow(label = stringResource(R.string.edit_off_value), selected = !on, onClick = onOff)
            EditViewModel.RING_AGAIN_INTERVALS.forEach { minutes ->
                MorningRadioRow(
                    label = stringResource(R.string.edit_ring_again_interval_option, minutes),
                    selected = on && ringAgain.intervalMinutes == minutes,
                    onClick = { onInterval(minutes) },
                )
            }
        }
        MorningSettingsCard(
            title = stringResource(R.string.edit_ring_again_count_title),
            modifier = Modifier.alpha(if (on) 1f else OFF_ALPHA),
        ) {
            EditViewModel.RING_AGAIN_COUNTS.forEach { count ->
                MorningRadioRow(
                    label = if (count == null) {
                        stringResource(R.string.edit_ring_again_count_until_off)
                    } else {
                        stringResource(R.string.edit_ring_again_count_option, count)
                    },
                    selected = on && ringAgain.count == count,
                    onClick = { onCount(count) },
                )
            }
        }
    }
}

/**
 * S0 '소리' 카드 안의 볼륨 막대(0~100, 5단위). 피그마 S6 `slider row`(`170:1786`)와 같아요.
 * 스피커 아이콘 20, 높이 6 둥근 막대(꺼짐 `control/track-off`, 켜짐 `primary/default`),
 * 흰 원 손잡이 24 + `shadow/knob`, 오른쪽 숫자 `Caption/Strong` `text/body`.
 */
@Composable
internal fun VolumeSlider(percent: Int, onVolume: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    val description = stringResource(R.string.edit_volume_percent, percent)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space12),
    ) {
        Icon(
            imageVector = Lucide.Volume2,
            contentDescription = null,
            tint = colors.textBody,
            modifier = Modifier.size(VolumeIconSize),
        )
        VolumeTrack(
            percent = percent,
            onVolume = onVolume,
            description = description,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = description,
            style = MorningTheme.typography.captionStrong,
            color = colors.textBody,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = PercentWidth),
        )
    }
}

@Composable
private fun VolumeTrack(percent: Int, onVolume: (Int) -> Unit, description: String, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    val max = EditViewModel.MAX_VOLUME
    val fraction = (percent.coerceIn(0, max) / max.toFloat())
    val latestOnVolume by rememberUpdatedState(onVolume)
    BoxWithConstraints(
        modifier = modifier
            .height(KnobSize)
            .pointerInput(Unit) {
                fun pick(x: Float) {
                    val knobPx = KnobSize.toPx()
                    val usable = (size.width - knobPx).coerceAtLeast(1f)
                    val f = ((x - knobPx / 2) / usable).coerceIn(0f, 1f)
                    latestOnVolume(((f * max) / VOLUME_STEP).roundToInt() * VOLUME_STEP)
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
                progressBarRangeInfo = ProgressBarRangeInfo(percent.toFloat(), 0f..max.toFloat(), max / VOLUME_STEP - 1)
                setProgress { target ->
                    val stepped = (target / VOLUME_STEP).roundToInt() * VOLUME_STEP
                    latestOnVolume(stepped.coerceIn(0, max))
                    true
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val travel = maxWidth - KnobSize
        val knobX = travel * fraction
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(TrackHeight)
                .background(colors.controlTrackOff, CircleShape),
        )
        Box(
            modifier = Modifier
                .width(knobX + KnobSize / 2)
                .height(TrackHeight)
                .background(colors.primaryDefault, CircleShape),
        )
        Box(
            modifier = Modifier
                .offset(x = knobX)
                .size(KnobSize)
                .morningShadow(MorningShadows.knob, CircleShape)
                .background(colors.controlKnob, CircleShape),
        )
    }
}

/** 진동 고르기. 맨 위 '사용 안 함'을 고르면 진동이 꺼지고, 모양을 고르면 켜져요. */
@Composable
fun VibrationPage(
    alarm: Alarm,
    onBack: () -> Unit,
    onPattern: (VibrationPattern) -> Unit,
    onOff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DetailPage(title = stringResource(R.string.edit_vibration_title), onBack = onBack, modifier = modifier) {
        MorningSettingsCard(title = stringResource(R.string.edit_vibration_pattern_title)) {
            MorningRadioRow(label = stringResource(R.string.edit_off_value), selected = !alarm.vibrate, onClick = onOff)
            VibrationPattern.entries.forEach { pattern ->
                MorningRadioRow(
                    label = stringResource(pattern.label()),
                    selected = alarm.vibrate && alarm.vibrationPattern == pattern,
                    onClick = { onPattern(pattern) },
                )
            }
        }
    }
}

/** S7 부드럽게 깨우기(피그마 `170:1821`). 작은 소리에서 정한 볼륨까지 올라가는 시간. */
@Composable
fun GentlePage(
    seconds: Int,
    on: Boolean,
    onBack: () -> Unit,
    onSeconds: (Int) -> Unit,
    onOff: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = alarmResources()
    DetailPage(
        title = stringResource(R.string.edit_gentle),
        onBack = onBack,
        help = stringResource(R.string.edit_gentle_help),
        modifier = modifier,
    ) {
        MorningSettingsCard(title = stringResource(R.string.edit_gentle_title)) {
            MorningRadioRow(label = stringResource(R.string.edit_off_value), selected = !on, onClick = onOff)
            Alarm.GENTLE_SECONDS.forEach { option ->
                MorningRadioRow(
                    label = AlarmText.duration(resources, option),
                    selected = on && seconds == option,
                    onClick = { onSeconds(option) },
                )
            }
        }
    }
}

/** 진동 모양 이름(S6 Radio, S0 줄 값). */
internal fun VibrationPattern.label(): Int = when (this) {
    VibrationPattern.Basic -> R.string.edit_vibration_basic
    VibrationPattern.ShortRepeat -> R.string.edit_vibration_short
    VibrationPattern.LongOnce -> R.string.edit_vibration_long
}
