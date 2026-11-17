package com.lumian.morningup.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumian.morningup.core.alarm.MinuteTicker
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.AlarmBook
import com.lumian.morningup.core.model.engine.HolidayCalendar
import com.lumian.morningup.core.model.engine.NextAlarm
import com.lumian.morningup.core.model.engine.NextRingCalculator
import com.lumian.morningup.core.model.engine.TimeUntil
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
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

/** 홈 아래에 잠깐 뜨는 알림. */
sealed interface HomeMessage {
    /** H6 저장 토스트. [until]이 null이면 울릴 시각이 없어요(꺼짐). */
    data class Saved(val until: TimeUntil?) : HomeMessage

    /** H7 삭제 스낵바. '되돌리기'로 [alarm]을 되살려요. */
    data class Deleted(val alarm: Alarm) : HomeMessage
}

data class HomeUiState(
    val loading: Boolean = true,
    val alarms: List<Alarm> = emptyList(),
    /** 다음에 울릴 알람. 켜진 알람이 없으면 null(H1 문구). */
    val next: NextAlarm? = null,
    val today: LocalDate? = null,
    /** H5 삭제 확인 중인 알람. */
    val pendingDelete: Alarm? = null,
    val message: HomeMessage? = null,
)

/** 알람 탭 홈(H0, H1, H5~H7). */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val book: AlarmBook,
    private val now: NowProvider,
    private val holidays: HolidayCalendar,
    ticker: MinuteTicker,
) : ViewModel() {
    private data class Overlay(val pendingDelete: Alarm? = null, val message: HomeMessage? = null)

    private val overlay = MutableStateFlow(Overlay())

    /** 처음 한 번, 그 뒤 분이 바뀔 때마다 지금 시각. */
    private val clock = merge(flowOf(Unit), ticker.ticks()).map { now.now() }

    val uiState: StateFlow<HomeUiState> = combine(book.alarms, clock, overlay) { alarms, at, o ->
        HomeUiState(
            loading = false,
            alarms = alarms,
            next = NextAlarm.find(alarms, at, holidays),
            today = at.toLocalDate(),
            pendingDelete = o.pendingDelete,
            message = o.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    /** 편집 화면에서 저장하고 돌아왔어요(H6 토스트). */
    fun onAlarmSaved(id: Long) {
        viewModelScope.launch {
            val alarm = book.get(id)
            val current = now.now()
            val until = alarm
                ?.let { NextRingCalculator.next(it, current, holidays) }
                ?.let { TimeUntil.between(current, it) }
            overlay.update { it.copy(message = HomeMessage.Saved(until)) }
        }
    }

    fun setEnabled(alarm: Alarm, enabled: Boolean) {
        viewModelScope.launch { book.setEnabled(alarm.id, enabled) }
    }

    fun requestDelete(alarm: Alarm) = overlay.update { it.copy(pendingDelete = alarm) }

    fun cancelDelete() = overlay.update { it.copy(pendingDelete = null) }

    fun confirmDelete() {
        val alarm = overlay.value.pendingDelete ?: return
        overlay.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            val deleted = book.delete(alarm.id) ?: return@launch
            overlay.update { it.copy(message = HomeMessage.Deleted(deleted)) }
        }
    }

    fun undoDelete() {
        val message = overlay.value.message as? HomeMessage.Deleted ?: return
        overlay.update { it.copy(message = null) }
        viewModelScope.launch { book.restore(message.alarm) }
    }

    /** 토스트나 스낵바가 시간이 다 되었거나 밀어서 닫혔어요. */
    fun dismissMessage(message: HomeMessage) =
        overlay.update { if (it.message == message) it.copy(message = null) else it }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
