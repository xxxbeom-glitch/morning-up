package com.lumian.morningup.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumian.morningup.core.alarm.MinuteTicker
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.AlarmBook
import com.lumian.morningup.core.model.WakeCheckGate
import com.lumian.morningup.core.model.WeatherKind
import com.lumian.morningup.core.model.WeatherSource
import com.lumian.morningup.core.model.engine.HolidayCalendar
import com.lumian.morningup.core.model.engine.NextAlarm
import com.lumian.morningup.core.model.engine.NextRingCalculator
import com.lumian.morningup.core.model.engine.TimeUntil
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
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
    /** H6 저장 토스트. [until]이 null이면 울릴 시각이 없어요(꺼짐). [restReleased]면 쉬기를 푼 저장이에요(결정 159). */
    data class Saved(val until: TimeUntil?, val restReleased: Boolean = false) : HomeMessage

    /** H7 삭제 스낵바. '되돌리기'로 [alarms]를 되살려요(선택 모드에서는 여러 개). */
    data class Deleted(val alarms: List<Alarm>) : HomeMessage

    /** 선택 모드 '복제' 뒤 토스트(결정 161). */
    data class Copied(val count: Int) : HomeMessage

    /** 편집 저장, 홈 토글 끄기, 지우기로 그 알람의 남은 확인(W2, W3)을 멈췄어요(결정 145, 153). */
    data object ChecksStopped : HomeMessage

    /** 울리는 중, 쉬는 중, 미션 중인 알람이라 끄기, 쉬기, 지우기를 막았어요(결정 165). */
    data object Ringing : HomeMessage

    /**
     * '이번만 쉬기'를 누른 순간 쉬는 날 [skip]과 다음 울림 [next]를 한 번 알려요(결정 175).
     * [nextOnDate]면 다음 울림이 7일 뒤 이상이라 요일 말고 날짜로 보여요(결정 162와 같은 경계).
     */
    data class Rested(val skip: LocalDate, val next: ZonedDateTime, val nextOnDate: Boolean = false) : HomeMessage
}

/**
 * 쉬는 중 카드 글(결정 154, 162). 다음 울림까지 24시간 안이면 [Within]('N시간 N분 뒤'), 넘으면 [OnDay],
 * 다음 울림 날이 7일 뒤 이상이면 [OnDate]예요.
 * 남은 시간은 홈 위쪽 문구와 같은 [TimeUntil]로 세서 두 곳 값이 같아요.
 */
sealed interface RestLabel {
    data class Within(val until: TimeUntil) : RestLabel

    data class OnDay(val day: DayOfWeek) : RestLabel

    /** 결정 162: 다음 울림이 멀면 요일만으로는 이번 주로 읽혀서 날짜를 붙여요('12월 3일(목)에'). */
    data class OnDate(val date: LocalDate) : RestLabel

    companion object {
        /**
         * 오늘부터 다음 울림 날까지 이 날 수 이상이면 날짜로 보여요. 딱 7일째(오늘과 같은 요일)도 '목요일에'가
         * 오늘로 읽혀서 날짜로 해요(결정 162 사용자 확정). 경계를 바꿀 때는 이 값만 고쳐요.
         */
        const val DATE_FROM_DAYS = 7L

        fun between(now: ZonedDateTime, ring: ZonedDateTime): RestLabel {
            val until = TimeUntil.between(now, ring)
            val days = ChronoUnit.DAYS.between(now.toLocalDate(), ring.withZoneSameInstant(now.zone).toLocalDate())
            return when {
                until.days == 0 -> Within(until)
                days >= DATE_FROM_DAYS -> OnDate(ring.toLocalDate())
                else -> OnDay(ring.dayOfWeek)
            }
        }
    }
}

data class HomeUiState(
    val loading: Boolean = true,
    val alarms: List<Alarm> = emptyList(),
    /** 다음에 울릴 알람. 켜진 알람이 없으면 null(H1 문구). */
    val next: NextAlarm? = null,
    val today: LocalDate? = null,
    /** 지금 시각(분 단위). H0 하늘 장면이 따라요. */
    val time: LocalTime? = null,
    /** H0 하늘 날씨. null이면 시간대만 따르는 맑은 하늘이에요. */
    val weather: WeatherKind? = null,
    val message: HomeMessage? = null,
    /** H8 Rest Offer: 이 화면에서 방금 끈 반복 알람. 카드 아래 '이번만 쉬기'가 붙어요. */
    val restOffers: Set<Long> = emptySet(),
    /** H8 Resting: '이번만 쉬기' 중인 알람과 다시 울릴 때까지의 글. 토글은 꺼진 모양으로 보여요. */
    val resting: Map<Long, RestLabel> = emptyMap(),
    /** 선택 모드 '이번만 쉬기'로 쉴 수 있는 알람(결정 158). 켜진 반복 알람이고 쉬면 다음 울림이 바뀌는 것만이에요. */
    val restable: Set<Long> = emptySet(),
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
    private val weatherSource: WeatherSource,
    private val checks: WakeCheckGate,
) : ViewModel() {
    /** 한 번 더 확인(W3)이 남았는지. 홈 위쪽 띠가 봐요(결정 145). */
    val recheckPending: StateFlow<Boolean> = checks.recheckPending
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)

    /**
     * 홈 위쪽 띠(결정 145, 165, 168). W3 대기면 '한 번 더 확인', 쉬는 중이면 '오전 6:40에 다시 울려요',
     * W2 대기면 '곧 깼는지 물어볼게요'예요. 울림 단계는 한 번에 하나라 띠도 하나예요.
     */
    val status: StateFlow<HomeStatus?> = combine(
        checks.recheckPending,
        checks.recheckLeft,
        checks.restingUntil,
        checks.wakeCheckWaiting,
        checks.wakeCheckAt,
    ) { recheck, left, restUntil, waiting, askAt ->
        when {
            recheck -> HomeStatus.Recheck(left)
            restUntil != null -> HomeStatus.Resting(localTime(restUntil))
            waiting -> HomeStatus.WakeCheckSoon(askAt?.let(::localTime))
            else -> null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private fun localTime(epochMillis: Long): java.time.LocalTime =
        java.time.Instant.ofEpochMilli(epochMillis).atZone(now.now().zone).toLocalTime().withSecond(0).withNano(0)

    /** 홈 띠 '확인하기'. W3를 지금 열어요. */
    fun openRecheck() = checks.openRecheck()

    /** 쉬는 중 띠를 눌렀어요. 쉬는 중 알림과 같이 울림 화면을 열어요(결정 165). */
    fun openRest() = checks.openRest()

    /** 울리는 중, 쉬는 중, 미션 중이라 막을 알람인지(결정 165). */
    private fun ringing(alarm: Alarm): Boolean = checks.isRinging(alarm.id)

    private fun showRinging() = overlay.update { it.copy(message = HomeMessage.Ringing) }

    private data class Overlay(
        val message: HomeMessage? = null,
        val restOffers: Set<Long> = emptySet(),
        val selection: Set<Long>? = null,
        /** 울리는 중이거나 남은 확인을 기다리던 중에 끈 알람. 이 '이번만 쉬기'는 오늘 것을 쉰 걸로 쳐요(결정 154). */
        val restToday: Set<Long> = emptySet(),
        /** 지금 알림이 닫히면 이어서 띄울 것. 삭제 스낵바 뒤 '남은 확인을 멈췄어요' 토스트예요(결정 153). */
        val queued: HomeMessage? = null,
    )

    private val overlay = MutableStateFlow(Overlay())

    /** 처음 한 번, 그 뒤 분이 바뀔 때마다 지금 시각. */
    private val clock = merge(flowOf(Unit), ticker.ticks()).map { now.now() }

    val uiState: StateFlow<HomeUiState> = combine(book.alarms, clock, overlay, weatherSource.weather) {
            alarms,
            at,
            o,
            weather,
        ->
        val ids = alarms.mapTo(HashSet()) { it.id }
        val resting = alarms.mapNotNull { a -> restingLabel(a, at)?.let { a.id to it } }.toMap()
        HomeUiState(
            loading = false,
            alarms = sortForHome(alarms, at),
            next = NextAlarm.find(alarms, at, holidays),
            today = at.toLocalDate(),
            time = at.toLocalTime().withSecond(0).withNano(0),
            weather = weather,
            message = o.message,
            restOffers = o.restOffers,
            resting = resting,
            restable = if (o.selection == null) emptySet() else restable(alarms, resting.keys, at),
            selection = o.selection?.filterTo(LinkedHashSet()) { it in ids },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), HomeUiState())

    /**
     * '이번만 쉬기' 중이면 다시 울릴 때까지의 글. 아니면 null이라 보통 카드로 돌아가요. 쉬는 중은 두 가지예요.
     * - 바로 다음 울림을 건너뛰는 중([Alarm.skipOnceDate]가 그날).
     * - 울린 뒤 끄고 쉬어서 오늘 것을 쉰 걸로 친 경우([Alarm.skipOnceDate]가 이미 울린 그날). 그다음 울림 전까지예요.
     */
    private fun restingLabel(alarm: Alarm, at: ZonedDateTime): RestLabel? {
        val skip = alarm.skipOnceDate?.takeIf { alarm.enabled && alarm.isRepeating } ?: return null
        val plain = alarm.copy(skipOnceDate = null)
        val upcoming = NextRingCalculator.next(plain, at, holidays)
        val resting = upcoming != null &&
            (upcoming.toLocalDate() == skip || restedOn(plain, skip, at, upcoming))
        val ring = if (resting) NextRingCalculator.next(alarm, at, holidays) else null
        return ring?.let { RestLabel.between(at, it) }
    }

    /** [skip]날 울림이 이미 지났고, 그 뒤 첫 울림이 [upcoming]인지. 그 사이에 다른 울림이 지났으면 쉬기는 끝났어요. */
    private fun restedOn(plain: Alarm, skip: LocalDate, at: ZonedDateTime, upcoming: ZonedDateTime): Boolean {
        val dayStart = skip.atStartOfDay(at.zone).minusSeconds(1)
        val rang = NextRingCalculator.next(plain, dayStart, holidays) ?: return false
        if (rang.toLocalDate() != skip || rang.isAfter(at)) return false
        return NextRingCalculator.next(plain, rang, holidays) == upcoming
    }

    /**
     * 결정 158: 켜져 있고 쉬는 중이 아닌 반복 알람 가운데, 쉬면 다음 울림이 바뀌는 것. 남은 확인을 기다리는
     * 알람은 빠져요(그 알람은 하나씩 끄기로 지금 울림을 쉬어요, 결정 175).
     */
    private fun restable(alarms: List<Alarm>, resting: Set<Long>, at: ZonedDateTime): Set<Long> = alarms
        .filter { it.enabled && it.isRepeating && it.id !in resting && !checks.isActive(it.id) }
        .filter { alarm ->
            val plain = alarm.copy(skipOnceDate = null)
            val skip = restDay(plain, at)
            skip != null && NextRingCalculator.next(plain.copy(skipOnceDate = skip), at, holidays) !=
                NextRingCalculator.next(plain, at, holidays)
        }
        .mapTo(HashSet()) { it.id }

    /** 결정 175: 지금 뒤 아직 오지 않은 다음 울림의 날(오늘 이미 울렸으면 그다음 울림 날). */
    private fun restDay(on: Alarm, at: ZonedDateTime): LocalDate? =
        NextRingCalculator.next(on, at, holidays)?.toLocalDate()

    /** [now]까지 마지막으로 울릴 차례였던 날. 자정을 넘겨 확인 중이어도 울린 그날이에요. */
    private fun lastRingDate(alarm: Alarm, now: ZonedDateTime): LocalDate? {
        val plain = alarm.copy(enabled = true, skipOnceDate = null)
        var last: ZonedDateTime? = null
        var ring = NextRingCalculator.next(plain, now.minusDays(LOOK_BACK_DAYS), holidays)
        while (ring != null && !ring.isAfter(now)) {
            last = ring
            ring = NextRingCalculator.next(plain, ring, holidays)
        }
        return last?.toLocalDate()
    }

    /**
     * 홈 카드 순서. 켜진 알람이 위에서 빨리 울리는 순서로, 꺼진 알람은 그 아래 시각 순서로 둬요.
     * 꺼진 카드를 켜면 바로 제자리(울릴 순서)로 올라가요.
     */
    private fun sortForHome(alarms: List<Alarm>, at: ZonedDateTime): List<Alarm> {
        val (on, off) = alarms.partition { it.enabled }
        val onSorted = on.sortedWith(
            compareBy<Alarm> { NextRingCalculator.next(it, at, holidays)?.toInstant() ?: java.time.Instant.MAX }
                .thenBy { it.time }
                .thenBy { it.id },
        )
        return onSorted + off.sortedWith(compareBy<Alarm> { it.time }.thenBy { it.id })
    }

    /** 홈이 앞으로 나올 때. 날씨가 오래됐으면 새로 받아요. */
    fun refreshWeather() = weatherSource.refresh()

    /** 편집 화면에서 저장하고 돌아왔어요(H6 토스트). 남은 확인을 멈췄으면([checksStopped]) 그 토스트예요. */
    fun onAlarmSaved(id: Long, checksStopped: Boolean = false, restReleased: Boolean = false) {
        if (checksStopped) {
            overlay.update { it.copy(message = HomeMessage.ChecksStopped) }
            return
        }
        viewModelScope.launch {
            val alarm = book.get(id)
            val current = now.now()
            val until = alarm
                ?.let { NextRingCalculator.next(it, current, holidays) }
                ?.let { TimeUntil.between(current, it) }
            overlay.update { it.copy(message = HomeMessage.Saved(until, restReleased)) }
        }
    }

    /** 카드 토글. 반복 알람을 끄면 '이번만 쉬기'를 달고, 쉬는 중인 알람을 켜면 쉬기를 풀어요. */
    fun setEnabled(alarm: Alarm, enabled: Boolean) {
        // 결정 165: 울리는 중인 알람은 토글을 막고 울림 화면에서 끄라고 알려요. 카드는 그대로 켜진 모양이에요.
        if (ringing(alarm)) {
            showRinging()
            return
        }
        val resting = alarm.id in uiState.value.resting
        val offer = !enabled && alarm.isRepeating
        // 끄기 직전에 울리는 중이거나 남은 확인을 기다렸는지 봐요. 끄면 확인이 멈춰서 나중엔 알 수 없어요(결정 154).
        val today = offer && checks.isActive(alarm.id)
        overlay.update {
            it.copy(
                restOffers = if (offer) it.restOffers + alarm.id else it.restOffers - alarm.id,
                restToday = if (today) it.restToday + alarm.id else it.restToday - alarm.id,
            )
        }
        viewModelScope.launch {
            when {
                enabled && resting -> book.save(alarm.copy(skipOnceDate = null))
                enabled -> book.setEnabled(alarm.id, true)
                else -> {
                    // 끄기 전에 남은 확인을 먼저 멈춰서 있었는지 알아내요. 장부도 다시 부르지만 그땐 멈출 것이 없어요.
                    val stopped = stopChecks(listOf(alarm.id))
                    book.setEnabled(alarm.id, false)
                    if (stopped) showChecksStopped()
                }
            }
        }
    }

    /** [ids]의 남은 확인을 모두 멈춰요. 하나라도 멈춘 것이 있으면 true예요(결정 153). */
    private suspend fun stopChecks(ids: List<Long>): Boolean =
        ids.fold(false) { stopped, id -> checks.cancelFor(id) || stopped }

    private fun showChecksStopped() = overlay.update { it.copy(message = HomeMessage.ChecksStopped) }

    /**
     * H8 '이번만 쉬기': 다시 켜되 아직 오지 않은 다음 울림 하나를 쉬어요(결정 175, 결정 156을 되돌림).
     * 1. 울리는 중이거나 남은 확인을 기다리다 끈 알람은 그 울림의 날을 쉰 걸로 쳐요(자정을 넘겨도 울린 날, 결정 154).
     * 2. 그 밖에는 지금 뒤 다음 울림을 건너뛰어요. 화요일 6:30이 울린 뒤면 수요일을 쉬고 목요일에 울려요.
     * 누른 순간 쉬는 날과 다음 울림을 토스트로 한 번 알려요.
     * 하나 끄기, '모든 알람 끄기', 선택 끄기 모두 이 함수를 거쳐요.
     */
    fun restOnce(alarm: Alarm) {
        val today = alarm.id in overlay.value.restToday
        overlay.update { it.copy(restOffers = it.restOffers - alarm.id, restToday = it.restToday - alarm.id) }
        viewModelScope.launch {
            val on = alarm.copy(enabled = true, skipOnceDate = null)
            val at = now.now()
            val skip = if (today) lastRingDate(on, at) else restDay(on, at)
            val rested = on.copy(skipOnceDate = skip)
            book.save(rested)
            showRested(rested, at)
        }
    }

    /** 쉬는 날과 그 뒤 다음 울림을 토스트로 알려요. 쉴 날이나 다음 울림이 없으면 띄우지 않아요. */
    private fun showRested(rested: Alarm, at: ZonedDateTime) {
        val skip = rested.skipOnceDate ?: return
        val next = NextRingCalculator.next(rested, at, holidays) ?: return
        val far = ChronoUnit.DAYS.between(at.toLocalDate(), next.toLocalDate()) >= RestLabel.DATE_FROM_DAYS
        overlay.update { it.copy(message = HomeMessage.Rested(skip, next, far)) }
    }

    /** 더보기 '모든 알람 끄기'. 켜져 있던 반복 알람마다 '이번만 쉬기'를 달아요(H8-4). */
    fun turnAllOff() = turnOff(uiState.value.alarms)

    /** 더보기 '모든 알람 켜기'(모두 꺼져 있을 때만 보여요). 쉬는 중인 알람은 쉬기를 풀어요. */
    fun turnAllOn() = turnOn(uiState.value.alarms)

    private fun turnOff(alarms: List<Alarm>) {
        val state = uiState.value
        val (blocked, targets) = alarms.filter { state.shownOn(it) }.partition { ringing(it) }
        val repeating = targets.filter { it.isRepeating }.map { it.id }.toSet()
        val today = repeating.filter { checks.isActive(it) }.toSet()
        overlay.update { o ->
            o.copy(restOffers = o.restOffers + repeating, restToday = o.restToday - repeating + today)
        }
        viewModelScope.launch {
            val stopped = stopChecks(targets.map { it.id })
            targets.forEach { book.setEnabled(it.id, false) }
            // 울리는 알람은 빼고 껐어요(결정 165). 그 토스트를 먼저, 확인을 멈췄으면 이어서 띄워요.
            when {
                blocked.isNotEmpty() -> overlay.update {
                    it.copy(message = HomeMessage.Ringing, queued = HomeMessage.ChecksStopped.takeIf { stopped })
                }
                stopped -> showChecksStopped()
            }
        }
    }

    private fun turnOn(alarms: List<Alarm>) {
        val state = uiState.value
        // 울리는 중(쉬는 중, 미션 중 포함)인 알람은 홈 토글처럼 켜기도 먹지 않아요(결정 165, ISS-050).
        val (blocked, targets) = alarms.filterNot { state.shownOn(it) }.partition { ringing(it) }
        val ids = targets.map { it.id }.toSet()
        overlay.update { o -> o.copy(restOffers = o.restOffers - ids, restToday = o.restToday - ids) }
        viewModelScope.launch {
            targets.forEach {
                if (it.id in state.resting) book.save(it.copy(skipOnceDate = null)) else book.setEnabled(it.id, true)
            }
        }
        if (blocked.isNotEmpty()) showRinging()
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

    /**
     * 선택 모드 '이번만 쉬기'(결정 158): 고른 것 중 쉴 수 있는 알람만 켜진 채로 쉬어요. 기준은 결정 175예요.
     * 선택 모드는 닫혀요. 쉬는 알람이 하나뿐이면 쉬는 날 토스트를 띄우고, 여럿이면 띄우지 않아요
     * (카드가 쉬는 중 모양으로 바뀌어요).
     */
    fun restSelected() {
        val state = uiState.value
        val picked = selectedAlarms()
        val chosen = picked.filter { it.id in state.restable }
        endSelection()
        // 울리는 알람은 쉴 수 없어서 빠져요(결정 165). 그때만 알려요.
        if (picked.any { ringing(it) }) showRinging()
        if (chosen.isEmpty()) return
        val ids = chosen.map { it.id }.toSet()
        overlay.update { o -> o.copy(restOffers = o.restOffers - ids, restToday = o.restToday - ids) }
        viewModelScope.launch {
            val at = now.now()
            val rested = chosen.map { alarm ->
                val on = alarm.copy(skipOnceDate = null)
                on.copy(skipOnceDate = restDay(on, at)).also { book.save(it) }
            }
            if (rested.size == 1 && picked.none { ringing(it) }) showRested(rested.single(), at)
        }
    }

    /** '복제'(결정 161): 고른 알람을 같은 설정으로 하나씩 더 만들어요. 쉬기 날짜와 남은 확인은 따라가지 않아요. */
    fun copySelected() {
        val chosen = selectedAlarms()
        endSelection()
        if (chosen.isEmpty()) return
        viewModelScope.launch {
            chosen.forEach { book.save(it.copy(id = 0, skipOnceDate = null, nextTriggerAt = null)) }
            overlay.update { it.copy(message = HomeMessage.Copied(chosen.size)) }
        }
    }

    /**
     * '삭제': 묻지 않고 바로 지우고 되돌리기 스낵바(H7)를 띄워요(결정 160, H5 확인 창 없앰).
     * 스낵바가 떠 있는 중에 또 지우면 앞 삭제는 확정되고 새 스낵바로 바뀌어요. 줄 서 있던 토스트는 합쳐 한 번만이에요.
     */
    fun deleteSelected() {
        val (blocked, alarms) = selectedAlarms().partition { ringing(it) }
        endSelection()
        // 울리는 알람은 지우지 않아요(결정 165). 지운 것이 없으면 바로, 있으면 스낵바 뒤에 알려요.
        if (alarms.isEmpty()) {
            if (blocked.isNotEmpty()) showRinging()
            return
        }
        viewModelScope.launch {
            val stopped = stopChecks(alarms.map { it.id })
            val deleted = alarms.mapNotNull { book.delete(it.id) }
            overlay.update {
                // 앞 스낵바가 아직 떠 있으면 그 뒤에 줄 선 토스트를 이어받아요.
                val waiting = it.queued.takeIf { _ -> it.message is HomeMessage.Deleted }
                val after = when {
                    blocked.isNotEmpty() -> HomeMessage.Ringing
                    stopped -> HomeMessage.ChecksStopped
                    else -> waiting
                }
                when {
                    // '되돌리기'가 먼저 보이게 스낵바를 띄우고, 닫히면 토스트를 이어서 띄워요.
                    deleted.isNotEmpty() -> it.copy(message = HomeMessage.Deleted(deleted), queued = after)
                    stopped -> it.copy(message = HomeMessage.ChecksStopped)
                    else -> it
                }
            }
        }
    }

    fun undoDelete() {
        val message = overlay.value.message as? HomeMessage.Deleted ?: return
        // 되살려도 멈춘 확인은 돌아오지 않아서, 이어서 띄울 토스트는 그대로 띄워요.
        overlay.update { it.copy(message = it.queued, queued = null) }
        viewModelScope.launch { message.alarms.forEach { book.restore(it) } }
    }

    /** 토스트나 스낵바가 시간이 다 되었거나 밀어서 닫혔어요. */
    fun dismissMessage(message: HomeMessage) =
        overlay.update { if (it.message == message) it.copy(message = it.queued, queued = null) else it }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /** 마지막 울림을 찾을 때 거슬러 보는 날 수. 매주 한 번인 알람도 넘어요. */
        const val LOOK_BACK_DAYS = 8L
    }
}
