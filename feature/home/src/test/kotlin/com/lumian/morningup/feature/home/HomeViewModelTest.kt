package com.lumian.morningup.feature.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.MinuteTicker
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.engine.HolidayCalendar
import com.lumian.morningup.core.model.engine.TimeUntil
import com.lumian.morningup.core.testing.FakeAlarmBook
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
        Alarm(id = 3, time = LocalTime.of(8, 0), enabled = false, missions = listOf(MissionType.QuickMath))

    private fun viewModel(book: FakeAlarmBook) = HomeViewModel(book, now, noHolidays, ticker)

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
            val state = awaitItem()
            assertThat(book.calls).containsExactly("enable:1:false")
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
            vm.requestDelete(a800)
            assertThat(awaitItem().pendingDelete).isEqualTo(a800)
            assertThat(book.calls).isEmpty()

            vm.confirmDelete()
            var state = awaitItem()
            while (state.message == null) state = awaitItem()
            assertThat(state.pendingDelete).isNull()
            assertThat(state.alarms.map { it.id }).containsExactly(1L)
            assertThat(state.message).isEqualTo(HomeMessage.Deleted(a800))

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
            vm.requestDelete(a630)
            awaitItem()
            vm.cancelDelete()
            assertThat(awaitItem().pendingDelete).isNull()
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
}
