package com.lumian.morningup.feature.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumian.morningup.core.alarm.MinuteTicker
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ShakeSupport
import com.lumian.morningup.core.model.WakeCheckSettings
import com.lumian.morningup.core.model.WakeCheck
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.AlarmBook
import com.lumian.morningup.core.model.WakeCheckGate
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.PermissionChecker
import com.lumian.morningup.core.model.SaveWarning
import com.lumian.morningup.core.model.saveWarning
import com.lumian.morningup.core.model.RingAgain
import com.lumian.morningup.core.model.Ringtones
import com.lumian.morningup.core.model.VibrationPattern
import com.lumian.morningup.core.model.engine.HolidayCalendar
import com.lumian.morningup.core.model.engine.NextAlarm
import com.lumian.morningup.core.model.engine.TimeUntil
import com.lumian.morningup.core.ui.AlarmBackgrounds
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** 알람 편집 화면 경로. [alarmId]가 [NEW_ALARM]이면 새 알람이에요. */
@Serializable
data class EditRoute(val alarmId: Long = NEW_ALARM) {
    companion object {
        const val NEW_ALARM = 0L
    }
}

/** 편집 화면을 닫는 이유. */
sealed interface EditExit {
    data object Discarded : EditExit

    /**
     * [restReleased]면 쉬는 알람의 시각, 요일, 공휴일 쉬기를 바꿔 '이번만 쉬기'를 풀었어요(결정 159).
     * [fix]가 있으면 저장 경고 창에서 '설정 열기', '권한 켜기'를 눌렀어요. 앱이 그 경고를 고칠 곳을 열어요.
     */
    data class Saved(
        val alarmId: Long,
        val checksStopped: Boolean = false,
        val restReleased: Boolean = false,
        val fix: SaveWarning? = null,
    ) : EditExit
}

data class EditUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val draft: Alarm = EditViewModel.newAlarm(),
    /** 처음 연 때와 달라졌으면 true. 뒤로가기에서 S0-1을 띄워요. */
    val changed: Boolean = false,
    /** 저장하면 몇 시간 몇 분 뒤에 울리는지(안내 글). */
    val until: TimeUntil? = null,
    /** S0-1 저장 안 하고 나가기 확인 중. */
    val confirmExit: Boolean = false,
    val saving: Boolean = false,
    /** 화면을 닫아야 하면 값이 생겨요. */
    val exit: EditExit? = null,
    /** 열려 있는 하위 페이지(S1-2 미션 고르기, S4 다시 잠들기 방지). */
    val detail: EditDetail? = null,
    /** 열려 있는 미션 설정 시트(S1-3~S1-7). */
    val missionSheet: MissionSheet? = null,
    /** 흔들기 센서가 있어야 Shake Shake를 고를 수 있어요. */
    val shakeAvailable: Boolean = true,
    /** 지금 울리는 알람이라 저장을 막았어요(결정 165). 토스트가 떠요. */
    val ringingBlocked: Boolean = false,
    /** 저장 전에 띄운 경고 창 S0-2, S0-3(TSK-177 답 1, 2). 두 버튼 모두 저장해요. */
    val saveWarning: SaveWarning? = null,
) {
    /** 미션이 없으면 '미션 선택' 토글 꺼짐, 값 '없음', '미리 해보기' 숨김, '전원 종료 방지' 비활성(dev-readiness 4-1). */
    val hasMissions: Boolean get() = draft.missions.isNotEmpty()

    /** 칸 4개를 다 채웠으면 더 고를 수 없어요. */
    val missionsFull: Boolean get() = draft.missions.size >= MissionSetting.MAX_MISSIONS

    /** 고른 미션 순서 그대로 위, 안 고른 미션은 원래 순서로 아래(S1-2). */
    val missionOrder: List<MissionType>
        get() = draft.missionTypes + MISSION_LIST_ORDER.filter { it !in draft.missionTypes }

    /** 이 미션 카드를 누를 수 있는지. 고른 카드는 늘 눌러요. */
    fun canOpen(type: MissionType): Boolean = when {
        type in draft.missionTypes -> true
        type == MissionType.ShakeShake && !shakeAvailable -> false
        else -> !missionsFull
    }

    companion object {
        /** S1-2 카드 원래 순서. */
        val MISSION_LIST_ORDER =
            listOf(MissionType.DropWords, MissionType.PopQuiz, MissionType.QuickMath, MissionType.ShakeShake)
    }
}

/** 미션 설정 시트 하나. [selected]면 이미 고른 미션이라 값이 바로 저장되고 버튼이 '선택 해제'예요. */
data class MissionSheet(val setting: MissionSetting, val selected: Boolean) {
    private val range get() = MissionSetting.rangeOf(setting.type)
    val canLevelDown: Boolean get() = setting.level > range.levels.first
    val canLevelUp: Boolean get() = setting.level < range.levels.last
    val canSpeedDown: Boolean get() = setting.speed > MissionSetting.SPEEDS.first
    val canSpeedUp: Boolean get() = setting.speed < MissionSetting.SPEEDS.last
    val canCountDown: Boolean get() = setting.count > range.counts.first
    val canCountUp: Boolean get() = setting.count < range.counts.last
}

/** 알람 편집 S0과 S0-1. 하위 페이지와 미션 시트도 같은 상태라 화면 하나에 뷰모델 하나로 둬요(ADR 0005). */
@Suppress("TooManyFunctions")
@HiltViewModel
class EditViewModel @Inject constructor(
    private val book: AlarmBook,
    private val now: NowProvider,
    private val holidays: HolidayCalendar,
    ticker: MinuteTicker,
    savedStateHandle: SavedStateHandle,
    shakeSupport: ShakeSupport,
    private val checks: WakeCheckGate? = null,
    private val permissions: PermissionChecker? = null,
) : ViewModel() {
    private data class Editor(
        val loading: Boolean = true,
        val original: Alarm = newAlarm(),
        val draft: Alarm = newAlarm(),
        val confirmExit: Boolean = false,
        val saving: Boolean = false,
        val exit: EditExit? = null,
        val detail: EditDetail? = null,
        val sheet: MissionSheet? = null,
        /** 고른 미션의 값을 기억해 둬요. 빼거나 해제한 뒤 다시 열면 그 값으로 열려요(이 편집 화면 안에서만). */
        val remembered: Map<MissionType, MissionSetting> = emptyMap(),
        /** 배경 토글을 다시 켤 때 되살릴 배경(null은 시간대 하늘). */
        val lastBackground: String? = null,
        val ringingBlocked: Boolean = false,
        val saveWarning: SaveWarning? = null,
    ) {
        /** 불러오는 중, 저장 중, 닫는 중이거나 경고 창이 떠 있으면 저장을 다시 받지 않아요. */
        val busy: Boolean get() = loading || saving || exit != null || saveWarning != null
    }

    private val shakeAvailable = shakeSupport.available()

    private val alarmId: Long = savedStateHandle.get<Long>(ALARM_ID_KEY) ?: EditRoute.NEW_ALARM
    private val editor = MutableStateFlow(Editor())
    private val clock = merge(flowOf(Unit), ticker.ticks()).map { now.now() }

    val uiState: StateFlow<EditUiState> = combine(editor, clock) { e, at ->
        EditUiState(
            loading = e.loading,
            isNew = e.original.id == EditRoute.NEW_ALARM,
            draft = e.draft,
            changed = e.draft != e.original,
            // 결정 159: 시각, 요일을 안 바꾼 동안은 쉬기를 넣고, 바꾸면 쉬기를 뺀 새 울림으로 세요.
            until = NextAlarm.ringTimeIfSaved(toSave(e.original, e.draft), at, holidays)
                ?.let { TimeUntil.between(at, it) },
            confirmExit = e.confirmExit,
            saving = e.saving,
            exit = e.exit,
            detail = e.detail,
            missionSheet = e.sheet,
            shakeAvailable = shakeAvailable,
            ringingBlocked = e.ringingBlocked,
            saveWarning = e.saveWarning,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), EditUiState())

    init {
        viewModelScope.launch {
            val loaded = alarmId.takeIf { it != EditRoute.NEW_ALARM }?.let { book.get(it) } ?: newAlarm()
            editor.update {
                it.copy(
                    loading = false,
                    original = loaded,
                    draft = loaded,
                    lastBackground = loaded.background.takeUnless { b -> b == Alarm.BACKGROUND_PLAIN },
                )
            }
        }
    }

    private fun edit(change: (Alarm) -> Alarm) = editor.update { it.copy(draft = change(it.draft)) }

    fun setTime(hour: Int, minute: Int) = edit { it.copy(time = LocalTime.of(hour, minute)) }

    fun toggleDay(day: DayOfWeek) = edit {
        it.copy(repeatDays = if (day in it.repeatDays) it.repeatDays - day else it.repeatDays + day)
    }

    fun setSkipHolidays(on: Boolean) = edit { it.copy(skipHolidays = on) }

    /** 끄면 고른 미션을 비워요. 켜면 미션 고르기(S1-2)를 열어요(골라야 켜져요). */
    fun setMissionsEnabled(on: Boolean) {
        if (on) {
            editor.update { it.copy(detail = EditDetail.Mission) }
        } else {
            edit { it.copy(missions = emptyList()) }
        }
    }

    /** S1-2 카드나 체크 원을 눌러 그 미션 설정 시트를 열어요. 고를 수 없는 카드면 아무 일 없어요. */
    fun openMissionSheet(type: MissionType) = editor.update { e ->
        val selected = e.draft.missions.firstOrNull { it.type == type }
        val full = e.draft.missions.size >= MissionSetting.MAX_MISSIONS
        when {
            selected != null -> e.copy(sheet = MissionSheet(selected.normalized(), selected = true))
            full || (type == MissionType.ShakeShake && !shakeAvailable) -> e
            else -> e.copy(sheet = MissionSheet(e.remembered[type] ?: MissionSetting.default(type), selected = false))
        }
    }

    /** 시트 난이도 < >. */
    fun stepMissionLevel(delta: Int) = changeSheet { it.copy(level = it.level + delta) }

    /** 시트 속도 < >(Drop Words). */
    fun stepMissionSpeed(delta: Int) = changeSheet { it.copy(speed = it.speed + delta) }

    /** 시트 개수 − +. 한 번에 [MissionSetting.Range.countStep]씩(11-19 사용자 결정으로 모든 미션 1씩). */
    fun stepMissionCount(delta: Int) = changeSheet {
        it.copy(count = it.count + delta * MissionSetting.rangeOf(it.type).countStep)
    }

    /** 범위 밖은 끝 값으로 맞춰요. 고른 미션이면 값이 바로 저장돼요. */
    private fun changeSheet(change: (MissionSetting) -> MissionSetting) = editor.update { e ->
        val sheet = e.sheet ?: return@update e
        val next = change(sheet.setting).normalized()
        val draft = if (sheet.selected) {
            e.draft.copy(missions = e.draft.missions.map { m -> if (m.type == next.type) next else m })
        } else {
            e.draft
        }
        // 안 고른 미션은 '선택 완료'를 눌러야 반영돼요. 그냥 닫으면 바꾼 값은 버려요(11-18 사용자 지시).
        val remembered = if (sheet.selected) e.remembered + (next.type to next) else e.remembered
        e.copy(sheet = sheet.copy(setting = next), draft = draft, remembered = remembered)
    }

    /** '선택 완료'. 고른 순서대로 뒤에 붙여요(최대 [MissionSetting.MAX_MISSIONS]개). */
    fun confirmMissionSheet() = editor.update { e ->
        val sheet = e.sheet ?: return@update e
        val canAdd = !sheet.selected &&
            e.draft.missions.none { it.type == sheet.setting.type } &&
            e.draft.missions.size < MissionSetting.MAX_MISSIONS
        val draft = if (canAdd) e.draft.copy(missions = e.draft.missions + sheet.setting) else e.draft
        e.copy(sheet = null, draft = draft)
    }

    /** '선택 해제'. 목록에서 빼고 시트를 닫아요(값은 기억해 둬요). */
    fun deselectMission() = editor.update { e ->
        val sheet = e.sheet ?: return@update e
        e.copy(
            sheet = null,
            draft = e.draft.copy(missions = e.draft.missions.filterNot { it.type == sheet.setting.type }),
        )
    }

    fun closeMissionSheet() = editor.update { it.copy(sheet = null) }

    /** 미션 칸 오른쪽 위 X(결정 87). 확인 창 없이 바로 빼요(값은 기억해 둬요). */
    fun removeMission(type: MissionType) = edit { it.copy(missions = it.missions.filterNot { m -> m.type == type }) }

    fun setRingAgain(on: Boolean) = edit {
        val count = if (on) countWhenOn(it.ringAgain.count) else 0
        it.copy(ringAgain = RingAgain(it.ringAgain.intervalMinutes, count))
    }

    fun setWakeCheck(on: Boolean) = edit { it.copy(wakeCheck = it.wakeCheck.copy(enabled = on)) }

    // S4 시트(결정 180). 값을 고르면 다시 잠들기 방지도 켜요(꺼진 줄의 글자를 눌러 시트를 연 경우).
    fun setWakeCheckAsk(minutes: Int) {
        if (minutes in WakeCheckSettings.ASK_MINUTES) editCheck { it.copy(askAfterMinutes = minutes) }
    }

    fun setWakeCheckAnswer(seconds: Int) {
        if (seconds in WakeCheckSettings.ANSWER_SECONDS) editCheck { it.copy(answerSeconds = seconds) }
    }

    fun setRecheck(on: Boolean) = editCheck { it.copy(recheckEnabled = on) }

    fun setRecheckWait(minutes: Int) {
        if (minutes in WakeCheckSettings.RECHECK_WAIT_MINUTES) editCheck { it.copy(recheckWaitMinutes = minutes) }
    }

    fun setRecheckCount(count: Int) {
        if (count in WakeCheckSettings.RECHECK_COUNTS) editCheck { it.copy(recheckCount = count) }
    }

    private fun editCheck(transform: (WakeCheck) -> WakeCheck) =
        edit { it.copy(wakeCheck = transform(it.wakeCheck).copy(enabled = true)) }

    /** S3 '간격'. 꺼져 있었으면 처음 횟수(3번)로 켜져요. */
    fun setRingAgainInterval(minutes: Int) {
        if (minutes in RING_AGAIN_INTERVALS) {
            edit {
                it.copy(ringAgain = RingAgain(minutes, countWhenOn(it.ringAgain.count)))
            }
        }
    }

    /** 꺼짐(0)일 때만 처음 횟수(3번)를 넣어요. 무제한(null)과 고른 횟수는 그대로 둬요(ISS-036). */
    private fun countWhenOn(count: Int?): Int? = if (count == 0) DEFAULT_RING_AGAIN_COUNT else count

    /** S3 '횟수'. 1회, 3회, 5회, null(무제한). 꺼져 있었으면 고른 횟수로 켜져요(꺼짐은 횟수 0이라서요). */
    fun setRingAgainCount(count: Int?) {
        if (count in RING_AGAIN_COUNTS) edit { it.copy(ringAgain = it.ringAgain.copy(count = count)) }
    }

    /** S6 볼륨(0~100). */
    fun setVolume(percent: Int) = edit { it.copy(volumePercent = percent.coerceIn(0, MAX_VOLUME)) }

    /** S6 진동 모양. 고르면 진동도 켜요. */
    fun setVibrationPattern(pattern: VibrationPattern) =
        edit { it.copy(vibrationPattern = pattern, vibrate = true) }

    /** S7 길이. 고르면 켜져요. */
    fun setGentleSeconds(seconds: Int) {
        if (seconds in Alarm.GENTLE_SECONDS) edit { it.copy(gentleSeconds = seconds, gentleWake = true) }
    }

    /** S8에서 고른 배경. null은 시간대 하늘, "photo:"로 시작하면 내 사진이에요. 모르는 id는 무시해요. */
    fun setBackgroundId(id: String?) {
        val known = id == null || AlarmBackgrounds.isPhoto(id) || AlarmBackgrounds.find(id) != null
        if (known) editor.update { it.copy(draft = it.draft.copy(background = id), lastBackground = id) }
    }

    /** 하위 페이지 열기. '미리 해보기'와 '전원 종료 방지'는 앱이 따로 열어요. */
    fun openDetail(detail: EditDetail) {
        if (detail in OPENABLE_DETAILS) editor.update { it.copy(detail = detail) }
    }

    fun closeDetail() = editor.update { it.copy(detail = null, sheet = null) }

    fun setPowerOffGuard(on: Boolean) = edit { it.copy(powerOffGuard = on) }

    /** S5에서 고른 알람음. 목록에 없는 id는 무시해요. */
    fun setSoundId(id: String) {
        if (Ringtones.all.any { it.id == id }) {
            editor.update { it.copy(draft = it.draft.copy(soundId = id)) }
        }
    }

    fun setVibrate(on: Boolean) = edit { it.copy(vibrate = on) }

    fun setGentleWake(on: Boolean) = edit { it.copy(gentleWake = on) }

    fun setBackground(on: Boolean) = editor.update {
        it.copy(draft = it.draft.copy(background = if (on) it.lastBackground else Alarm.BACKGROUND_PLAIN))
    }

    /** 뒤로가기. 하위 페이지가 열려 있으면 그것만 닫고, 바뀐 게 있으면 S0-1로 묻고, 없으면 바로 닫아요. */
    fun onBack() = editor.update {
        when {
            it.sheet != null -> it.copy(sheet = null)
            it.detail != null -> it.copy(detail = null)
            it.saving || it.exit != null -> it
            it.saveWarning != null -> it.copy(saveWarning = null)
            it.draft != it.original -> it.copy(confirmExit = true)
            else -> it.copy(exit = EditExit.Discarded)
        }
    }

    fun keepEditing() = editor.update { it.copy(confirmExit = false) }

    fun discard() = editor.update { it.copy(confirmExit = false, exit = EditExit.Discarded) }

    /** 저장하면 알람이 켜지고 예약이 따라와요. 두 번 눌러도 한 번만 저장해요. */
    fun save() {
        val current = editor.value
        if (current.busy) return
        // 결정 165: 울리는 중, 쉬는 중, 미션 중인 알람은 저장하지 않고 울림 화면에서 끄라고 알려요. 편집 화면은 그대로예요.
        if (current.original.id != EditRoute.NEW_ALARM && checks?.isRinging(current.original.id) == true) {
            editor.update { it.copy(ringingBlocked = true) }
            return
        }
        // TSK-177 답 1, 2: 경고할 것이 있으면 저장 전에 창부터 띄워요. 창의 두 버튼이 모두 저장해요.
        val warning = permissions?.let {
            it.refresh()
            it.report.value.saveWarning
        }
        if (warning != null) {
            editor.update { it.copy(saveWarning = warning) }
            return
        }
        write(fix = null)
    }

    /** 경고 창에서 저장. [fix]면 '설정 열기', '권한 켜기'를 눌러 저장 뒤 고칠 곳을 열어요. */
    fun saveAnyway(fix: Boolean) {
        val warning = editor.value.saveWarning ?: return
        editor.update { it.copy(saveWarning = null) }
        write(fix = warning.takeIf { fix })
    }

    /** 창 밖을 누르거나 뒤로 가면 저장하지 않고 편집으로 돌아가요. */
    fun closeSaveWarning() = editor.update { it.copy(saveWarning = null) }

    private fun write(fix: SaveWarning?) {
        val current = editor.value
        editor.update { it.copy(saving = true) }
        val released = restingAt(current.original, now.now()) && timingChanged(current.original, current.draft)
        viewModelScope.launch {
            val saved = book.save(toSave(current.original, current.draft).copy(enabled = true))
            // 울린 뒤 남은 확인(W2, W3)이 있던 알람을 저장하면 그 확인을 멈춰요(결정 145, 홈 토스트).
            val stopped = current.draft.id != 0L && checks?.cancelFor(saved.id) == true
            editor.update { it.copy(saving = false, exit = EditExit.Saved(saved.id, stopped, released, fix)) }
        }
    }

    /** 저장 막음 토스트가 닫혔어요. */
    fun ringingNoticeShown() = editor.update { it.copy(ringingBlocked = false) }

    /** 결정 159: 언제 울리는지(시각, 요일, 공휴일 쉬기)를 바꿨으면 '이번만 쉬기'를 풀어요. 처음 값으로 되돌렸으면 안 바꾼 거예요. */
    private fun toSave(original: Alarm, draft: Alarm): Alarm =
        if (timingChanged(original, draft)) draft.copy(skipOnceDate = null) else draft

    /** 연 알람이 지금 쉬는 중인지. 쉬는 날이 오늘이나 어제(자정을 넘겨 울린 날)거나 그 뒤면 쉬는 중으로 쳐요. */
    private fun restingAt(original: Alarm, at: ZonedDateTime): Boolean {
        val skip = original.skipOnceDate ?: return false
        return original.enabled && original.isRepeating && !skip.isBefore(at.toLocalDate().minusDays(1))
    }

    companion object {
        /** 울리는 때를 바꾸는 값이 바뀌었는지(결정 159). */
        fun timingChanged(original: Alarm, draft: Alarm): Boolean =
            original.time != draft.time ||
                original.repeatDays != draft.repeatDays ||
                original.skipHolidays != draft.skipHolidays

        /** [EditRoute]의 속성 이름. 타입 안전 경로는 속성 이름으로 SavedStateHandle에 값을 넣어요. */
        const val ALARM_ID_KEY = "alarmId"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val DEFAULT_RING_AGAIN_COUNT = 3
        private val DEFAULT_TIME: LocalTime = LocalTime.of(7, 0)

        /** S3 다시 울림 시트 선택지(결정 82). 횟수 null은 '무제한'이에요. */
        val RING_AGAIN_INTERVALS = listOf(1, 3, 5, 10)
        val RING_AGAIN_COUNTS = listOf(null, 1, 3, 5)
        const val MAX_VOLUME = 100

        private val OPENABLE_DETAILS = setOf(
            EditDetail.Mission,
            EditDetail.Sound,
            EditDetail.RingAgain,
            EditDetail.Vibration,
            EditDetail.Gentle,
            EditDetail.Background,
        )

        /**
         * 새 알람 처음 값. 미션 없음(4-1), 다시 울림 켬(5분, 3번, 결정 163. 옛 알람은 저장된 값 그대로),
         * 다시 잠들기 방지 꺼짐(간격과 시간은 전체 설정, 결정 145),
         * 알람음 기본, 진동 켬, 배경 시간대 하늘. 시각은 오전 7:00.
         */
        fun newAlarm(): Alarm = Alarm(time = DEFAULT_TIME, ringAgain = RingAgain(count = DEFAULT_RING_AGAIN_COUNT))
    }
}
