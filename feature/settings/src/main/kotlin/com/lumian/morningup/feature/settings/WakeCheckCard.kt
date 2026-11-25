package com.lumian.morningup.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningChoiceChip
import com.lumian.morningup.core.designsystem.component.MorningModalSheet
import com.lumian.morningup.core.designsystem.component.MorningOverlay
import com.lumian.morningup.core.designsystem.component.MorningSettingRow
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.MorningSheetHeader
import com.lumian.morningup.core.designsystem.component.SettingRowState
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.WakeCheckSettings

/** T1 카드에서 여는 시트(결정 145). */
enum class WakeCheckSheet { Ask, Recheck }

const val WAKE_CHECK_ASK_SHEET_TAG = "wake_check_ask_sheet"
const val WAKE_CHECK_RECHECK_SHEET_TAG = "wake_check_recheck_sheet"

private const val SECONDS_PER_MINUTE = 60
private val ChipGap = 6.dp
private val RowChipWidth = 60.dp

/** 시트 판(좌우 24) 밖으로 8씩 넓혀 피그마 footer(좌우 16)와 맞춰요(편집 시트와 같음). */
internal val FooterBleed = 8.dp

/** '알람' 카드 아래 '다시 잠들기 방지' 카드('묻는 때', '한 번 더 확인')와 아래 작은 글(결정 145, 164, 시안 t1). */
@Composable
fun WakeCheckCard(viewModel: WakeCheckSettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val noGuarded by viewModel.noGuardedAlarm.collectAsStateWithLifecycle()
    WakeCheckCardContent(
        settings = settings,
        noGuardedAlarm = noGuarded,
        onAskAfter = viewModel::setAskAfter,
        onAnswer = viewModel::setAnswer,
        onRecheck = viewModel::setRecheck,
        onRecheckMinutes = viewModel::setRecheckMinutes,
    )
}

/** 상태 없는 카드. 시트는 하단 메뉴 위에 떠야 해서 [MorningOverlay]로 띄워요. */
@Composable
fun WakeCheckCardContent(
    settings: WakeCheckSettings,
    onAskAfter: (Int) -> Unit,
    onAnswer: (Int) -> Unit,
    onRecheck: (Boolean) -> Unit,
    onRecheckMinutes: (Int) -> Unit,
    noGuardedAlarm: Boolean = false,
) {
    var sheet by rememberSaveable { mutableStateOf<WakeCheckSheet?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space12)) {
        MorningSettingsCard(title = stringResource(R.string.settings_wake_check_card)) {
            NavRows {
                NavRow(
                    label = stringResource(R.string.settings_wake_check),
                    value = wakeCheckValue(settings),
                    onClick = { sheet = WakeCheckSheet.Ask },
                )
            }
            // 스위치는 켜고 끄기, 글자 쪽은 T1-2 시트를 열어요.
            // 실험실 여러 번 묻기를 켜면 켜진 채 잠겨요(결정 146 3절).
            val multi = settings.lab.multiAskEnabled
            MorningSettingRow(
                label = stringResource(R.string.settings_recheck),
                value = if (multi) {
                    stringResource(R.string.settings_recheck_lab, settings.lab.multiAskCount)
                } else {
                    stringResource(R.string.settings_recheck_desc)
                },
                state = if (settings.recheckOn) SettingRowState.On else SettingRowState.Off,
                onCheckedChange = onRecheck,
                onClick = { sheet = WakeCheckSheet.Recheck },
                modifier = Modifier.heightIn(min = SettingsToggleRowHeight),
                toggleLocked = multi,
            )
        }
        Text(
            // 다시 잠들기 방지를 켠 알람이 없으면 왜 안 묻는지 알려 줘요(결정 164).
            text = stringResource(
                if (noGuardedAlarm) R.string.settings_wake_check_none else R.string.settings_wake_check_note,
            ),
            style = MorningTheme.typography.captionMedium,
            color = MorningTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = MorningDimens.layoutCardPadding),
        )
    }
    val open = sheet
    if (open != null) {
        MorningOverlay {
            MorningModalSheet(
                visible = true,
                onDismiss = { sheet = null },
                tag = if (open == WakeCheckSheet.Ask) WAKE_CHECK_ASK_SHEET_TAG else WAKE_CHECK_RECHECK_SHEET_TAG,
            ) {
                when (open) {
                    WakeCheckSheet.Ask -> AskSheet(settings, onAskAfter, onAnswer) { sheet = null }
                    WakeCheckSheet.Recheck -> RecheckSheet(settings, onRecheckMinutes) { sheet = null }
                }
            }
        }
    }
}

/** '묻는 때' 줄 값: '3분 후, 1분'. */
@Composable
internal fun wakeCheckValue(settings: WakeCheckSettings): String =
    stringResource(R.string.settings_wake_check_value, settings.askAfterMinutes, answerLabel(settings.answerSeconds))

@Composable
private fun answerLabel(seconds: Int): String = if (seconds < SECONDS_PER_MINUTE) {
    stringResource(R.string.settings_seconds, seconds)
} else {
    stringResource(R.string.settings_minutes, seconds / SECONDS_PER_MINUTE)
}

/** T1-1: 다시 물어보기 3/5/10분 후, 대답할 시간 30초/1분/3분. */
@Composable
private fun ColumnScope.AskSheet(
    settings: WakeCheckSettings,
    onAskAfter: (Int) -> Unit,
    onAnswer: (Int) -> Unit,
    onDone: () -> Unit,
) {
    SheetFrame(
        title = stringResource(R.string.settings_wake_check),
        description = stringResource(R.string.settings_wake_check_sheet_desc),
        onDone = onDone,
    ) {
        ChipRow(
            label = stringResource(R.string.settings_wake_check_ask),
            options = WakeCheckSettings.ASK_MINUTES,
            selected = settings.askAfterMinutes,
            optionLabel = { stringResource(R.string.settings_minutes_after, it) },
            onSelect = onAskAfter,
        )
        ChipRow(
            label = stringResource(R.string.settings_wake_check_answer),
            options = WakeCheckSettings.ANSWER_SECONDS,
            selected = settings.answerSeconds,
            optionLabel = { answerLabel(it) },
            onSelect = onAnswer,
        )
    }
}

/** T1-2: 확인할 시간 3/5/10분. */
@Composable
private fun ColumnScope.RecheckSheet(settings: WakeCheckSettings, onMinutes: (Int) -> Unit, onDone: () -> Unit) {
    SheetFrame(
        title = stringResource(R.string.settings_recheck),
        description = stringResource(R.string.settings_recheck_sheet_desc),
        onDone = onDone,
    ) {
        ChipRow(
            label = stringResource(R.string.settings_recheck_minutes),
            options = WakeCheckSettings.RECHECK_MINUTES,
            selected = settings.recheckMinutes,
            optionLabel = { stringResource(R.string.settings_minutes, it) },
            onSelect = onMinutes,
        )
    }
}

/** 시트 공통 틀: 머리, 내용(머리 아래 16), 아래 '완료' 버튼(위 24). 편집 시트와 같아요. */
@Composable
internal fun SheetFrame(
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
        text = stringResource(R.string.pl_done),
        onClick = onDone,
        modifier = Modifier.fillMaxWidth().bleed(FooterBleed).testTag(WAKE_CHECK_DONE_TAG),
    )
}

const val WAKE_CHECK_DONE_TAG = "wake_check_done"

/** 왼쪽 이름, 오른쪽 Choice Chip(너비 60, 사이 6). 위아래 8. */
@Composable
private fun <T> ChipRow(
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
            modifier = Modifier.weight(1f).padding(end = MorningDimens.space12),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(ChipGap)) {
            options.forEach { option ->
                MorningChoiceChip(
                    label = optionLabel(option),
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    modifier = Modifier.width(RowChipWidth),
                )
            }
        }
    }
}

internal fun Modifier.bleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = horizontal.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra),
    )
    layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
}
