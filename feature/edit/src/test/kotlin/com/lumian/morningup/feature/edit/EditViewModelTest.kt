package com.lumian.morningup.feature.edit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.MinuteTicker
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.RingAgain
import com.lumian.morningup.core.model.WakeCheck
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

class EditViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    /** 2026-11-16 월요일 23:10. */
    private var clock = ZonedDateTime.of(2026, 11, 16, 23, 10, 0, 0, ZoneId.of("Asia/Seoul"))
    private val now = NowProvider { clock }
    private val ticks = MutableSharedFlow<Unit>()
    private val noHolidays = object : HolidayCalendar {
        override fun isHoliday(date: LocalDate) = false

        override fun covers(year: Int) = true
    }
    private val withMission = Alarm(
        id = 4,
        time = LocalTime.of(6, 30),
        repeatDays = setOf(DayOfWeek.MONDAY),
        missions = listOf(MissionSetting(MissionType.DropWords)),
        powerOffGuard = true,
        enabled = false,
    )

    private fun viewModel(book: FakeAlarmBook, id: Long = EditRoute.NEW_ALARM) =
        EditViewModel(book, now, noHolidays, MinuteTicker { ticks }, SavedStateHandle(mapOf("alarmId" to id)))

    private suspend fun app.cash.turbine.ReceiveTurbine<EditUiState>.loaded(): EditUiState {
        var state = awaitItem()
        while (state.loading) state = awaitItem()
        return state
    }

    @Test
    fun newAlarm_startsWithoutMission_andPlanDefaults() = runTest {
        viewModel(FakeAlarmBook()).uiState.test {
            val state = loaded()
            assertThat(state.isNew).isTrue()
            assertThat(state.changed).isFalse()
            assertThat(state.hasMissions).isFalse()
            assertThat(state.draft.time).isEqualTo(LocalTime.of(7, 0))
            assertThat(state.draft.ringAgain).isEqualTo(RingAgain(intervalMinutes = 5, count = 3))
            assertThat(state.draft.wakeCheck)
                .isEqualTo(WakeCheck(enabled = false, askAfterMinutes = 1, answerSeconds = 60))
            // 23:10에 내일 07:00 → 7시간 50분 후.
            assertThat(state.until).isEqualTo(TimeUntil(0, 7, 50))
        }
    }

    @Test
    fun existingAlarm_loads_andTurningMissionsOff_meansNoMission() = runTest {
        val vm = viewModel(FakeAlarmBook(listOf(withMission)), id = 4)
        vm.uiState.test {
            val state = loaded()
            assertThat(state.isNew).isFalse()
            assertThat(state.hasMissions).isTrue()
            vm.setMissionsEnabled(false)
            val off = awaitItem()
            assertThat(off.hasMissions).isFalse()
            assertThat(off.changed).isTrue()
            // 켜기는 미션을 고르는 화면이 필요해서 지금은 그대로예요.
            vm.setMissionsEnabled(true)
            expectNoEvents()
        }
    }

    @Test
    fun timeChange_updatesUntil_andMarksChanged() = runTest {
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            loaded()
            vm.setTime(6, 30)
            val state = awaitItem()
            assertThat(state.changed).isTrue()
            assertThat(state.until).isEqualTo(TimeUntil(0, 7, 20))
            clock = clock.plusMinutes(1)
            ticks.emit(Unit)
            assertThat(awaitItem().until).isEqualTo(TimeUntil(0, 7, 19))
        }
    }

    @Test
    fun backWithoutChanges_closes() = runTest {
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            loaded()
            vm.onBack()
            val state = awaitItem()
            assertThat(state.confirmExit).isFalse()
            assertThat(state.exit).isEqualTo(EditExit.Discarded)
        }
    }

    @Test
    fun backWithChanges_asksFirst_thenKeepOrDiscard() = runTest {
        val book = FakeAlarmBook()
        val vm = viewModel(book)
        vm.uiState.test {
            loaded()
            vm.toggleDay(DayOfWeek.MONDAY)
            awaitItem()
            vm.onBack()
            assertThat(awaitItem().confirmExit).isTrue()
            vm.keepEditing()
            assertThat(awaitItem().confirmExit).isFalse()
            vm.onBack()
            awaitItem()
            vm.discard()
            assertThat(awaitItem().exit).isEqualTo(EditExit.Discarded)
            assertThat(book.calls).isEmpty()
        }
    }

    @Test
    fun undoingChange_isNotChanged() = runTest {
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            loaded()
            vm.setVibrate(false)
            assertThat(awaitItem().changed).isTrue()
            vm.setVibrate(true)
            assertThat(awaitItem().changed).isFalse()
        }
    }

    @Test
    fun save_storesEnabledAlarm_once_andExitsWithId() = runTest {
        val book = FakeAlarmBook(listOf(withMission))
        val vm = viewModel(book, id = 4)
        vm.uiState.test {
            loaded()
            vm.setTime(5, 0)
            awaitItem()
            vm.save()
            vm.save()
            var state = awaitItem()
            while (state.exit == null) state = awaitItem()
            assertThat(state.exit).isEqualTo(EditExit.Saved(4))
            assertThat(book.calls).containsExactly("save:4")
            val stored = book.get(4)!!
            assertThat(stored.enabled).isTrue()
            assertThat(stored.time).isEqualTo(LocalTime.of(5, 0))
        }
    }

    @Test
    fun saveNew_getsNewId() = runTest {
        val book = FakeAlarmBook(listOf(withMission))
        val vm = viewModel(book)
        vm.uiState.test {
            loaded()
            vm.save()
            var state = awaitItem()
            while (state.exit == null) state = awaitItem()
            assertThat(state.exit).isEqualTo(EditExit.Saved(5))
            assertThat(book.get(5)!!.missions).isEmpty()
        }
    }

    @Test
    fun toggles_mapToAlarmFields() = runTest {
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            loaded()
            vm.setRingAgain(false)
            assertThat(awaitItem().draft.ringAgain.count).isEqualTo(0)
            vm.setRingAgain(true)
            assertThat(awaitItem().draft.ringAgain.count).isEqualTo(3)
            vm.setWakeCheck(true)
            assertThat(awaitItem().draft.wakeCheck.enabled).isTrue()
            vm.setSound(false)
            assertThat(awaitItem().draft.soundId).isEqualTo(Alarm.SILENT_SOUND_ID)
            vm.setBackground(false)
            assertThat(awaitItem().draft.background).isEqualTo(Alarm.BACKGROUND_PLAIN)
            vm.setGentleWake(true)
            assertThat(awaitItem().draft.gentleWake).isTrue()
            vm.setSkipHolidays(true)
            assertThat(awaitItem().draft.skipHolidays).isTrue()
        }
    }

    @Test
    fun wakeCheckPage_opensClosesWithBack_andPicksValues_withoutTurningOn() = runTest {
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            loaded()
            vm.openDetail(EditDetail.WakeCheck)
            assertThat(awaitItem().detail).isEqualTo(EditDetail.WakeCheck)
            vm.setWakeCheckAsk(5)
            vm.setWakeCheckAnswer(120)
            var state = awaitItem()
            if (state.draft.wakeCheck.answerSeconds != 120) state = awaitItem()
            assertThat(
                state.draft.wakeCheck,
            ).isEqualTo(WakeCheck(enabled = false, askAfterMinutes = 5, answerSeconds = 120))
            // 고를 수 없는 값은 무시해요.
            vm.setWakeCheckAsk(7)
            vm.setWakeCheckAnswer(45)
            expectNoEvents()
            vm.onBack()
            val closed = awaitItem()
            assertThat(closed.detail).isNull()
            assertThat(closed.confirmExit).isFalse()
        }
    }

    @Test
    fun otherDetails_doNotOpen() = runTest {
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            loaded()
            vm.openDetail(EditDetail.Sound)
            expectNoEvents()
        }
    }

    @Test
    fun debugPath_turnsOnQuickMath() = runTest {
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            loaded()
            vm.setMissionsEnabled(true, allowDebugQuickMath = true)
            val state = awaitItem()
            assertThat(state.draft.missions).containsExactly(MissionSetting.default(MissionType.QuickMath))
            assertThat(state.hasMissions).isTrue()
        }
    }
}
