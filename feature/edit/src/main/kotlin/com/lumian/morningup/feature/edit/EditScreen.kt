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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningDayChip
import com.lumian.morningup.core.designsystem.component.MorningDialog
import com.lumian.morningup.core.designsystem.component.MorningPreviewButton
import com.lumian.morningup.core.designsystem.component.MorningModalSheet
import com.lumian.morningup.core.designsystem.component.MorningSettingRow
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.component.MorningWheelPicker
import com.lumian.morningup.core.designsystem.component.SettingRowState
import com.lumian.morningup.core.designsystem.component.WheelItemHeight
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.Ringtones
import com.lumian.morningup.core.model.VibrationPattern
import com.lumian.morningup.core.ui.AlarmText
import com.lumian.morningup.core.ui.alarmResources
import java.time.DayOfWeek

/** 편집 화면의 하위 화면. S1-2(Mission)와 S4(WakeCheck)만 열리고 나머지는 7단계 이후예요. */
enum class EditDetail { Mission, Preview, RingAgain, WakeCheck, PowerOff, Sound, Vibration, Gentle, Background }

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
private val PeriodWidth = 96.dp
private val NumberWidth = 72.dp
private val ColonWidth = 24.dp

/**
 * 알람 편집 S0. [onSaved]는 저장한 알람 id를, [onClose]는 저장 없이 닫을 때 불러요.
 * [onOpenDetail]은 아직 없는 하위 화면을 열 자리예요(지금은 아무 일 없음).
 * [onPreview]는 '미리 해보기'(S0, 고른 미션 전부)와 시트 '미리보기'(그 미션 하나)에서 불러요.
 */
@Composable
fun AlarmEditRoute(
    onSaved: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenDetail: (EditDetail) -> Unit = {},
    onPreview: (List<MissionSetting>) -> Unit = {},
    viewModel: EditViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val saved by rememberUpdatedState(onSaved)
    val close by rememberUpdatedState(onClose)
    val preview by rememberUpdatedState(onPreview)
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
                onMissions = viewModel::setMissionsEnabled,
                onRingAgain = viewModel::setRingAgain,
                onWakeCheck = viewModel::setWakeCheck,
                onSound = viewModel::setSound,
                onSoundId = viewModel::setSoundId,
                onVibrate = viewModel::setVibrate,
                onGentle = viewModel::setGentleWake,
                onBackground = viewModel::setBackground,
                onSave = viewModel::save,
                onKeepEditing = viewModel::keepEditing,
                onDiscard = viewModel::discard,
                onWakeCheckAsk = viewModel::setWakeCheckAsk,
                onWakeCheckAnswer = viewModel::setWakeCheckAnswer,
                onRingAgainInterval = viewModel::setRingAgainInterval,
                onRingAgainCount = viewModel::setRingAgainCount,
                onVolume = viewModel::setVolume,
                onVibrationPattern = viewModel::setVibrationPattern,
                onGentleSeconds = viewModel::setGentleSeconds,
                onBackgroundId = viewModel::setBackgroundId,
                onCloseDetail = viewModel::closeDetail,
                onOpenMissionSheet = viewModel::openMissionSheet,
                onCloseMissionSheet = viewModel::closeMissionSheet,
                onMissionLevel = viewModel::stepMissionLevel,
                onMissionSpeed = viewModel::stepMissionSpeed,
                onMissionCount = viewModel::stepMissionCount,
                onConfirmMission = viewModel::confirmMissionSheet,
                onDeselectMission = viewModel::deselectMission,
                onPreview = { preview(it) },
            )
        },
        onOpenDetail = { detail ->
            if (detail in PAGE_DETAILS) viewModel.openDetail(detail) else onOpenDetail(detail)
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
    val onSound: (Boolean) -> Unit = {},
    val onSoundId: (String) -> Unit = {},
    val onVibrate: (Boolean) -> Unit = {},
    val onGentle: (Boolean) -> Unit = {},
    val onBackground: (Boolean) -> Unit = {},
    val onSave: () -> Unit = {},
    val onKeepEditing: () -> Unit = {},
    val onDiscard: () -> Unit = {},
    val onWakeCheckAsk: (Int) -> Unit = {},
    val onWakeCheckAnswer: (Int) -> Unit = {},
    val onRingAgainInterval: (Int) -> Unit = {},
    val onRingAgainCount: (Int?) -> Unit = {},
    val onVolume: (Int) -> Unit = {},
    val onVibrationPattern: (VibrationPattern) -> Unit = {},
    val onGentleSeconds: (Int) -> Unit = {},
    val onBackgroundId: (String?) -> Unit = {},
    val onCloseDetail: () -> Unit = {},
    val onOpenMissionSheet: (MissionType) -> Unit = {},
    val onCloseMissionSheet: () -> Unit = {},
    val onMissionLevel: (Int) -> Unit = {},
    val onMissionSpeed: (Int) -> Unit = {},
    val onMissionCount: (Int) -> Unit = {},
    val onConfirmMission: () -> Unit = {},
    val onDeselectMission: () -> Unit = {},
    val onPreview: (List<MissionSetting>) -> Unit = {},
)

/** 편집 화면 안에서 페이지로 여는 하위 화면. */
private val PAGE_DETAILS = setOf(
    EditDetail.Mission,
    EditDetail.WakeCheck,
    EditDetail.Sound,
    EditDetail.RingAgain,
    EditDetail.Vibration,
    EditDetail.Gentle,
    EditDetail.Background,
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
    // 편집 화면 위에 띄우는 시트(볼륨 등). 화면 회전에도 열린 채로 남아요.
    var sheet by rememberSaveable { mutableStateOf<EditSheet?>(null) }
    var lastSheet by remember { mutableStateOf<EditSheet?>(null) }
    if (sheet != null) lastSheet = sheet
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
                    SoundCard(state.draft, onOpenDetail, onOpenSheet = { sheet = it })
                    // 저장 버튼은 고정하지 않고 내용 맨 끝에 둬요. 끝까지 내려 봐야 보여서 아래 설정을 놓치지 않아요.
                    MorningButton(
                        text = stringResource(R.string.edit_save),
                        onClick = actions.onSave,
                        enabled = !state.saving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = MorningDimens.space16)
                            .navigationBarsPadding()
                            .padding(bottom = MorningDimens.space16),
                    )
                }
            }
        }
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
        when (state.detail) {
            EditDetail.WakeCheck -> WakeCheckPage(
                wakeCheck = state.draft.wakeCheck,
                onBack = actions.onCloseDetail,
                onAsk = actions.onWakeCheckAsk,
                onAnswer = actions.onWakeCheckAnswer,
                onOff = { actions.onWakeCheck(false) },
            )
            EditDetail.Mission -> MissionPickPage(state = state, actions = actions)
            EditDetail.Sound -> SoundPage(
                selectedId = state.draft.soundId,
                onBack = actions.onCloseDetail,
                onSelect = actions.onSoundId,
                onOff = { actions.onSound(false) },
            )
            EditDetail.RingAgain -> RingAgainPage(
                ringAgain = state.draft.ringAgain,
                onBack = actions.onCloseDetail,
                onInterval = actions.onRingAgainInterval,
                onCount = actions.onRingAgainCount,
                onOff = { actions.onRingAgain(false) },
            )
            EditDetail.Vibration -> VibrationPage(
                alarm = state.draft,
                onBack = actions.onCloseDetail,
                onPattern = actions.onVibrationPattern,
                onOff = { actions.onVibrate(false) },
            )
            EditDetail.Gentle -> GentlePage(
                seconds = state.draft.gentleSeconds,
                on = state.draft.gentleWake,
                onBack = actions.onCloseDetail,
                onSeconds = actions.onGentleSeconds,
                onOff = { actions.onGentle(false) },
            )
            EditDetail.Background -> BackgroundPage(
                selectedId = state.draft.background,
                onBack = actions.onCloseDetail,
                onSelect = actions.onBackgroundId,
                onOff = { actions.onBackground(false) },
            )
            else -> Unit
        }
        MorningModalSheet(visible = sheet != null, onDismiss = { sheet = null }, tag = EDIT_SHEET_TAG) {
            // 닫히는 동안에도 마지막 내용을 그대로 그려요.
            when (lastSheet) {
                EditSheet.Volume -> VolumeSheetContent(state.draft, actions, onDone = { sheet = null })
                else -> Unit
            }
        }
    }
}

const val EDIT_SHEET_TAG = "editSheet"

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

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = MorningDimens.space8),
    ) {
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
                // 쌍점은 숫자 가운데보다 살짝 아래로 보여서 4 올려요(데모와 같음).
                modifier = Modifier.width(ColonWidth).padding(bottom = MorningDimens.space4),
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
            value = if (state.hasMissions) AlarmText.missions(resources, alarm.missionTypes) else offValue(),
            state = onOff(state.hasMissions),
            onCheckedChange = actions.onMissions,
            onClick = { onOpenDetail(EditDetail.Mission) },
        )
        if (state.hasMissions) {
            MorningPreviewButton(
                text = stringResource(R.string.edit_preview),
                onClick = { actions.onPreview(alarm.missions) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val ringAgain = alarm.ringAgain
        val ringAgainCount = ringAgain.count
        MorningSettingRow(
            label = stringResource(R.string.edit_ring_again),
            value = when (ringAgainCount) {
                0 -> offValue()
                null -> stringResource(R.string.edit_ring_again_until_off, ringAgain.intervalMinutes)
                else -> stringResource(R.string.edit_ring_again_value, ringAgain.intervalMinutes, ringAgainCount)
            },
            state = onOff(ringAgainCount != 0),
            onCheckedChange = pickOrOff(EditDetail.RingAgain, onOpenDetail, actions.onRingAgain),
            onClick = { onOpenDetail(EditDetail.RingAgain) },
        )
        MorningSettingRow(
            label = stringResource(R.string.edit_wake_check),
            value = if (alarm.wakeCheck.enabled) {
                stringResource(R.string.edit_wake_check_value, alarm.wakeCheck.askAfterMinutes)
            } else {
                offValue()
            },
            state = onOff(alarm.wakeCheck.enabled),
            onCheckedChange = pickOrOff(EditDetail.WakeCheck, onOpenDetail, actions.onWakeCheck),
            onClick = { onOpenDetail(EditDetail.WakeCheck) },
        )
    }
}

/** 알람 사운드 카드(결정 97, 98): '알람음'과 '볼륨' 두 줄, 둘 다 > 줄. 볼륨 줄은 S6 볼륨 시트를 열어요. */
@Composable
private fun SoundCard(alarm: Alarm, onOpenDetail: (EditDetail) -> Unit, onOpenSheet: (EditSheet) -> Unit) {
    MorningSettingsCard(title = stringResource(R.string.edit_card_sound)) {
        val soundOn = alarm.soundId != Alarm.SILENT_SOUND_ID
        MorningSettingRow(
            label = stringResource(R.string.edit_sound),
            value = if (soundOn) Ringtones.find(alarm.soundId).name else offValue(),
            // 알람음은 토글 없이 '>'로 들어가서 골라요. '사용 안 함'은 들어간 화면 맨 위에 있어요.
            state = SettingRowState.Navigate,
            onCheckedChange = {},
            onClick = { onOpenDetail(EditDetail.Sound) },
        )
        MorningSettingRow(
            label = stringResource(R.string.edit_volume_title),
            value = volumeValue(alarm),
            state = SettingRowState.Navigate,
            onCheckedChange = {},
            onClick = { onOpenSheet(EditSheet.Volume) },
        )
    }
}

/** 꺼진 줄의 값 글자. 고른 것처럼 보이는 값을 남기지 않아요. */
@Composable
private fun offValue(): String = stringResource(R.string.edit_off_value)

/**
 * 고를 값이 있는 줄의 토글. 켜면 바로 켜지 않고 선택 화면을 열어(값을 골라야 켜져요), 끄면 바로 꺼요.
 */
private fun pickOrOff(
    detail: EditDetail,
    onOpenDetail: (EditDetail) -> Unit,
    setOn: (Boolean) -> Unit,
): (Boolean) -> Unit = { on -> if (on) onOpenDetail(detail) else setOn(false) }

private fun onOff(on: Boolean) = if (on) SettingRowState.On else SettingRowState.Off
