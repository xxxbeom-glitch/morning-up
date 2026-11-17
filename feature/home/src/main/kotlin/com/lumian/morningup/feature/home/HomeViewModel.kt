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
import java.time.DayOfWeek
import java.time.LocalDate
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

/** 홈 아래에 잠깐 뜨는 알림. */
sealed interface HomeMessage {
    /** H6 저장 토스트. [until]이 null이면 울릴 시각이 없어요(꺼짐). */
    data class Saved(val until: TimeUntil?) : HomeMessage

    /** H7 삭제 스낵바. '되돌리기'로 [alarms]를 되살려요(선택 모드에서는 여러 개). */
    data class Deleted(val alarms: List<Alarm>) : HomeMessage

    /** 선택 모드 '복사' 뒤 토스트. */
    data class Copied(val count: Int) : HomeMessage
}

data class HomeUiState(
    val loading: Boolean = true,
    val alarms: List<Alarm> = emptyList(),
    /** 다음에 울릴 알람. 켜진 알람이 없으면 null(H1 문구). */
    val next: NextAlarm? = null,
    val today: LocalDate? = null,
    /** 지금 시각(분 단위). H0 하늘 장면이 따라요. */
    val time: LocalTime? = null,
    /** H5 삭제 확인 중인 알람. 비어 있으면 확인 창이 없어요. */
    val pendingDelete: List<Alarm> = emptyList(),
    val message: HomeMessage? = null,
    /** H8 Rest Offer: 이 화면에서 방금 끈 반복 알람. 카드 아래 '내일 다시 켜기'가 붙어요. */
    val restOffers: Set<Long> = emptySet(),
    /** H8 Resting: '이번만 쉬기' 중인 알람과 다시 울릴 요일. 토글은 꺼진 모양으로 보여요. */
    val resting: Map<Long, DayOfWeek> = emptyMap(),
    /** H3-1 선택 모드에서 고른 알람. null이면 선택 모드가 아니에요. */
    val selection: Set<Long>? = null,
) {
    val selecting: Boolean get() = selection != null

    /** 화면에 켜진 모양으로 보이는지. 쉬는 중인 알람은 꺼진 모양이에요. */
    fun shownOn(alarm: Alarm): Boolean = alarm.enabled && alarm.id !in resting

    fun showsRestOffer(alarm: Alarm): Boolean = alarm.id in restOffers && !alarm.enabled && alarm.isRepeating
}

/** 알람 탭 홈(H0, H1, H3, H5~H8). */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val book: AlarmBook,
    private val now: NowProvider,
    private val holidays: HolidayCalendar,
    ticker: MinuteTicker,
) : ViewModel() {
    private data class Overlay(
        val pendingDelete: List<Alarm> = emptyList(),
        val message: HomeMessage? = null,
        val restOffers: Set<Long> = emptySet(),
        val selection: Set<Long>? = null,
    )

    private val overlay = MutableStateFlow(Overlay())

    /** 처음 한 번, 그 뒤 분이 바뀔 때마다 지금 시각. */
    private val clock = merge(flowOf(Unit), ticker.ticks()).map { now.now() }

    val uiState: StateFlow<HomeUiState> = combine(book.alarms, clock, overlay) { alarms, at, o ->
        val ids = alarms.mapTo(HashSet()) { it.id }
        HomeUiState(
            loading = false,
            alarms = alarms,
            next = NextAlarm.find(alarms, at, holidays),
            today = at.toLocalDate(),
            time = at.toLocalTime().withSecond(0).withNano(0),
            pendingDelete = o.pendingDelete,
            message = o.message,
            restOffers = o.restOffers,
            resting = alarms.mapNotNull { a -> restingDay(a, at)?.let { a.id to it } }.toMap(),
            selection = o.selection?.filterTo(LinkedHashSet()) { it in ids },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    /**
     * '이번만 쉬기'가 바로 다음 울림을 건너뛰고 있으면 그다음 울릴 요일. 건너뛸 날이 이미 지났거나
     * 다음 울림과 상관없으면 null이라 보통 카드로 돌아가요.
     */
    private fun restingDay(alarm: Alarm, at: ZonedDateTime): DayOfWeek? {
        val skip = alarm.skipOnceDate
        val active = skip != null && alarm.enabled && alarm.isRepeating
        val skipsNext = active &&
            NextRingCalculator.next(alarm.copy(skipOnceDate = null), at, holidays)?.toLocalDate() == skip
        return if (skipsNext) NextRingCalculator.next(alarm, at, holidays)?.dayOfWeek else null
    }

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

    /** 카드 토글. 반복 알람을 끄면 '내일 다시 켜기'를 달고, 쉬는 중인 알람을 켜면 쉬기를 풀어요. */
    fun setEnabled(alarm: Alarm, enabled: Boolean) {
        val resting = alarm.id in uiState.value.resting
        val offer = !enabled && alarm.isRepeating
        overlay.update {
            it.copy(restOffers = if (offer) it.restOffers + alarm.id else it.restOffers - alarm.id)
        }
        viewModelScope.launch {
            when {
                enabled && resting -> book.save(alarm.copy(skipOnceDate = null))
                else -> book.setEnabled(alarm.id, enabled)
            }
        }
    }

    /** H8 '내일 다시 켜기': 다시 켜되 바로 다음 한 번만 쉬어요. */
    fun restOnce(alarm: Alarm) {
        overlay.update { it.copy(restOffers = it.restOffers - alarm.id) }
        viewModelScope.launch {
            val on = alarm.copy(enabled = true, skipOnceDate = null)
            val skip = NextRingCalculator.next(on, now.now(), holidays)?.toLocalDate()
            book.save(on.copy(skipOnceDate = skip))
        }
    }

    /** 더보기 '모든 알람 끄기'. 켜져 있던 반복 알람마다 '내일 다시 켜기'를 달아요(H8-4). */
    fun turnAllOff() = turnOff(uiState.value.alarms)

    private fun turnOff(alarms: List<Alarm>) {
        val state = uiState.value
        val targets = alarms.filter { state.shownOn(it) }
        overlay.update { o -> o.copy(restOffers = o.restOffers + targets.filter { it.isRepeating }.map { it.id }) }
        viewModelScope.launch { targets.forEach { book.setEnabled(it.id, false) } }
    }

    private fun turnOn(alarms: List<Alarm>) {
        val state = uiState.value
        val targets = alarms.filterNot { state.shownOn(it) }
        overlay.update { o -> o.copy(restOffers = o.restOffers - targets.map { it.id }.toSet()) }
        viewModelScope.launch {
            targets.forEach {
                if (it.id in state.resting) book.save(it.copy(skipOnceDate = null)) else book.setEnabled(it.id, true)
            }
        }
    }

    // H3 선택 모드

    /** 더보기 '선택' 또는 카드 길게 누르기. [first]가 있으면 그 카드를 고른 채로 들어가요. */
    fun startSelection(first: Alarm? = null) =
        overlay.update { it.copy(selection = first?.let { a -> setOf(a.id) } ?: emptySet()) }

    fun endSelection() = overlay.update { it.copy(selection = null) }

    fun toggleSelected(alarm: Alarm) = overlay.update { o ->
        val current = o.selection ?: return@update o
        o.copy(selection = if (alarm.id in current) current - alarm.id else current + alarm.id)
    }

    /** '전체 선택'. 다 골라져 있으면 모두 풀어요. */
    fun toggleSelectAll() {
        val all = uiState.value.alarms.map { it.id }.toSet()
        overlay.update { o ->
            val current = o.selection ?: return@update o
            o.copy(selection = if (current.containsAll(all)) emptySet() else all)
        }
    }

    private fun selectedAlarms(): List<Alarm> {
        val state = uiState.value
        val chosen = state.selection ?: return emptyList()
        return state.alarms.filter { it.id in chosen }
    }

    fun turnOnSelected() {
        val chosen = selectedAlarms()
        endSelection()
        turnOn(chosen)
    }

    fun turnOffSelected() {
        val chosen = selectedAlarms()
        endSelection()
        turnOff(chosen)
    }

    /** '복사': 고른 알람을 같은 설정으로 하나씩 더 만들어요. */
    fun copySelected() {
        val chosen = selectedAlarms()
        endSelection()
        if (chosen.isEmpty()) return
        viewModelScope.launch {
            chosen.forEach { book.save(it.copy(id = 0, skipOnceDate = null, nextTriggerAt = null)) }
            overlay.update { it.copy(message = HomeMessage.Copied(chosen.size)) }
        }
    }

    /** '삭제': 확인 창(H5)을 먼저 띄워요. */
    fun deleteSelected() {
        val chosen = selectedAlarms()
        if (chosen.isNotEmpty()) overlay.update { it.copy(pendingDelete = chosen) }
    }

    fun cancelDelete() = overlay.update { it.copy(pendingDelete = emptyList()) }

    fun confirmDelete() {
        val alarms = overlay.value.pendingDelete
        if (alarms.isEmpty()) return
        overlay.update { it.copy(pendingDelete = emptyList(), selection = null) }
        viewModelScope.launch {
            val deleted = alarms.mapNotNull { book.delete(it.id) }
            if (deleted.isNotEmpty()) overlay.update { it.copy(message = HomeMessage.Deleted(deleted)) }
        }
    }

    fun undoDelete() {
        val message = overlay.value.message as? HomeMessage.Deleted ?: return
        overlay.update { it.copy(message = null) }
        viewModelScope.launch { message.alarms.forEach { book.restore(it) } }
    }

    /** 토스트나 스낵바가 시간이 다 되었거나 밀어서 닫혔어요. */
    fun dismissMessage(message: HomeMessage) =
        overlay.update { if (it.message == message) it.copy(message = null) else it }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
