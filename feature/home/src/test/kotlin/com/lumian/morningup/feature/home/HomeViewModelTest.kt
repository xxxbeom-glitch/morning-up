package com.lumian.morningup.feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.MinuteTicker
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.WeatherKind
import com.lumian.morningup.core.model.WeatherSource
import com.lumian.morningup.core.model.engine.HolidayCalendar
import com.lumian.morningup.core.model.engine.TimeUntil
import com.lumian.morningup.core.testing.FakeAlarmBook
import com.lumian.morningup.core.testing.FakeWakeCheckGate
import com.lumian.morningup.core.testing.MainDispatcherRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val seoul = ZoneId.of("Asia/Seoul")

    /** 2026-11-16 월요일 23:10. */
    private var clock = ZonedDateTime.of(2026, 11, 16, 23, 10, 0, 0, seoul)
    private val now = NowProvider { clock }
    private val ticks = MutableSharedFlow<Unit>()
    private val ticker = MinuteTicker { ticks }
    private val noHolidays = object : HolidayCalendar {
        override fun isHoliday(date: LocalDate) = false

        override fun covers(year: Int) = true
    }
    private val weekdays = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    )
    private val a630 = Alarm(id = 1, time = LocalTime.of(6, 30), repeatDays = weekdays)
    private val a710 = Alarm(id = 2, time = LocalTime.of(7, 10), repeatDays = setOf(DayOfWeek.SATURDAY))
    private val a800 =
        Alarm(
            id = 3,
            time = LocalTime.of(8, 0),
            enabled = false,
            missions = listOf(MissionSetting(MissionType.QuickMath)),
        )

    private val checks = FakeWakeCheckGate()

    private fun viewModel(book: FakeAlarmBook) = HomeViewModel(book, now, noHolidays, ticker, NoWeather, checks)

    @Test
    fun showsNextAlarm_inHoursAndMinutes() = runTest {
        val vm = viewModel(FakeAlarmBook(listOf(a630, a710, a800)))
        vm.uiState.test {
            val state = awaitItem().let { if (it.loading) awaitItem() else it }
            assertThat(state.alarms.map { it.id }).containsExactly(1L, 2L, 3L).inOrder()
            assertThat(state.next!!.alarm.id).isEqualTo(1)
            assertThat(state.next!!.until).isEqualTo(TimeUntil(0, 7, 20))
            assertThat(state.today).isEqualTo(LocalDate.of(2026, 11, 16))
        }
    }

    @Test
    fun minuteTick_recalculates() = runTest {
        val vm = viewModel(FakeAlarmBook(listOf(a630)))
        vm.uiState.test {
            val first = awaitItem().let { if (it.loading) awaitItem() else it }
            assertThat(first.next!!.until).isEqualTo(TimeUntil(0, 7, 20))
            clock = clock.plusMinutes(1)
            ticks.emit(Unit)
            assertThat(awaitItem().next!!.until).isEqualTo(TimeUntil(0, 7, 19))
        }
    }

    @Test
    fun noEnabledAlarm_hasNoNext() = runTest {
        val vm = viewModel(FakeAlarmBook(listOf(a800)))
        vm.uiState.test {
            val state = awaitItem().let { if (it.loading) awaitItem() else it }
            assertThat(state.next).isNull()
            assertThat(state.alarms).hasSize(1)
        }
    }

    @Test
    fun toggle_savesThroughBook_andMovesNext() = runTest {
        val book = FakeAlarmBook(listOf(a630, a710))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.setEnabled(a630, false)
            var state = awaitItem()
            while (state.next?.alarm?.id != 2L) state = awaitItem()
            assertThat(book.calls).containsExactly("enable:1:false")
            // 반복 알람을 끄면 '이번만 쉬기'가 붙어요(H8 Rest Offer).
            assertThat(state.restOffers).containsExactly(1L)
            // 평일 06:30이 꺼지면 다음은 토요일 07:10이에요.
            assertThat(state.next!!.alarm.id).isEqualTo(2)
        }
    }

    @Test
    fun delete_asksFirst_thenDeletes_andUndoRestores() = runTest {
        val book = FakeAlarmBook(listOf(a630, a800))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.startSelection(a800)
            awaitItem()
            vm.deleteSelected()
            assertThat(awaitItem().pendingDelete).containsExactly(a800)
            assertThat(book.calls).isEmpty()

            vm.confirmDelete()
            var state = awaitItem()
            while (state.message == null) state = awaitItem()
            assertThat(state.pendingDelete).isEmpty()
            assertThat(state.selection).isNull()
            assertThat(state.alarms.map { it.id }).containsExactly(1L)
            assertThat(state.message).isEqualTo(HomeMessage.Deleted(listOf(a800)))

            vm.undoDelete()
            state = awaitItem()
            while (state.alarms.size < 2) state = awaitItem()
            assertThat(state.message).isNull()
            assertThat(book.calls).containsExactly("delete:3", "restore:3").inOrder()
        }
    }

    @Test
    fun cancelDelete_keepsAlarm() = runTest {
        val book = FakeAlarmBook(listOf(a630))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.startSelection(a630)
            awaitItem()
            vm.deleteSelected()
            awaitItem()
            vm.cancelDelete()
            assertThat(awaitItem().pendingDelete).isEmpty()
            assertThat(book.calls).isEmpty()
        }
    }

    @Test
    fun savedAlarm_showsToastWithTimeUntil_thenDismisses() = runTest {
        val vm = viewModel(FakeAlarmBook(listOf(a630)))
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.onAlarmSaved(1)
            val message = awaitItem().message
            assertThat(message).isEqualTo(HomeMessage.Saved(TimeUntil(0, 7, 20)))
            vm.dismissMessage(message!!)
            assertThat(awaitItem().message).isNull()
        }
    }

    @Test
    fun savedAlarm_thatStoppedChecks_showsChecksStoppedToast() = runTest {
        val vm = viewModel(FakeAlarmBook(listOf(a630)))
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.onAlarmSaved(1, checksStopped = true)
            assertThat(awaitItem().message).isEqualTo(HomeMessage.ChecksStopped)
        }
    }

    @Test
    fun toggleOff_withPendingChecks_showsChecksStoppedToast() = runTest {
        checks.pendingFor = setOf(1L)
        val book = FakeAlarmBook(listOf(a630))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.setEnabled(a630, false)
            var state = awaitItem()
            while (state.message == null) state = awaitItem()
            assertThat(state.message).isEqualTo(HomeMessage.ChecksStopped)
            assertThat(checks.calls).contains("cancel:1")
            assertThat(state.alarms.single().enabled).isFalse()
        }
    }

    @Test
    fun toggleOff_withoutPendingChecks_showsNoToast() = runTest {
        val book = FakeAlarmBook(listOf(a630))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.setEnabled(a630, false)
            var state = awaitItem()
            while (state.alarms.single().enabled) state = awaitItem()
            assertThat(state.message).isNull()
            expectNoEvents()
        }
    }

    @Test
    fun turnAllOff_withPendingChecks_showsChecksStoppedToast() = runTest {
        checks.pendingFor = setOf(2L)
        val vm = viewModel(FakeAlarmBook(listOf(a630, a710)))
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.turnAllOff()
            var state = awaitItem()
            while (state.message == null) state = awaitItem()
            assertThat(state.message).isEqualTo(HomeMessage.ChecksStopped)
        }
    }

    @Test
    fun delete_withPendingChecks_showsSnackbarThenChecksStoppedToast() = runTest {
        checks.pendingFor = setOf(3L)
        val book = FakeAlarmBook(listOf(a630, a800))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.startSelection(a800)
            awaitItem()
            vm.deleteSelected()
            awaitItem()
            vm.confirmDelete()
            var state = awaitItem()
            while (state.message == null) state = awaitItem()
            val deleted = state.message
            assertThat(deleted).isEqualTo(HomeMessage.Deleted(listOf(a800)))
            assertThat(checks.calls).contains("cancel:3")

            vm.dismissMessage(deleted!!)
            assertThat(awaitItem().message).isEqualTo(HomeMessage.ChecksStopped)
            vm.dismissMessage(HomeMessage.ChecksStopped)
            assertThat(awaitItem().message).isNull()
        }
    }

    @Test
    fun delete_withPendingChecks_thenUndo_stillShowsChecksStoppedToast() = runTest {
        checks.pendingFor = setOf(3L)
        val book = FakeAlarmBook(listOf(a630, a800))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.startSelection(a800)
            awaitItem()
            vm.deleteSelected()
            awaitItem()
            vm.confirmDelete()
            var state = awaitItem()
            while (state.message == null) state = awaitItem()
            vm.undoDelete()
            state = awaitItem()
            assertThat(state.message).isEqualTo(HomeMessage.ChecksStopped)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun restOnce_afterRingWhileChecksPending_restsToday_andRingsNextNormalDay() = runTest {
        // 수요일 06:30 알람이 울린 뒤 07:00, 남은 확인을 기다리는 중이에요.
        clock = ZonedDateTime.of(2026, 11, 18, 7, 0, 0, 0, seoul)
        checks.pendingFor = setOf(1L)
        val book = FakeAlarmBook(listOf(a630))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.setEnabled(a630, false)
            var state = awaitItem()
            while (state.alarms.first().enabled) state = awaitItem()
            // 토글로 끄면서 확인은 멈췄지만, '이번만 쉬기'는 끄기 직전 상태로 판단해요.
            assertThat(checks.pendingFor).isEmpty()
            vm.restOnce(state.alarms.first())
            while (1L !in state.resting) state = awaitItem()
            assertThat(state.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 18))
            // 목요일 06:30까지 23시간 30분이에요. 홈 위쪽 문구와 같은 값이에요.
            assertThat(state.next!!.at).isEqualTo(ZonedDateTime.of(2026, 11, 19, 6, 30, 0, 0, seoul))
            assertThat(state.resting[1L]).isEqualTo(RestLabel.Within(TimeUntil(0, 23, 30)))
            assertThat(state.next!!.until).isEqualTo(TimeUntil(0, 23, 30))
            assertThat(state.shownOn(state.alarms.first())).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun restOnce_whileRinging_restsToday() = runTest {
        clock = ZonedDateTime.of(2026, 11, 18, 6, 31, 0, 0, seoul)
        checks.ringingFor = setOf(1L)
        val book = FakeAlarmBook(listOf(a630))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.setEnabled(a630, false)
            var state = awaitItem()
            while (state.alarms.first().enabled) state = awaitItem()
            vm.restOnce(state.alarms.first())
            while (1L !in state.resting) state = awaitItem()
            assertThat(state.next!!.at.toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 19))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun restOnce_afterChecksEnded_onRingDay_restsToday_andRingsTomorrow() = runTest {
        // 수요일 07:00, 06:30 울림과 남은 확인이 모두 끝났어요. 오늘(수)을 쉰 걸로 쳐서 목요일에 울려요(결정 156).
        val state = rested(a630, ZonedDateTime.of(2026, 11, 18, 7, 0, 0, 0, seoul))
        assertThat(state.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 18))
        assertThat(state.next!!.at).isEqualTo(ZonedDateTime.of(2026, 11, 19, 6, 30, 0, 0, seoul))
        assertThat(state.resting[1L]).isEqualTo(RestLabel.Within(TimeUntil(0, 23, 30)))
        assertThat(state.next!!.until).isEqualTo(TimeUntil(0, 23, 30))
    }

    @Test
    fun restOnce_ringDayAfternoon_ringsTomorrow() = runTest {
        // 사용자가 찾은 경우: 수요일 오후에 누르면 '금요일'이 아니라 목요일에 울려요(ISS-049).
        val state = rested(a630, ZonedDateTime.of(2026, 11, 18, 15, 0, 0, 0, seoul))
        assertThat(state.next!!.at).isEqualTo(ZonedDateTime.of(2026, 11, 19, 6, 30, 0, 0, seoul))
        assertThat(state.resting[1L]).isEqualTo(RestLabel.Within(TimeUntil(0, 15, 30)))
    }

    @Test
    fun restOnce_aroundRingTime() = runTest {
        val wed = LocalDate.of(2026, 11, 18)
        val thu = ZonedDateTime.of(2026, 11, 19, 6, 30, 0, 0, seoul)
        // 울림 직전(06:29:59)은 오늘 울림만 건너뛰어요.
        rested(a630, ZonedDateTime.of(2026, 11, 18, 6, 29, 59, 0, seoul)).let {
            assertThat(it.alarms.first().skipOnceDate).isEqualTo(wed)
            assertThat(it.next!!.at).isEqualTo(thu)
            assertThat(it.resting[1L]).isEqualTo(RestLabel.OnDay(DayOfWeek.THURSDAY))
        }
        // 울림 시각 그때(06:30:00)와 직후(06:30:01)도 목요일이에요.
        rested(a630, ZonedDateTime.of(2026, 11, 18, 6, 30, 0, 0, seoul)).let {
            assertThat(it.alarms.first().skipOnceDate).isEqualTo(wed)
            assertThat(it.next!!.at).isEqualTo(thu)
        }
        rested(a630, ZonedDateTime.of(2026, 11, 18, 6, 30, 1, 0, seoul)).let {
            assertThat(it.next!!.at).isEqualTo(thu)
            assertThat(it.resting[1L]).isEqualTo(RestLabel.OnDay(DayOfWeek.THURSDAY))
        }
    }

    @Test
    fun restOnce_aroundMidnight() = runTest {
        // 수요일 23:59는 수요일을 쉬어서 목요일 06:30(6시간 31분 뒤)에 울려요.
        rested(a630, ZonedDateTime.of(2026, 11, 18, 23, 59, 0, 0, seoul)).let {
            assertThat(it.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 18))
            assertThat(it.next!!.at).isEqualTo(ZonedDateTime.of(2026, 11, 19, 6, 30, 0, 0, seoul))
            assertThat(it.resting[1L]).isEqualTo(RestLabel.Within(TimeUntil(0, 6, 31)))
        }
        // 목요일 00:00은 이미 목요일이라 목요일 울림을 쉬고 금요일에 울려요.
        rested(a630, ZonedDateTime.of(2026, 11, 19, 0, 0, 0, 0, seoul)).let {
            assertThat(it.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 19))
            assertThat(it.next!!.at).isEqualTo(ZonedDateTime.of(2026, 11, 20, 6, 30, 0, 0, seoul))
        }
    }

    @Test
    fun restOnce_onWeekend_skipsNextRing() = runTest {
        // 토, 일은 울리지 않는 날이라 다음 울림(월)을 쉬고 화요일에 울려요.
        val tue = ZonedDateTime.of(2026, 11, 24, 6, 30, 0, 0, seoul)
        rested(a630, ZonedDateTime.of(2026, 11, 21, 10, 0, 0, 0, seoul)).let {
            assertThat(it.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 23))
            assertThat(it.next!!.at).isEqualTo(tue)
            assertThat(it.resting[1L]).isEqualTo(RestLabel.OnDay(DayOfWeek.TUESDAY))
        }
        rested(a630, ZonedDateTime.of(2026, 11, 22, 23, 59, 0, 0, seoul)).let {
            assertThat(it.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 23))
            assertThat(it.next!!.at).isEqualTo(tue)
        }
    }

    @Test
    fun restOnce_onHoliday_followsHolidayRule() = runTest {
        val wedHoliday = object : HolidayCalendar {
            override fun isHoliday(date: LocalDate) = date == LocalDate.of(2026, 11, 18)

            override fun covers(year: Int) = true
        }
        val at = ZonedDateTime.of(2026, 11, 18, 10, 0, 0, 0, seoul)
        // 공휴일에 쉬는 알람은 수요일이 울리지 않는 날이라 다음 울림(목)을 쉬고 금요일에 울려요.
        rested(a630.copy(skipHolidays = true), at, wedHoliday).let {
            assertThat(it.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 19))
            assertThat(it.next!!.at).isEqualTo(ZonedDateTime.of(2026, 11, 20, 6, 30, 0, 0, seoul))
        }
        // 공휴일에도 울리는 알람은 수요일이 울리는 날이라 목요일에 울려요.
        rested(a630, at, wedHoliday).let {
            assertThat(it.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 18))
            assertThat(it.next!!.at).isEqualTo(ZonedDateTime.of(2026, 11, 19, 6, 30, 0, 0, seoul))
        }
    }

    @Test
    fun restOnce_checksPastMidnight_restsRingDay() = runTest {
        // 수요일 06:30에 울리고 확인이 목요일 00:10까지 이어졌어요. 울린 날(수) 기준이라 목요일 06:30에 울려요.
        checks.pendingFor = setOf(1L)
        val state = rested(a630, ZonedDateTime.of(2026, 11, 19, 0, 10, 0, 0, seoul))
        assertThat(state.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 18))
        assertThat(state.next!!.at).isEqualTo(ZonedDateTime.of(2026, 11, 19, 6, 30, 0, 0, seoul))
        assertThat(state.resting[1L]).isEqualTo(RestLabel.Within(TimeUntil(0, 6, 20)))
    }

    @Test
    fun restOnce_afterTurnAllOff_usesSameRule() = runTest {
        // '모든 알람 끄기'에서 생긴 '이번만 쉬기'도 수요일 오후면 목요일에 울려요.
        clock = ZonedDateTime.of(2026, 11, 18, 15, 0, 0, 0, seoul)
        val vm = viewModel(FakeAlarmBook(listOf(a630)))
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.turnAllOff()
            var state = awaitItem()
            while (state.alarms.first().enabled) state = awaitItem()
            vm.restOnce(state.alarms.first())
            while (1L !in state.resting) state = awaitItem()
            assertThat(state.next!!.at).isEqualTo(ZonedDateTime.of(2026, 11, 19, 6, 30, 0, 0, seoul))
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** [at]에 [alarm]을 홈 토글로 끄고 '이번만 쉬기'를 누른 뒤, 쉬는 중이 된 홈 상태. */
    private suspend fun rested(alarm: Alarm, at: ZonedDateTime, holidays: HolidayCalendar = noHolidays): HomeUiState {
        clock = at
        val vm = HomeViewModel(FakeAlarmBook(listOf(alarm)), now, holidays, ticker, NoWeather, checks)
        lateinit var result: HomeUiState
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.setEnabled(alarm, false)
            var state = awaitItem()
            while (state.alarms.first().enabled) state = awaitItem()
            vm.restOnce(state.alarms.first())
            while (alarm.id !in state.resting) state = awaitItem()
            result = state
            cancelAndIgnoreRemainingEvents()
        }
        return result
    }

    @Test
    fun restedToday_endsAfterNextRing() = runTest {
        // 수요일 것을 쉰 걸로 친 알람이 목요일 06:30에 울리고 나면 보통 카드로 돌아가요.
        clock = ZonedDateTime.of(2026, 11, 19, 6, 31, 0, 0, seoul)
        val rested = a630.copy(skipOnceDate = LocalDate.of(2026, 11, 18))
        val vm = viewModel(FakeAlarmBook(listOf(rested)))
        vm.uiState.test {
            val state = awaitItem().let { if (it.loading) awaitItem() else it }
            assertThat(state.resting).isEmpty()
        }
    }

    @Test
    fun restLabel_boundaries() {
        val ring = ZonedDateTime.of(2026, 11, 19, 6, 30, 0, 0, seoul)
        fun at(minutesBefore: Long, seconds: Long = 0) = ring.minusMinutes(minutesBefore).plusSeconds(seconds)
        // 24시간 넘으면 요일, 딱 24시간도 요일이에요.
        assertThat(RestLabel.between(at(24 * 60 + 1), ring)).isEqualTo(RestLabel.OnDay(DayOfWeek.THURSDAY))
        assertThat(RestLabel.between(at(24 * 60), ring)).isEqualTo(RestLabel.OnDay(DayOfWeek.THURSDAY))
        // 24시간 안은 시간과 분을 같이 보여요.
        assertThat(RestLabel.between(at(24 * 60 - 1), ring)).isEqualTo(RestLabel.Within(TimeUntil(0, 23, 59)))
        assertThat(RestLabel.between(at(19 * 60 + 43), ring)).isEqualTo(RestLabel.Within(TimeUntil(0, 19, 43)))
        // 분이 0이면 'N시간 뒤', 1시간 안이면 'N분 뒤'(글은 AlarmText.until이 0인 칸을 빼요).
        assertThat(RestLabel.between(at(60), ring)).isEqualTo(RestLabel.Within(TimeUntil(0, 1, 0)))
        assertThat(RestLabel.between(at(59), ring)).isEqualTo(RestLabel.Within(TimeUntil(0, 0, 59)))
        // 분은 홈 위쪽 문구처럼 올림이에요. 30초 남아도 1분이에요.
        assertThat(RestLabel.between(at(2, 30), ring)).isEqualTo(RestLabel.Within(TimeUntil(0, 0, 2)))
        assertThat(RestLabel.between(ring.minusSeconds(30), ring)).isEqualTo(RestLabel.Within(TimeUntil(0, 0, 1)))
    }

    @Test
    fun recheckBanner_followsGate_andOpensW3() = runTest {
        val vm = viewModel(FakeAlarmBook(listOf(a630)))
        vm.recheckPending.test {
            assertThat(awaitItem()).isFalse()
            checks.recheckPending.value = true
            assertThat(awaitItem()).isTrue()
        }
        vm.openRecheck()
        assertThat(checks.calls).containsExactly("open")
    }

    @Test
    fun restOnce_ringDayNight_restsToday_andShowsTimeLeft() = runTest {
        val book = FakeAlarmBook(listOf(a630))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.setEnabled(a630, false)
            var state = awaitItem()
            while (state.alarms.first().enabled) state = awaitItem()
            vm.restOnce(state.alarms.first())
            while (1L !in state.resting) state = awaitItem()
            // 월요일 밤(울리는 날, 이미 울림)에 쉬면 월요일을 쉰 걸로 치고 화요일에 울려요(결정 156).
            assertThat(state.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 16))
            // 월요일 23:10부터 화요일 06:30까지 7시간 20분이에요.
            assertThat(state.resting[1L]).isEqualTo(RestLabel.Within(TimeUntil(0, 7, 20)))
            assertThat(state.shownOn(state.alarms.first())).isFalse()
            assertThat(state.restOffers).isEmpty()
            assertThat(state.next!!.at.toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 17))

            // 쉬는 중인 카드의 토글을 켜면 쉬기를 풀어요.
            vm.setEnabled(state.alarms.first(), true)
            while (1L in state.resting) state = awaitItem()
            assertThat(state.alarms.first().skipOnceDate).isNull()
            assertThat(book.calls).containsExactly("enable:1:false", "save:1", "save:1").inOrder()
        }
    }

    @Test
    fun selection_turnOffSelected_offersRestForRepeating() = runTest {
        val book = FakeAlarmBook(listOf(a630, a710, a800))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.startSelection(a630)
            assertThat(awaitItem().selection).containsExactly(1L)
            vm.toggleSelected(a800)
            assertThat(awaitItem().selection).containsExactly(1L, 3L)
            vm.toggleSelectAll()
            assertThat(awaitItem().selection).containsExactly(1L, 2L, 3L)
            vm.turnOffSelected()
            var state = awaitItem()
            while (state.alarms.any { it.id != 3L && it.enabled }) state = awaitItem()
            assertThat(state.selection).isNull()
            // 이미 꺼져 있던 8:00은 그대로, 켜져 있던 반복 알람마다 '이번만 쉬기'(H8-4).
            assertThat(state.restOffers).containsExactly(1L, 2L)
            assertThat(book.calls).containsExactly("enable:1:false", "enable:2:false")
        }
    }

    @Test
    fun turnAllOn_afterTurnAllOff_turnsEverythingBackOn() = runTest {
        val book = FakeAlarmBook(listOf(a630, a710, a800))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.turnAllOff()
            var state = awaitItem()
            while (state.alarms.any { it.enabled }) state = awaitItem()
            // 모두 꺼진 뒤 '모든 알람 켜기'는 원래 꺼져 있던 8:00까지 모두 켜고 '이번만 쉬기'를 거둬요.
            vm.turnAllOn()
            while (state.alarms.any { !it.enabled } || state.restOffers.isNotEmpty()) state = awaitItem()
            assertThat(state.alarms.all { state.shownOn(it) }).isTrue()
            assertThat(book.calls).containsExactly(
                "enable:1:false", "enable:2:false", "enable:1:true", "enable:2:true", "enable:3:true",
            )
        }
    }

    @Test
    fun copySelected_makesNewAlarms_andToasts() = runTest {
        val book = FakeAlarmBook(listOf(a630))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.startSelection(a630)
            awaitItem()
            vm.copySelected()
            var state = awaitItem()
            while (state.message == null) state = awaitItem()
            assertThat(state.alarms).hasSize(2)
            assertThat(state.message).isEqualTo(HomeMessage.Copied(1))
        }
    }
}

/** 날씨 없는 시험용 출처(홈은 맑은 하늘). */
private object NoWeather : WeatherSource {
    override val weather: StateFlow<WeatherKind?> = MutableStateFlow(null)

    override fun refresh(force: Boolean) = Unit
}
