package com.lumian.morningup.feature.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Volume2
import com.lumian.morningup.core.designsystem.component.MorningRadioRow
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.MorningToggle
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.RingAgain
import com.lumian.morningup.core.model.VibrationPattern
import com.lumian.morningup.core.ui.AlarmText
import com.lumian.morningup.core.ui.alarmResources
import kotlin.math.roundToInt

private const val OFF_ALPHA = 0.45f
private const val VOLUME_STEPS = 19
private val VolumeIconSize = 20.dp
private val PercentWidth = 44.dp

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
    modifier: Modifier = Modifier,
) {
    // 꺼짐(횟수 0)이면 처음 값 3번을 고른 모양으로 보여요(S0 줄과 같게).
    val shownCount = ringAgain.count.takeIf { it != 0 } ?: EditViewModel.RING_AGAIN_COUNTS.first()
    DetailPage(
        title = stringResource(R.string.edit_ring_again),
        onBack = onBack,
        help = stringResource(R.string.edit_ring_again_help),
        modifier = modifier,
    ) {
        MorningSettingsCard(title = stringResource(R.string.edit_ring_again_interval_title)) {
            EditViewModel.RING_AGAIN_INTERVALS.forEach { minutes ->
                MorningRadioRow(
                    label = stringResource(R.string.edit_ring_again_interval_option, minutes),
                    selected = ringAgain.intervalMinutes == minutes,
                    onClick = { onInterval(minutes) },
                )
            }
        }
        MorningSettingsCard(title = stringResource(R.string.edit_ring_again_count_title)) {
            EditViewModel.RING_AGAIN_COUNTS.forEach { count ->
                MorningRadioRow(
                    label = if (count == null) {
                        stringResource(R.string.edit_ring_again_count_until_off)
                    } else {
                        stringResource(R.string.edit_ring_again_count_option, count)
                    },
                    selected = shownCount == count,
                    onClick = { onCount(count) },
                )
            }
        }
    }
}

/** S6 볼륨과 진동(피그마 `170:1778`). 볼륨 막대, 진동 켬과 끔, 진동 모양 셋. */
@Composable
fun VolumePage(
    alarm: Alarm,
    onBack: () -> Unit,
    onVolume: (Int) -> Unit,
    onVibrate: (Boolean) -> Unit,
    onPattern: (VibrationPattern) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MorningTheme.colors
    DetailPage(title = stringResource(R.string.edit_volume), onBack = onBack, modifier = modifier) {
        MorningSettingsCard(title = stringResource(R.string.edit_volume_title)) {
            val description = stringResource(R.string.edit_volume_percent, alarm.volumePercent)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MorningDimens.space12),
            ) {
                Icon(
                    imageVector = Lucide.Volume2,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(VolumeIconSize),
                )
                Slider(
                    value = alarm.volumePercent.toFloat(),
                    onValueChange = { onVolume(it.roundToInt()) },
                    valueRange = 0f..EditViewModel.MAX_VOLUME.toFloat(),
                    steps = VOLUME_STEPS,
                    colors = SliderDefaults.colors(
                        thumbColor = colors.primaryDefault,
                        activeTrackColor = colors.primaryDefault,
                        inactiveTrackColor = colors.controlTrackOff,
                        activeTickColor = colors.primaryDefault.copy(alpha = 0f),
                        inactiveTickColor = colors.controlTrackOff.copy(alpha = 0f),
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = description },
                )
                Text(
                    text = description,
                    style = MorningTheme.typography.bodyMediumStrong,
                    color = colors.textPrimary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(min = PercentWidth),
                )
            }
        }
        MorningSettingsCard(title = stringResource(R.string.edit_vibration_title)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.edit_vibration_sub),
                    style = MorningTheme.typography.bodyMedium,
                    color = colors.textBody,
                    modifier = Modifier.weight(1f),
                )
                MorningToggle(checked = alarm.vibrate, onCheckedChange = onVibrate)
            }
            Column(Modifier.alpha(if (alarm.vibrate) 1f else OFF_ALPHA)) {
                VibrationPattern.entries.forEach { pattern ->
                    MorningRadioRow(
                        label = stringResource(pattern.label()),
                        selected = alarm.vibrationPattern == pattern,
                        onClick = { onPattern(pattern) },
                    )
                }
            }
        }
    }
}

/** S7 부드럽게 깨우기(피그마 `170:1821`). 작은 소리에서 정한 볼륨까지 올라가는 시간. */
@Composable
fun GentlePage(
    seconds: Int,
    onBack: () -> Unit,
    onSeconds: (Int) -> Unit,
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
            Alarm.GENTLE_SECONDS.forEach { option ->
                MorningRadioRow(
                    label = AlarmText.duration(resources, option),
                    selected = seconds == option,
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
