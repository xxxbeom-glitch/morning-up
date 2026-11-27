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

@Suppress("LargeClass")
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
    fun restSelected_restsOnlyRestable_keepsOthersOn_andClosesSelection() = runTest {
        // 월 23:10: 평일 06:30은 다음 울림(화)을, 토 07:10은 다음 토요일을 쉬어요. 꺼진 8:00은 빠져요(결정 158, 175).
        val book = FakeAlarmBook(listOf(a630, a710, a800))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.startSelection()
            awaitItem()
            vm.toggleSelectAll()
            assertThat(awaitItem().restable).containsExactly(1L, 2L)
            vm.restSelected()
            var state = awaitItem()
            while (2L !in state.resting || 1L !in state.resting) state = awaitItem()
            assertThat(state.selection).isNull()
            // 둘 이상 쉬면 쉬는 날 토스트는 띄우지 않아요.
            assertThat(state.message).isNull()
            assertThat(book.get(2)!!.skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 21))
            assertThat(book.get(2)!!.enabled).isTrue()
            assertThat(book.get(1)!!.skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 17))
            assertThat(book.get(3)!!.skipOnceDate).isNull()
        }
    }

    @Test
    fun restable_beforeTodaysRing_restsToday_butNotWhileChecksPendingOrResting() = runTest {
        clock = ZonedDateTime.of(2026, 11, 16, 5, 0, 0, 0, seoul)
        val resting = a710.copy(id = 4, skipOnceDate = LocalDate.of(2026, 11, 21))
        checks.pendingFor = setOf(2L)
        val book = FakeAlarmBook(listOf(a630, a710, resting))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.startSelection()
            awaitItem()
            vm.toggleSelectAll()
            // 쉬는 중인 4는 '켜기'로 풀고, 확인을 기다리는 2는 쉴 것이 없어요.
            assertThat(awaitItem().restable).containsExactly(1L)
            vm.restSelected()
            var state = awaitItem()
            while (1L !in state.resting) state = awaitItem()
            assertThat(book.get(1)!!.skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 16))
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
    fun delete_deletesRightAway_andUndoRestores() = runTest {
        val book = FakeAlarmBook(listOf(a630, a800))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.startSelection(a800)
            awaitItem()
            vm.deleteSelected()
            var state = awaitItem()
            while (state.message == null) state = awaitItem()
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
    fun delete_again_whileSnackbar_replacesIt_andKeepsQueuedToastOnce() = runTest {
        // 결정 160 5번: 앞 스낵바 뒤에 줄 선 '남은 확인을 멈췄어요'는 새 스낵바 뒤로 한 번만 넘어가요.
        checks.pendingFor = setOf(3L)
        val book = FakeAlarmBook(listOf(a630, a800))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.startSelection(a800)
            awaitItem()
            vm.deleteSelected()
            var state = awaitItem()
            while (state.message == null) state = awaitItem()
            vm.startSelection(a630)
            vm.deleteSelected()
            while (state.message != HomeMessage.Deleted(listOf(a630))) state = awaitItem()
            vm.dismissMessage(state.message!!)
            assertThat(awaitItem().message).isEqualTo(HomeMessage.ChecksStopped)
            vm.dismissMessage(HomeMessage.ChecksStopped)
            assertThat(awaitItem().message).isNull()
            assertThat(book.calls).containsExactly("delete:3", "delete:1").inOrder()
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
    fun releasedInW4_showsChecksStoppedToastOnce() = runTest {
        // TSK-266: W4로 재확인을 해제하고 홈에 오면 토스트를 한 번 띄우고 표시를 지워요.
        val vm = viewModel(FakeAlarmBook(listOf(a630)))
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            checks.releaseNotice.value = true
            var state = awaitItem()
            while (state.message == null) state = awaitItem()
            assertThat(state.message).isEqualTo(HomeMessage.ChecksStopped)
            assertThat(checks.releaseNotice.value).isFalse()
            assertThat(checks.calls).contains("release_seen")
        }
    }

    @Test
    fun releaseBanner_opensW4() {
        viewModel(FakeAlarmBook(listOf(a630))).openRelease()
        assertThat(checks.calls).contains("open_release")
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
    fun restOnce_whileChecksPending_restsToday() = runTest {
        // 울리는 중에는 끄기를 막아서(결정 165), 오늘 것을 쉰 걸로 치는 것은 W2, W3를 기다리다 끈 경우예요(결정 154).
        clock = ZonedDateTime.of(2026, 11, 18, 6, 31, 0, 0, seoul)
        checks.pendingFor = setOf(1L)
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
    fun restOnce_afterChecksEnded_onRingDay_skipsTomorrow() = runTest {
        // 수요일 07:00, 06:30 울림과 남은 확인이 모두 끝났어요. 다음 울림(목)을 쉬고 금요일에 울려요(결정 175).
        val state = rested(a630, ZonedDateTime.of(2026, 11, 18, 7, 0, 0, 0, seoul))
        assertThat(state.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 19))
        assertThat(state.next!!.at).isEqualTo(ZonedDateTime.of(2026, 11, 20, 6, 30, 0, 0, seoul))
        assertThat(state.resting[1L]).isEqualTo(RestLabel.OnDay(DayOfWeek.FRIDAY))
        assertThat(state.next!!.until).isEqualTo(TimeUntil(1, 23, 30))
    }

    @Test
    fun restOnce_ringDayAfternoon_skipsTomorrow() = runTest {
        // 수요일 오후에 누르면 아직 오지 않은 목요일을 쉬고 금요일에 울려요(결정 175, 156을 되돌림).
        val state = rested(a630, ZonedDateTime.of(2026, 11, 18, 15, 0, 0, 0, seoul))
        assertThat(state.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 19))
        assertThat(state.next!!.at).isEqualTo(ZonedDateTime.of(2026, 11, 20, 6, 30, 0, 0, seoul))
        assertThat(state.resting[1L]).isEqualTo(RestLabel.OnDay(DayOfWeek.FRIDAY))
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
        // 울림 시각 그때(06:30:00)와 직후(06:30:01)는 수요일 울림이 왔으니 목요일을 쉬고 금요일이에요(결정 175).
        val fri = ZonedDateTime.of(2026, 11, 20, 6, 30, 0, 0, seoul)
        rested(a630, ZonedDateTime.of(2026, 11, 18, 6, 30, 0, 0, seoul)).let {
            assertThat(it.alarms.first().skipOnceDate).isEqualTo(thu.toLocalDate())
            assertThat(it.next!!.at).isEqualTo(fri)
        }
        rested(a630, ZonedDateTime.of(2026, 11, 18, 6, 30, 1, 0, seoul)).let {
            assertThat(it.next!!.at).isEqualTo(fri)
            assertThat(it.resting[1L]).isEqualTo(RestLabel.OnDay(DayOfWeek.FRIDAY))
        }
    }

    @Test
    fun restOnce_aroundMidnight() = runTest {
        // 수요일 23:59는 다음 울림(목)을 쉬어서 금요일 06:30에 울려요.
        rested(a630, ZonedDateTime.of(2026, 11, 18, 23, 59, 0, 0, seoul)).let {
            assertThat(it.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 19))
            assertThat(it.next!!.at).isEqualTo(ZonedDateTime.of(2026, 11, 20, 6, 30, 0, 0, seoul))
            assertThat(it.resting[1L]).isEqualTo(RestLabel.OnDay(DayOfWeek.FRIDAY))
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
        // 공휴일에도 울리는 알람은 수요일 울림이 이미 지나서 목요일을 쉬고 금요일에 울려요.
        rested(a630, at, wedHoliday).let {
            assertThat(it.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 19))
            assertThat(it.next!!.at).isEqualTo(ZonedDateTime.of(2026, 11, 20, 6, 30, 0, 0, seoul))
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
        // '모든 알람 끄기'에서 생긴 '이번만 쉬기'도 수요일 오후면 목요일을 쉬고 금요일에 울려요.
        clock = ZonedDateTime.of(2026, 11, 18, 15, 0, 0, 0, seoul)
        val vm = viewModel(FakeAlarmBook(listOf(a630)))
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.turnAllOff()
            var state = awaitItem()
            while (state.alarms.first().enabled) state = awaitItem()
            vm.restOnce(state.alarms.first())
            while (1L !in state.resting) state = awaitItem()
            assertThat(state.next!!.at).isEqualTo(ZonedDateTime.of(2026, 11, 20, 6, 30, 0, 0, seoul))
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
    fun restLabel_sevenDaysOrMore_showsDate() {
        // 결정 162: 수 11-25에 목 알람을 쉬면 다음 울림은 12-3(8일 뒤)이라 날짜, 딱 7일째(같은 요일)도 날짜예요.
        val now = ZonedDateTime.of(2026, 11, 25, 15, 33, 0, 0, seoul)
        val eightDays = ZonedDateTime.of(2026, 12, 3, 16, 17, 0, 0, seoul)
        val sevenDays = ZonedDateTime.of(2026, 12, 2, 16, 17, 0, 0, seoul)
        val sixDays = ZonedDateTime.of(2026, 12, 1, 16, 17, 0, 0, seoul)
        assertThat(RestLabel.between(now, eightDays)).isEqualTo(RestLabel.OnDate(LocalDate.of(2026, 12, 3)))
        assertThat(RestLabel.between(now, sevenDays)).isEqualTo(RestLabel.OnDate(LocalDate.of(2026, 12, 2)))
        assertThat(RestLabel.between(now, sixDays)).isEqualTo(RestLabel.OnDay(DayOfWeek.TUESDAY))
        // 시각이 아니라 날로 세요. 7일째 이른 아침(6일 15시간 뒤)도 날짜예요.
        val earlySeventh = ZonedDateTime.of(2026, 12, 2, 6, 30, 0, 0, seoul)
        assertThat(RestLabel.between(now, earlySeventh)).isEqualTo(RestLabel.OnDate(LocalDate.of(2026, 12, 2)))
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
        vm.status.test {
            assertThat(awaitItem()).isNull()
            checks.recheckPending.value = true
            assertThat(awaitItem()).isEqualTo(HomeStatus.Recheck())
        }
        vm.openRecheck()
        assertThat(checks.calls).containsExactly("open")
    }

    @Test
    fun restingBand_showsRingAgainTime_andOpensRingScreen() = runTest {
        val vm = viewModel(FakeAlarmBook(listOf(a630)))
        vm.status.test {
            assertThat(awaitItem()).isNull()
            // 결정 165: 쉬는 중이면 다시 울릴 시각을 띠에 보여요(23:15).
            checks.restingUntil.value = clock.plusMinutes(5).toInstant().toEpochMilli()
            assertThat(awaitItem()).isEqualTo(HomeStatus.Resting(LocalTime.of(23, 15)))
            checks.restingUntil.value = null
            assertThat(awaitItem()).isNull()
            // 결정 168: W2를 기다리는 동안은 글만 있는 띠예요. 둘째 줄에 W2가 뜰 시각을 보여요(TSK-201, 23:13).
            checks.wakeCheckAt.value = clock.plusMinutes(3).toInstant().toEpochMilli()
            checks.wakeCheckWaiting.value = true
            assertThat(awaitItem()).isEqualTo(HomeStatus.WakeCheckSoon(LocalTime.of(23, 13)))
        }
        vm.openRest()
        assertThat(checks.calls).containsExactly("open_rest")
    }

    @Test
    fun ringingAlarm_toggleIsBlocked_withToast() = runTest {
        checks.ringingFor = setOf(1L)
        val book = FakeAlarmBook(listOf(a630))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.setEnabled(a630, false)
            val state = awaitItem()
            // 결정 165: 끄지 않고 '울림 화면에서 꺼 주세요' 토스트만 떠요. '이번만 쉬기'도 달지 않아요.
            assertThat(state.message).isEqualTo(HomeMessage.Ringing)
            assertThat(state.restOffers).isEmpty()
            assertThat(book.get(1)!!.enabled).isTrue()
            assertThat(checks.calls).isEmpty()
        }
    }

    @Test
    fun ringingAlarm_isLeftOut_ofAllOffAndDelete() = runTest {
        checks.ringingFor = setOf(1L)
        val book = FakeAlarmBook(listOf(a630, a710))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.turnAllOff()
            var state = awaitItem()
            while (state.message == null) state = awaitItem()
            assertThat(state.message).isEqualTo(HomeMessage.Ringing)
            assertThat(book.get(1)!!.enabled).isTrue()
            assertThat(book.get(2)!!.enabled).isFalse()
            vm.dismissMessage(HomeMessage.Ringing)
            while (state.message != null) state = awaitItem()
            vm.startSelection(a630)
            awaitItem()
            vm.deleteSelected()
            while (state.message != HomeMessage.Ringing) state = awaitItem()
            assertThat(book.get(1)).isNotNull()
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** ISS-050: 꺼진 채 울리는 알람(한 번 알람 등)은 선택 모드 '켜기'도 먹지 않아요(홈 토글과 같음). */
    @Test
    fun ringingAlarm_isLeftOut_ofSelectionTurnOn() = runTest {
        checks.ringingFor = setOf(1L)
        val book = FakeAlarmBook(listOf(a630.copy(enabled = false), a710.copy(enabled = false)))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.startSelection(a630)
            awaitItem()
            vm.toggleSelectAll()
            awaitItem()
            vm.turnOnSelected()
            var state = awaitItem()
            while (state.message == null) state = awaitItem()
            assertThat(state.message).isEqualTo(HomeMessage.Ringing)
            while (state.alarms.none { it.enabled }) state = awaitItem()
            assertThat(book.get(1)!!.enabled).isFalse()
            assertThat(book.get(2)!!.enabled).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun restOnce_ringDayNight_skipsNextRing_andShowsDay() = runTest {
        val book = FakeAlarmBook(listOf(a630))
        val vm = viewModel(book)
        vm.uiState.test {
            awaitItem().let { if (it.loading) awaitItem() else it }
            vm.setEnabled(a630, false)
            var state = awaitItem()
            while (state.alarms.first().enabled) state = awaitItem()
            vm.restOnce(state.alarms.first())
            while (1L !in state.resting) state = awaitItem()
            // 월요일 밤(이미 울림)에 쉬면 다음 울림(화)을 쉬고 수요일에 울려요(결정 175).
            assertThat(state.alarms.first().skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 17))
            assertThat(state.resting[1L]).isEqualTo(RestLabel.OnDay(DayOfWeek.WEDNESDAY))
            assertThat(state.shownOn(state.alarms.first())).isFalse()
            assertThat(state.restOffers).isEmpty()
            assertThat(state.next!!.at.toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 18))

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
internal object NoWeather : WeatherSource {
    override val weather: StateFlow<WeatherKind?> = MutableStateFlow(null)

    override fun refresh(force: Boolean) = Unit
}
