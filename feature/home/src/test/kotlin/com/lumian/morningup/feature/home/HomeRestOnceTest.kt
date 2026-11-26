package com.lumian.morningup.feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.MinuteTicker
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.engine.HolidayCalendar
import com.lumian.morningup.core.testing.FakeAlarmBook
import com.lumian.morningup.core.testing.FakeWakeCheckGate
import com.lumian.morningup.core.testing.MainDispatcherRule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** 결정 175 '이번만 쉬기' 예시와 쉬는 날 토스트. */
class HomeRestOnceTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val seoul = ZoneId.of("Asia/Seoul")
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
    private val checks = FakeWakeCheckGate()

    private fun viewModel(book: FakeAlarmBook) = HomeViewModel(book, now, noHolidays, ticker, NoWeather, checks)

    /** [at]에 [alarm]을 홈 토글로 끄고 '이번만 쉬기'를 누른 뒤, 쉬는 중이 된 홈 상태. */
    private suspend fun rested(alarm: Alarm, at: ZonedDateTime): HomeUiState {
        clock = at
        val vm = viewModel(FakeAlarmBook(listOf(alarm)))
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
    fun restOnce_decision175_examples() = runTest {
        // 화요일 06:30이 울린 뒤 누르면 수요일을 쉬고 목요일에 울려요.
        rested(a630, ZonedDateTime.of(2026, 11, 17, 6, 40, 0, 0, seoul)).let {
            assertThat(it.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 18))
            assertThat(it.next!!.at).isEqualTo(ZonedDateTime.of(2026, 11, 19, 6, 30, 0, 0, seoul))
        }
        // 수요일 밤에 누르면 목요일을 쉬고 금요일에 울려요.
        rested(a630, ZonedDateTime.of(2026, 11, 18, 22, 0, 0, 0, seoul)).let {
            assertThat(it.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 19))
            assertThat(it.next!!.at).isEqualTo(ZonedDateTime.of(2026, 11, 20, 6, 30, 0, 0, seoul))
        }
        // 금요일 밤에 누르면 다음 울림(월)을 쉬고 화요일에 울려요.
        rested(a630, ZonedDateTime.of(2026, 11, 20, 22, 0, 0, 0, seoul)).let {
            assertThat(it.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 23))
            assertThat(it.next!!.at).isEqualTo(ZonedDateTime.of(2026, 11, 24, 6, 30, 0, 0, seoul))
        }
    }

    @Test
    fun restOnce_showsRestedToast_once() = runTest {
        // 수요일 밤에 누른 순간 '목요일은 쉬어요. 금요일 오전 6:30에 울려요' 토스트를 한 번 띄워요(결정 175).
        clock = ZonedDateTime.of(2026, 11, 18, 22, 0, 0, 0, seoul)
        val vm = viewModel(FakeAlarmBook(listOf(a630)))
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.setEnabled(a630, false)
            var state = awaitItem()
            while (state.alarms.first().enabled) state = awaitItem()
            vm.restOnce(state.alarms.first())
            while (state.message !is HomeMessage.Rested) state = awaitItem()
            assertThat(state.message).isEqualTo(
                HomeMessage.Rested(
                    skip = LocalDate.of(2026, 11, 19),
                    next = ZonedDateTime.of(2026, 11, 20, 6, 30, 0, 0, seoul),
                    nextOnDate = false,
                ),
            )
            vm.dismissMessage(state.message!!)
            while (state.message != null) state = awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun restOnce_weeklyAlarm_toastShowsDate() = runTest {
        // 토요일만 울리는 알람을 토요일 밤에 쉬면 다음 울림이 12월 5일이라 날짜로 알려요(결정 162 경계).
        clock = ZonedDateTime.of(2026, 11, 21, 22, 0, 0, 0, seoul)
        val vm = viewModel(FakeAlarmBook(listOf(a710)))
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.setEnabled(a710, false)
            var state = awaitItem()
            while (state.alarms.first().enabled) state = awaitItem()
            vm.restOnce(state.alarms.first())
            while (state.message !is HomeMessage.Rested) state = awaitItem()
            val shown = state.message as HomeMessage.Rested
            assertThat(shown.skip).isEqualTo(LocalDate.of(2026, 11, 28))
            assertThat(shown.next.toLocalDate()).isEqualTo(LocalDate.of(2026, 12, 5))
            assertThat(shown.nextOnDate).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
