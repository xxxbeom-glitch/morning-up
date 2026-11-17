package com.lumian.morningup.feature.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningDayChip
import com.lumian.morningup.core.designsystem.component.MorningDialog
import com.lumian.morningup.core.designsystem.component.MorningPreviewButton
import com.lumian.morningup.core.designsystem.component.MorningSettingRow
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.component.MorningWheelPicker
import com.lumian.morningup.core.designsystem.component.SettingRowState
import com.lumian.morningup.core.designsystem.component.WheelItemHeight
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.ui.AlarmText
import com.lumian.morningup.core.ui.alarmResources
import java.time.DayOfWeek

/** 편집 화면의 하위 화면. S4(WakeCheck)만 열리고 나머지는 7단계 이후예요. */
enum class EditDetail { Mission, Preview, RingAgain, WakeCheck, PowerOff, Sound, Volume, Gentle, Background }

/** 피그마 S0의 요일 순서(일~토). */
private val DayOrder = listOf(
    DayOfWeek.SUNDAY,
    DayOfWeek.MONDAY,
    DayOfWeek.TUESDAY,
    DayOfWeek.WEDNESDAY,
    DayOfWeek.THURSDAY,
    DayOfWeek.FRIDAY,
    DayOfWeek.SATURDAY,
)

private const val HOURS_HALF_DAY = 12
private const val MINUTES_PER_HOUR = 60
private const val RING_AGAIN_SHOWN_WHEN_OFF = 3
private val PeriodWidth = 96.dp
private val NumberWidth = 72.dp
private val ColonWidth = 24.dp

/** 저장 버튼 위로 내용이 비쳐 사라지는 높이. */
private val SaveFadeHeight = 24.dp

/**
 * 알람 편집 S0. [onSaved]는 저장한 알람 id를, [onClose]는 저장 없이 닫을 때 불러요.
 * [onOpenDetail]은 하위 화면을 열 자리예요(지금은 아무 일 없음).
 */
@Composable
fun AlarmEditRoute(
    onSaved: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenDetail: (EditDetail) -> Unit = {},
    viewModel: EditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saved by rememberUpdatedState(onSaved)
    val close by rememberUpdatedState(onClose)
    LaunchedEffect(state.exit) {
        when (val exit = state.exit) {
            is EditExit.Saved -> saved(exit.alarmId)
            EditExit.Discarded -> close()
            null -> Unit
        }
    }
    EditScreen(
        state = state,
        actions = remember(viewModel) {
            EditActions(
                onBack = viewModel::onBack,
                onTime = viewModel::setTime,
                onToggleDay = viewModel::toggleDay,
                onSkipHolidays = viewModel::setSkipHolidays,
                // 디버그 빌드만 미션을 켜면 Quick Math가 들어가요(미션 고르기는 5단계).
                onMissions = { on -> viewModel.setMissionsEnabled(on, allowDebugQuickMath = BuildConfig.DEBUG) },
                onRingAgain = viewModel::setRingAgain,
                onWakeCheck = viewModel::setWakeCheck,
                onPowerOff = viewModel::setPowerOffGuard,
                onSound = viewModel::setSound,
                onVibrate = viewModel::setVibrate,
                onGentle = viewModel::setGentleWake,
                onBackground = viewModel::setBackground,
                onSave = viewModel::save,
                onKeepEditing = viewModel::keepEditing,
                onDiscard = viewModel::discard,
                onWakeCheckAsk = viewModel::setWakeCheckAsk,
                onWakeCheckAnswer = viewModel::setWakeCheckAnswer,
                onCloseDetail = viewModel::closeDetail,
            )
        },
        onOpenDetail = { detail ->
            if (detail == EditDetail.WakeCheck) viewModel.openDetail(detail) else onOpenDetail(detail)
        },
        modifier = modifier,
    )
}

/** 편집 화면이 부르는 일 묶음. */
data class EditActions(
    val onBack: () -> Unit = {},
    val onTime: (hour: Int, minute: Int) -> Unit = { _, _ -> },
    val onToggleDay: (DayOfWeek) -> Unit = {},
    val onSkipHolidays: (Boolean) -> Unit = {},
    val onMissions: (Boolean) -> Unit = {},
    val onRingAgain: (Boolean) -> Unit = {},
    val onWakeCheck: (Boolean) -> Unit = {},
    val onPowerOff: (Boolean) -> Unit = {},
    val onSound: (Boolean) -> Unit = {},
    val onVibrate: (Boolean) -> Unit = {},
    val onGentle: (Boolean) -> Unit = {},
    val onBackground: (Boolean) -> Unit = {},
    val onSave: () -> Unit = {},
    val onKeepEditing: () -> Unit = {},
    val onDiscard: () -> Unit = {},
    val onWakeCheckAsk: (Int) -> Unit = {},
    val onWakeCheckAnswer: (Int) -> Unit = {},
    val onCloseDetail: () -> Unit = {},
)

/** S0 알람 편집(미션 있음, 없음)과 S0-1 저장 안 하고 나가기. 상태와 콜백만 받아요. */
@Composable
fun EditScreen(
    state: EditUiState,
    actions: EditActions,
    modifier: Modifier = Modifier,
    onOpenDetail: (EditDetail) -> Unit = {},
) {
    BackHandler(onBack = actions.onBack)
    val colors = MorningTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBase),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            MorningTopBar(
                title = stringResource(R.string.edit_title),
                onBack = actions.onBack,
                backContentDescription = stringResource(R.string.edit_back),
            )
            if (!state.loading) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = MorningDimens.layoutScreenMargin),
                    verticalArrangement = Arrangement.spacedBy(MorningDimens.layoutCardGap),
                ) {
                    UntilLine(state)
                    TimeWheels(state.draft, actions.onTime)
                    RepeatCard(state.draft, actions)
                    MissionCard(state, actions, onOpenDetail)
                    SoundCard(state.draft, actions, onOpenDetail)
                    // 고정된 저장 버튼에 마지막 카드가 가리지 않게 비워요.
                    Spacer(Modifier.height(MorningDimens.sizeButtonLg + MorningDimens.space40))
                }
            }
        }
        SaveBar(
            enabled = !state.loading && !state.saving,
            onSave = actions.onSave,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        if (state.confirmExit) {
            MorningDialog(
                title = stringResource(R.string.edit_exit_title),
                message = stringResource(R.string.edit_exit_message),
                dismissLabel = stringResource(R.string.edit_exit_keep),
                confirmLabel = stringResource(R.string.edit_exit_leave),
                onDismiss = actions.onKeepEditing,
                onConfirm = actions.onDiscard,
                danger = true,
            )
        }
        if (state.detail == EditDetail.WakeCheck) {
            WakeCheckPage(
                wakeCheck = state.draft.wakeCheck,
                onBack = actions.onCloseDetail,
                onAsk = actions.onWakeCheckAsk,
                onAnswer = actions.onWakeCheckAnswer,
            )
        }
    }
}

@Composable
private fun UntilLine(state: EditUiState) {
    val resources = alarmResources()
    val text = state.until?.let { stringResource(R.string.edit_until, AlarmText.until(resources, it)) }.orEmpty()
    Text(
        text = text,
        style = MorningTheme.typography.bodyMedium,
        color = MorningTheme.colors.textBody,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MorningDimens.space16),
    )
}

/** 오전오후, 시, 분 드럼롤. 가운데 줄 뒤에 흰 띠가 있어요. */
@Composable
private fun TimeWheels(alarm: Alarm, onTime: (Int, Int) -> Unit) {
    val resources = alarmResources()
    val periods = remember(resources) {
        listOf(AlarmText.period(resources, pm = false), AlarmText.period(resources, pm = true))
    }
    val hours = remember { (1..HOURS_HALF_DAY).map { it.toString() } }
    val minutes = remember { (0 until MINUTES_PER_HOUR).map { it.toString().padStart(2, '0') } }
    val hour24 = alarm.time.hour
    val minute = alarm.time.minute
    val pm = hour24 >= HOURS_HALF_DAY
    val hour12 = AlarmText.hour12(hour24)
    fun to24(h12: Int, isPm: Boolean) = h12 % HOURS_HALF_DAY + if (isPm) HOURS_HALF_DAY else 0

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WheelItemHeight)
                .background(MorningTheme.colors.surfaceCard, RoundedCornerShape(MorningDimens.radiusTile)),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            MorningWheelPicker(
                items = periods,
                selectedIndex = if (pm) 1 else 0,
                onSelect = { onTime(to24(hour12, it == 1), minute) },
                width = PeriodWidth,
                contentDescription = stringResource(R.string.edit_period),
            )
            Spacer(Modifier.width(MorningDimens.space8))
            MorningWheelPicker(
                items = hours,
                selectedIndex = hour12 - 1,
                onSelect = { onTime(to24(it + 1, pm), minute) },
                width = NumberWidth,
                looping = true,
                contentDescription = stringResource(R.string.edit_hour),
            )
            Text(
                text = stringResource(R.string.edit_colon),
                style = MorningTheme.typography.displayMedium,
                color = MorningTheme.colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(ColonWidth),
            )
            MorningWheelPicker(
                items = minutes,
                selectedIndex = minute,
                onSelect = { onTime(hour24, it) },
                width = NumberWidth,
                looping = true,
                contentDescription = stringResource(R.string.edit_minute),
            )
        }
    }
}

@Composable
private fun RepeatCard(alarm: Alarm, actions: EditActions) {
    val resources = alarmResources()
    MorningSettingsCard(title = stringResource(R.string.edit_card_repeat)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DayOrder.forEach { day ->
                val label = AlarmText.dayShort(resources, day)
                MorningDayChip(
                    label = label,
                    selected = day in alarm.repeatDays,
                    onClick = { actions.onToggleDay(day) },
                    contentDescription = stringResource(R.string.edit_day_description, label),
                )
            }
        }
        MorningSettingRow(
            label = stringResource(R.string.edit_skip_holidays),
            value = stringResource(R.string.edit_skip_holidays_value),
            state = onOff(alarm.skipHolidays),
            onCheckedChange = actions.onSkipHolidays,
        )
    }
}

@Composable
private fun MissionCard(state: EditUiState, actions: EditActions, onOpenDetail: (EditDetail) -> Unit) {
    val alarm = state.draft
    val resources = alarmResources()
    MorningSettingsCard(title = stringResource(R.string.edit_card_mission)) {
        MorningSettingRow(
            label = stringResource(R.string.edit_mission),
            value = AlarmText.missions(resources, alarm.missionTypes),
            state = onOff(state.hasMissions),
            onCheckedChange = actions.onMissions,
            onClick = { onOpenDetail(EditDetail.Mission) },
        )
        if (state.hasMissions) {
            MorningPreviewButton(
                text = stringResource(R.string.edit_preview),
                onClick = { onOpenDetail(EditDetail.Preview) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val ringAgain = alarm.ringAgain
        val shownCount = ringAgain.count?.takeIf { it > 0 } ?: RING_AGAIN_SHOWN_WHEN_OFF
        MorningSettingRow(
            label = stringResource(R.string.edit_ring_again),
            value = if (ringAgain.count == null) {
                stringResource(R.string.edit_ring_again_until_off, ringAgain.intervalMinutes)
            } else {
                stringResource(R.string.edit_ring_again_value, ringAgain.intervalMinutes, shownCount)
            },
            state = onOff(ringAgain.count != 0),
            onCheckedChange = actions.onRingAgain,
            onClick = { onOpenDetail(EditDetail.RingAgain) },
        )
        MorningSettingRow(
            label = stringResource(R.string.edit_wake_check),
            value = stringResource(R.string.edit_wake_check_value, alarm.wakeCheck.askAfterMinutes),
            state = onOff(alarm.wakeCheck.enabled),
            onCheckedChange = actions.onWakeCheck,
            onClick = { onOpenDetail(EditDetail.WakeCheck) },
        )
        MorningSettingRow(
            label = stringResource(R.string.edit_power_off),
            value = stringResource(R.string.edit_power_off_value),
            state = if (state.hasMissions) onOff(alarm.powerOffGuard) else SettingRowState.Disabled,
            onCheckedChange = actions.onPowerOff,
            onClick = { onOpenDetail(EditDetail.PowerOff) },
        )
    }
}

@Composable
private fun SoundCard(alarm: Alarm, actions: EditActions, onOpenDetail: (EditDetail) -> Unit) {
    MorningSettingsCard(title = stringResource(R.string.edit_card_sound)) {
        MorningSettingRow(
            label = stringResource(R.string.edit_sound),
            value = stringResource(R.string.edit_sound_default),
            state = onOff(alarm.soundId != Alarm.SILENT_SOUND_ID),
            onCheckedChange = actions.onSound,
            onClick = { onOpenDetail(EditDetail.Sound) },
        )
        MorningSettingRow(
            label = stringResource(R.string.edit_volume),
            value = stringResource(
                if (alarm.vibrate) R.string.edit_volume_vibrate else R.string.edit_volume_no_vibrate,
                alarm.volumePercent,
            ),
            state = onOff(alarm.vibrate),
            onCheckedChange = actions.onVibrate,
            onClick = { onOpenDetail(EditDetail.Volume) },
        )
        MorningSettingRow(
            label = stringResource(R.string.edit_gentle),
            value = stringResource(R.string.edit_gentle_value),
            state = onOff(alarm.gentleWake),
            onCheckedChange = actions.onGentle,
            onClick = { onOpenDetail(EditDetail.Gentle) },
        )
        MorningSettingRow(
            label = stringResource(R.string.edit_background),
            value = stringResource(R.string.edit_background_value),
            state = onOff(alarm.background != Alarm.BACKGROUND_PLAIN),
            onCheckedChange = actions.onBackground,
            onClick = { onOpenDetail(EditDetail.Background) },
        )
    }
}

/** 화면 아래 고정 저장 버튼. 위쪽은 바탕색으로 서서히 덮어요. */
@Composable
private fun SaveBar(enabled: Boolean, onSave: () -> Unit, modifier: Modifier = Modifier) {
    val base = MorningTheme.colors.backgroundBase
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(SaveFadeHeight)
                .background(Brush.verticalGradient(listOf(base.copy(alpha = 0f), base))),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(base)
                .navigationBarsPadding()
                .padding(
                    start = MorningDimens.layoutScreenMargin,
                    end = MorningDimens.layoutScreenMargin,
                    bottom = MorningDimens.space16,
                ),
        ) {
            MorningButton(
                text = stringResource(R.string.edit_save),
                onClick = onSave,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun onOff(on: Boolean) = if (on) SettingRowState.On else SettingRowState.Off
