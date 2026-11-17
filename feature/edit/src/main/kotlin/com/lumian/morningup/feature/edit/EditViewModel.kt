package com.lumian.morningup.feature.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumian.morningup.core.alarm.MinuteTicker
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.AlarmBook
import com.lumian.morningup.core.model.RingAgain
import com.lumian.morningup.core.model.engine.HolidayCalendar
import com.lumian.morningup.core.model.engine.NextAlarm
import com.lumian.morningup.core.model.engine.TimeUntil
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalTime
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

    data class Saved(val alarmId: Long) : EditExit
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
) {
    /** 미션이 없으면 '미션 선택' 토글 꺼짐, 값 '없음', '미리 해보기' 숨김, '전원 종료 방지' 비활성(dev-readiness 4-1). */
    val hasMissions: Boolean get() = draft.missions.isNotEmpty()
}

/** 알람 편집 S0과 S0-1. */
@HiltViewModel
class EditViewModel @Inject constructor(
    private val book: AlarmBook,
    private val now: NowProvider,
    private val holidays: HolidayCalendar,
    ticker: MinuteTicker,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private data class Editor(
        val loading: Boolean = true,
        val original: Alarm = newAlarm(),
        val draft: Alarm = newAlarm(),
        val confirmExit: Boolean = false,
        val saving: Boolean = false,
        val exit: EditExit? = null,
    )

    private val alarmId: Long = savedStateHandle.get<Long>(ALARM_ID_KEY) ?: EditRoute.NEW_ALARM
    private val editor = MutableStateFlow(Editor())
    private val clock = merge(flowOf(Unit), ticker.ticks()).map { now.now() }

    val uiState: StateFlow<EditUiState> = combine(editor, clock) { e, at ->
        EditUiState(
            loading = e.loading,
            isNew = e.original.id == EditRoute.NEW_ALARM,
            draft = e.draft,
            changed = e.draft != e.original,
            until = NextAlarm.ringTimeIfSaved(e.draft, at, holidays)?.let { TimeUntil.between(at, it) },
            confirmExit = e.confirmExit,
            saving = e.saving,
            exit = e.exit,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), EditUiState())

    init {
        viewModelScope.launch {
            val loaded = alarmId.takeIf { it != EditRoute.NEW_ALARM }?.let { book.get(it) } ?: newAlarm()
            editor.update { it.copy(loading = false, original = loaded, draft = loaded) }
        }
    }

    private fun edit(change: (Alarm) -> Alarm) = editor.update { it.copy(draft = change(it.draft)) }

    fun setTime(hour: Int, minute: Int) = edit { it.copy(time = LocalTime.of(hour, minute)) }

    fun toggleDay(day: DayOfWeek) = edit {
        it.copy(repeatDays = if (day in it.repeatDays) it.repeatDays - day else it.repeatDays + day)
    }

    fun setSkipHolidays(on: Boolean) = edit { it.copy(skipHolidays = on) }

    /** 끄면 고른 미션을 비워요. 켜기는 미션 고르기(S1-2, 5단계)에서 미션을 골라야 돼서 지금은 그대로 둬요. */
    fun setMissionsEnabled(on: Boolean) {
        if (!on) edit { it.copy(missions = emptyList()) }
    }

    fun setRingAgain(on: Boolean) = edit {
        val count = if (on) it.ringAgain.count.takeIf { c -> c != 0 } ?: DEFAULT_RING_AGAIN_COUNT else 0
        it.copy(ringAgain = RingAgain(it.ringAgain.intervalMinutes, count))
    }

    fun setWakeCheck(on: Boolean) = edit { it.copy(wakeCheck = it.wakeCheck.copy(enabled = on)) }

    fun setPowerOffGuard(on: Boolean) = edit { it.copy(powerOffGuard = on) }

    fun setSound(on: Boolean) = edit {
        it.copy(soundId = if (on) Alarm.DEFAULT_SOUND_ID else Alarm.SILENT_SOUND_ID)
    }

    fun setVibrate(on: Boolean) = edit { it.copy(vibrate = on) }

    fun setGentleWake(on: Boolean) = edit { it.copy(gentleWake = on) }

    fun setBackground(on: Boolean) = edit { it.copy(background = if (on) null else Alarm.BACKGROUND_PLAIN) }

    /** 뒤로가기. 바뀐 게 있으면 S0-1로 묻고, 없으면 바로 닫아요. */
    fun onBack() = editor.update {
        when {
            it.saving || it.exit != null -> it
            it.draft != it.original -> it.copy(confirmExit = true)
            else -> it.copy(exit = EditExit.Discarded)
        }
    }

    fun keepEditing() = editor.update { it.copy(confirmExit = false) }

    fun discard() = editor.update { it.copy(confirmExit = false, exit = EditExit.Discarded) }

    /** 저장하면 알람이 켜지고 예약이 따라와요. 두 번 눌러도 한 번만 저장해요. */
    fun save() {
        val current = editor.value
        if (current.loading || current.saving || current.exit != null) return
        editor.update { it.copy(saving = true) }
        viewModelScope.launch {
            val saved = book.save(current.draft.copy(enabled = true))
            editor.update { it.copy(saving = false, exit = EditExit.Saved(saved.id)) }
        }
    }

    companion object {
        /** [EditRoute]의 속성 이름. 타입 안전 경로는 속성 이름으로 SavedStateHandle에 값을 넣어요. */
        const val ALARM_ID_KEY = "alarmId"
        private const val STOP_TIMEOUT_MILLIS = 5_000L
        private const val DEFAULT_RING_AGAIN_COUNT = 3
        private val DEFAULT_TIME: LocalTime = LocalTime.of(7, 0)

        /**
         * 새 알람 처음 값. 미션 없음(4-1), 다시 울림 5분 3번, 다시 잠들기 방지 1분 후 1분(F9, 꺼진 채),
         * 알람음 기본, 진동 켬, 배경 시간대 하늘. 시각은 오전 7:00.
         */
        fun newAlarm(): Alarm = Alarm(time = DEFAULT_TIME)
    }
}
