package com.lumian.morningup.feature.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningChoiceChip
import com.lumian.morningup.core.designsystem.component.MorningSettingRow
import com.lumian.morningup.core.designsystem.component.MorningSheetHeader
import com.lumian.morningup.core.designsystem.component.SettingRowState
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.Alarm

/** 알람 편집 화면 위에 띄우는 바텀시트(결정 78, 98). 페이지 대신 시트로 고르는 설정이에요. */
enum class EditSheet { Volume, RingAgain, WakeCheck }

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
    MorningSheetHeader(title = title, description = description, modifier = Modifier.padding(top = MorningDimens.space8))
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
    EditSheetFrame(
        title = stringResource(R.string.edit_volume_title),
        description = stringResource(R.string.edit_volume_sheet_desc),
        onDone = onDone,
    ) {
        VolumeSlider(
            percent = alarm.volumePercent,
            onVolume = actions.onVolume,
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
            onSelect = { s -> if (s == 0) actions.onGentle(false) else actions.onGentleSeconds(s) },
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
