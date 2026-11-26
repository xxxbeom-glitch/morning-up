package com.lumian.morningup.feature.edit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.MinuteTicker
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ShakeSupport
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionChecker
import com.lumian.morningup.core.model.PermissionReport
import com.lumian.morningup.core.model.SaveWarning
import com.lumian.morningup.core.model.engine.HolidayCalendar
import com.lumian.morningup.core.testing.FakeAlarmBook
import com.lumian.morningup.core.testing.FakeWakeCheckGate
import com.lumian.morningup.core.testing.MainDispatcherRule
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** 저장 때 경고 창 S0-2, S0-3(TSK-177 답 1, 2). */
class EditSaveWarningTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val now = NowProvider { ZonedDateTime.of(2026, 11, 16, 23, 10, 0, 0, ZoneId.of("Asia/Seoul")) }
    private val noHolidays = object : HolidayCalendar {
        override fun isHoliday(date: LocalDate) = false

        override fun covers(year: Int) = true
    }

    private class FakeChecker(var next: PermissionReport) : PermissionChecker {
        private val state = MutableStateFlow(PermissionReport.Unknown)
        override val report: StateFlow<PermissionReport> = state
        var refreshes = 0

        override fun refresh() {
            refreshes++
            state.value = next
        }
    }

    private fun report(vararg off: AlarmPermission, silenced: Boolean = false) =
        PermissionReport(AlarmPermission.entries.associateWith { it !in off }, alarmsSilenced = silenced)

    private fun viewModel(book: FakeAlarmBook, checker: FakeChecker) = EditViewModel(
        book,
        now,
        noHolidays,
        MinuteTicker { MutableSharedFlow() },
        SavedStateHandle(mapOf("alarmId" to EditRoute.NEW_ALARM)),
        ShakeSupport { true },
        FakeWakeCheckGate(),
        checker,
    )

    private suspend fun app.cash.turbine.ReceiveTurbine<EditUiState>.loaded(): EditUiState {
        var state = awaitItem()
        while (state.loading) state = awaitItem()
        return state
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<EditUiState>.exited(): EditUiState {
        var state = awaitItem()
        while (state.exit == null) state = awaitItem()
        return state
    }

    @Test
    fun allFine_savesWithoutDialog() = runTest {
        val book = FakeAlarmBook()
        val checker = FakeChecker(report())
        val vm = viewModel(book, checker)
        vm.uiState.test {
            loaded()
            vm.save()
            assertThat(exited().exit).isEqualTo(EditExit.Saved(1))
            assertThat(checker.refreshes).isEqualTo(1)
        }
    }

    @Test
    fun cannotDismiss_showsDialog_thenSaveAnyway_savesWithoutFix() = runTest {
        val book = FakeAlarmBook()
        val vm = viewModel(book, FakeChecker(report(AlarmPermission.Notifications, AlarmPermission.Overlay)))
        vm.uiState.test {
            loaded()
            vm.save()
            val state = awaitItem()
            assertThat(state.saveWarning).isEqualTo(SaveWarning.CannotDismiss)
            assertThat(book.calls).isEmpty()
            vm.save()
            vm.saveAnyway(fix = false)
            assertThat(exited().exit).isEqualTo(EditExit.Saved(1))
            assertThat(book.calls).containsExactly("save:1")
        }
    }

    @Test
    fun silenced_fix_savesAndAsksAppToOpenGuide() = runTest {
        val book = FakeAlarmBook()
        val vm = viewModel(book, FakeChecker(report(silenced = true)))
        vm.uiState.test {
            loaded()
            vm.save()
            assertThat(awaitItem().saveWarning).isEqualTo(SaveWarning.AlarmsSilenced)
            vm.saveAnyway(fix = true)
            assertThat(exited().exit).isEqualTo(EditExit.Saved(1, fix = SaveWarning.AlarmsSilenced))
        }
    }

    @Test
    fun closeOrBack_keepsEditing_withoutSaving() = runTest {
        val book = FakeAlarmBook()
        val vm = viewModel(book, FakeChecker(report(silenced = true)))
        vm.uiState.test {
            loaded()
            vm.save()
            awaitItem()
            vm.closeSaveWarning()
            assertThat(awaitItem().saveWarning).isNull()
            vm.save()
            awaitItem()
            vm.onBack()
            val state = awaitItem()
            assertThat(state.saveWarning).isNull()
            assertThat(state.exit).isNull()
            assertThat(book.calls).isEmpty()
        }
    }
}
