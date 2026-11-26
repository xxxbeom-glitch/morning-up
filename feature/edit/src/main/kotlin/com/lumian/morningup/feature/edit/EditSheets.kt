package com.lumian.morningup.feature.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.alarm.ring.VolumePreviewer
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningChoiceChip
import com.lumian.morningup.core.designsystem.component.MorningSettingRow
import com.lumian.morningup.core.designsystem.component.MorningSheetHeader
import com.lumian.morningup.core.designsystem.component.SettingRowState
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.WakeCheckSettings

/** 알람 편집 화면 위에 띄우는 바텀시트(결정 78, 98). 페이지 대신 시트로 고르는 설정이에요. */
enum class EditSheet { Volume, RingAgain, MissionPick, WakeCheck }

/** 시트 판(좌우 24) 밖으로 8씩 넓혀 피그마 footer(좌우 16)와 맞춰요. */
private val FooterBleed = 8.dp

private fun Modifier.bleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = horizontal.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra),
    )
    layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
}

/** 시트 공통 틀: 머리, 내용(머리 아래 16), 아래 '완료' 버튼(위 24). */
@Composable
internal fun EditSheetFrame(
    title: String,
    description: String,
    onDone: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    MorningSheetHeader(
        title = title,
        description = description,
        modifier = Modifier.padding(top = MorningDimens.space8),
    )
    Spacer(Modifier.height(MorningDimens.space16))
    Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space4), content = content)
    Spacer(Modifier.height(MorningDimens.space24))
    MorningButton(
        text = stringResource(R.string.edit_done),
        onClick = onDone,
        modifier = Modifier.fillMaxWidth().bleed(FooterBleed),
    )
}

/** 줄 이름(`Body/Medium` `text/body`) 아래 12에 Choice Chip을 줄 너비에 꽉 차게 나눠 둬요. 위아래 8. */
@Composable
internal fun <T> ChoiceGroup(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(vertical = MorningDimens.space8)) {
        Text(text = label, style = MorningTheme.typography.bodyMedium, color = MorningTheme.colors.textBody)
        Spacer(Modifier.height(MorningDimens.space12))
        Row(horizontalArrangement = Arrangement.spacedBy(ChipGap)) {
            options.forEach { option ->
                MorningChoiceChip(
                    label = optionLabel(option),
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val ChipGap = 6.dp

/** '볼륨 천천히 올리기' 선택지. 0은 '안 함'(꺼짐). */
private val GentleOptions = listOf(0) + Alarm.GENTLE_SECONDS

/**
 * S6 볼륨 시트(피그마 `509:202`, 결정 98): 볼륨 막대, '진동' 토글 줄(아래 글 없음),
 * '볼륨 천천히 올리기' 안 함/10초/15초/20초. 고르는 즉시 편집 중인 알람에 반영돼요.
 */
@Composable
internal fun VolumeSheetContent(alarm: Alarm, actions: EditActions, onDone: () -> Unit) {
    // 막대를 움직이거나 '볼륨 천천히 올리기'를 누를 때마다 미리 들려줘요(11-18 사용자 지시). 시트를 닫으면 멈춰요.
    val context = LocalContext.current
    val previewer = remember(context) { VolumePreviewer(context.applicationContext) }
    DisposableEffect(previewer) { onDispose { previewer.stop() } }
    EditSheetFrame(
        title = stringResource(R.string.edit_volume_title),
        description = stringResource(R.string.edit_volume_sheet_desc),
        onDone = onDone,
    ) {
        VolumeSlider(
            percent = alarm.volumePercent,
            onVolume = { percent ->
                actions.onVolume(percent)
                previewer.volume(alarm.soundId, percent)
            },
            modifier = Modifier.padding(vertical = MorningDimens.space8),
        )
        MorningSettingRow(
            label = stringResource(R.string.edit_vibration_title),
            value = null,
            state = if (alarm.vibrate) SettingRowState.On else SettingRowState.Off,
            onCheckedChange = actions.onVibrate,
            modifier = Modifier.height(SheetToggleRowHeight),
        )
        ChoiceGroup(
            label = stringResource(R.string.edit_gentle),
            options = GentleOptions,
            selected = if (alarm.gentleWake) alarm.gentleSeconds else 0,
            optionLabel = { s ->
                if (s == 0) stringResource(R.string.edit_gentle_none) else stringResource(R.string.edit_seconds, s)
            },
            onSelect = { s ->
                if (s == 0) actions.onGentle(false) else actions.onGentleSeconds(s)
                previewer.gentle(alarm.soundId, alarm.volumePercent, s)
            },
        )
    }
}

/** 피그마 시트 안 토글 줄 높이 54. */
private val SheetToggleRowHeight = 54.dp

/** '볼륨' 줄 값: '80%, 진동, 볼륨 천천히 올리기'처럼 볼륨 다음에 켜진 것만 이어 써요(결정 98). */
@Composable
internal fun volumeValue(alarm: Alarm): String {
    val parts = buildList {
        add(stringResource(R.string.edit_volume_percent, alarm.volumePercent))
        if (alarm.vibrate) add(stringResource(R.string.edit_vibration_title))
        if (alarm.gentleWake) add(stringResource(R.string.edit_gentle))
    }
    return parts.joinToString(", ")
}

/**
 * 피그마 S3, S4 줄: 왼쪽 이름(`Body/Medium` `text/body`), 오른쪽 Choice Chip(너비 60, 사이 6). 위아래 8(결정 81).
 * 이름은 늘 한 줄이에요. 칩이 4개라 좁은 화면(Z플립5)에서 자리가 모자라면 칩만 조금 줄어요(ISS-058).
 */
@Composable
internal fun <T> ChipRow(
    label: String,
    options: List<T>,
    selected: T?,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = MorningDimens.space8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MorningTheme.typography.bodyMedium,
            color = MorningTheme.colors.textBody,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(end = MorningDimens.space12),
        )
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(ChipGap, Alignment.End),
        ) {
            options.forEach { option ->
                MorningChoiceChip(
                    label = optionLabel(option),
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    modifier = Modifier.weight(1f, fill = false).width(RowChipWidth),
                )
            }
        }
    }
}

private val RowChipWidth = 60.dp

/** S3 다시 울림 시트(피그마 `461:7911`, 결정 82): 간격 1/3/5/10분, 횟수 무제한/1/3/5회. */
@Composable
internal fun RingAgainSheetContent(alarm: Alarm, actions: EditActions, onDone: () -> Unit) {
    val ringAgain = alarm.ringAgain
    val on = ringAgain.count != 0
    EditSheetFrame(
        title = stringResource(R.string.edit_ring_again),
        description = stringResource(R.string.edit_ring_again_sheet_desc),
        onDone = onDone,
    ) {
        ChipRow(
            label = stringResource(R.string.edit_ring_again_interval_title),
            options = EditViewModel.RING_AGAIN_INTERVALS,
            selected = ringAgain.intervalMinutes.takeIf { on },
            optionLabel = { stringResource(R.string.edit_ring_again_interval_option, it) },
            onSelect = actions.onRingAgainInterval,
        )
        ChipRow(
            label = stringResource(R.string.edit_ring_again_count_title),
            options = EditViewModel.RING_AGAIN_COUNTS,
            selected = if (on) ringAgain.count else NOT_SELECTED,
            optionLabel = { c ->
                if (c == null) {
                    stringResource(R.string.edit_ring_again_unlimited)
                } else {
                    stringResource(R.string.edit_ring_again_count_sheet, c)
                }
            },
            onSelect = actions.onRingAgainCount,
        )
    }
}

/** 꺼져 있을 때 횟수 칸을 아무것도 고르지 않은 모양으로 두는 값(선택지에 없는 수). */
private const val NOT_SELECTED = -1

/** '한 번 더 확인' 줄의 '안 함' 칸 값. 고르면 한 번 더 확인(W3)을 꺼요. */
private const val RECHECK_NONE = 0

private val RecheckOptions = listOf(RECHECK_NONE) + WakeCheckSettings.RECHECK_COUNTS

/**
 * S4 다시 잠들기 방지 시트, 두 줄(결정 183 A안). ① '간격' 3, 5, 10분 후 ② '한 번 더 확인' 안 함, 1번, 2번, 3번.
 * ② 줄은 설정 > 실험실 '한 번 더 확인'을 켰을 때만 보여요(결정 184). 줄 아래 설명 글은 없어요(TSK-247). 고르는 즉시 편집 중인 알람에 반영돼요. W2 대답할 시간 1분, W3까지 3분, W3 대답할 시간 1분은 고정이에요.
 */
@Composable
internal fun WakeCheckSheetContent(alarm: Alarm, labRecheck: Boolean, actions: EditActions, onDone: () -> Unit) {
    val check = alarm.wakeCheck
    EditSheetFrame(
        title = stringResource(R.string.edit_wake_check),
        description = stringResource(R.string.edit_wake_check_sheet_desc),
        onDone = onDone,
    ) {
        ChipRow(
            label = stringResource(R.string.edit_wake_check_ask),
            options = WakeCheckSettings.ASK_MINUTES,
            selected = check.askAfterMinutes,
            optionLabel = { stringResource(R.string.edit_minutes_after, it) },
            onSelect = actions.onWakeCheckAsk,
        )
        // 결정 184: '한 번 더 확인' 줄은 실험실에서 켰을 때만 보여요.
        if (labRecheck) RecheckRow(check.recheckEnabled, check.recheckCount, actions)
    }
}

@Composable
private fun RecheckRow(enabled: Boolean, count: Int, actions: EditActions) {
    ChipRow(
        label = stringResource(R.string.edit_recheck),
        options = RecheckOptions,
        selected = if (enabled) count else RECHECK_NONE,
        optionLabel = { n ->
            if (n == RECHECK_NONE) stringResource(R.string.edit_recheck_none) else stringResource(R.string.edit_times, n)
        },
        onSelect = { n -> if (n == RECHECK_NONE) actions.onRecheck(false) else actions.onRecheckCount(n) },
    )
}

/**
 * '다시 잠들기 방지' 줄 값: '3분 후, 한 번 더 확인 1번'. 한 번 더 확인이 꺼져 있거나 실험실에서 꺼 두었으면 '3분 후'예요(결정 184).
 */
@Composable
internal fun wakeCheckValue(alarm: Alarm, labRecheck: Boolean): String {
    val check = alarm.wakeCheck
    return if (check.recheckEnabled && labRecheck) {
        stringResource(R.string.edit_wake_check_value, check.askAfterMinutes, check.recheckCount)
    } else {
        stringResource(R.string.edit_wake_check_value_no_recheck, check.askAfterMinutes)
    }
}
