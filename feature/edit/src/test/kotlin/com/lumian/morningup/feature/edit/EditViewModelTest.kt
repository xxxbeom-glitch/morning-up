package com.lumian.morningup.feature.edit

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.MinuteTicker
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ShakeSupport
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.RingAgain
import com.lumian.morningup.core.model.VibrationPattern
import com.lumian.morningup.core.model.WakeCheck
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

    private fun viewModel(
        book: FakeAlarmBook,
        id: Long = EditRoute.NEW_ALARM,
        shake: Boolean = true,
        checks: FakeWakeCheckGate = FakeWakeCheckGate(),
    ) = EditViewModel(
        book,
        now,
        noHolidays,
        MinuteTicker { ticks },
        SavedStateHandle(mapOf("alarmId" to id)),
        ShakeSupport { shake },
        checks,
    )

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
            // 다시 울림은 꺼진 채 시작해요(결정 187). 켜면 5분 3번이에요.
            assertThat(state.draft.ringAgain).isEqualTo(RingAgain(intervalMinutes = 5, count = 0))
            // 다시 잠들기 방지는 꺼진 채 시작해요. 간격과 시간은 설정 탭 전체 설정이에요(결정 145).
            assertThat(state.draft.wakeCheck).isEqualTo(WakeCheck(enabled = false))
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
            // 켜면 미션 고르기(S1-2)가 열려요. 골라야 미션이 생겨요.
            vm.setMissionsEnabled(true)
            val picking = awaitItem()
            assertThat(picking.detail).isEqualTo(EditDetail.Mission)
            assertThat(picking.hasMissions).isFalse()
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
            // 새 알람은 다시 울림 꺼짐으로 시작해요(결정 187).
            vm.setRingAgain(true)
            assertThat(awaitItem().draft.ringAgain.count).isEqualTo(3)
            vm.setRingAgain(false)
            assertThat(awaitItem().draft.ringAgain.count).isEqualTo(0)
            vm.setWakeCheck(true)
            assertThat(awaitItem().draft.wakeCheck.enabled).isTrue()
            vm.setSoundId("aube_paisible")
            assertThat(awaitItem().draft.soundId).isEqualTo("aube_paisible")
            vm.setSoundId("not_in_list")
            vm.setSoundId(Alarm.DEFAULT_SOUND_ID)
            assertThat(awaitItem().draft.soundId).isEqualTo(Alarm.DEFAULT_SOUND_ID)
            vm.setBackground(false)
            assertThat(awaitItem().draft.background).isEqualTo(Alarm.BACKGROUND_PLAIN)
            vm.setGentleWake(true)
            assertThat(awaitItem().draft.gentleWake).isTrue()
            vm.setSkipHolidays(true)
            assertThat(awaitItem().draft.skipHolidays).isTrue()
        }
    }

    @Test
    fun snooze_offForNewAlarm_minutesAndCountSteps_minOneNoMax() = runTest {
        // 결정 195: 새 알람은 미루기 꺼짐. 간격 1, 3, 5, 10분, 횟수 +, - 최소 1, 최대 없음. 고르면 미루기도 켜요.
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            assertThat(loaded().draft.snooze.enabled).isFalse()
            vm.setSnoozeMinutes(10)
            var snooze = awaitItem().draft.snooze
            assertThat(snooze.enabled).isTrue()
            assertThat(snooze.minutes).isEqualTo(10)
            vm.setSnoozeMinutes(7)
            repeat(4) { vm.stepSnoozeCount(1) }
            snooze = expectMostRecentItem().draft.snooze
            assertThat(snooze.minutes).isEqualTo(10)
            assertThat(snooze.count).isEqualTo(7)
            repeat(9) { vm.stepSnoozeCount(-1) }
            assertThat(expectMostRecentItem().draft.snooze.count).isEqualTo(1)
            vm.setSnooze(false)
            assertThat(awaitItem().draft.snooze.enabled).isFalse()
        }
    }

    @Test
    fun wakeCheck_intervalOneMinute_andCountSteps_minOneNoMax() = runTest {
        // 결정 186: 간격 1분을 고를 수 있고, 횟수는 +, - 로 최소 1, 최대 없음이에요. 고르면 다시 잠들기 방지도 켜요.
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            loaded()
            vm.setWakeCheckAsk(1)
            var check = awaitItem().draft.wakeCheck
            assertThat(check.enabled).isTrue()
            assertThat(check.askAfterMinutes).isEqualTo(1)
            vm.stepRecheckCount(-1)
            repeat(5) { vm.stepRecheckCount(1) }
            check = expectMostRecentItem().draft.wakeCheck
            assertThat(check.recheckCount).isEqualTo(6)
            repeat(9) { vm.stepRecheckCount(-1) }
            assertThat(expectMostRecentItem().draft.wakeCheck.recheckCount).isEqualTo(1)
        }
    }

    @Test
    fun save_existingAlarm_stopsLeftChecks_andTellsHome() = runTest {
        val book = FakeAlarmBook(listOf(withMission))
        val checks = FakeWakeCheckGate(pendingFor = setOf(4))
        val vm = viewModel(book, id = 4, checks = checks)
        vm.uiState.test {
            loaded()
            vm.save()
            var state = awaitItem()
            while (state.exit == null) state = awaitItem()
            assertThat(state.exit).isEqualTo(EditExit.Saved(4, checksStopped = true))
            assertThat(checks.calls).containsExactly("cancel:4")
        }
    }

    @Test
    fun save_ringingAlarm_isBlocked_withToast_andStaysOpen() = runTest {
        val book = FakeAlarmBook(listOf(withMission))
        val checks = FakeWakeCheckGate(ringingFor = setOf(4))
        val vm = viewModel(book, id = 4, checks = checks)
        vm.uiState.test {
            loaded()
            vm.setTime(8, 0)
            awaitItem()
            vm.save()
            val state = awaitItem()
            // 결정 165: 울리는 중, 쉬는 중, 미션 중이면 저장하지 않고 토스트만 띄워요.
            assertThat(state.ringingBlocked).isTrue()
            assertThat(state.exit).isNull()
            assertThat(book.get(4)!!.time).isEqualTo(LocalTime.of(6, 30))
            assertThat(checks.calls).isEmpty()
            vm.ringingNoticeShown()
            assertThat(awaitItem().ringingBlocked).isFalse()
        }
    }

    @Test
    fun existingAlarm_keepsItsRingAgain() = runTest {
        val old = withMission.copy(ringAgain = RingAgain(intervalMinutes = 5, count = 0))
        viewModel(FakeAlarmBook(listOf(old)), id = 4).uiState.test {
            // 결정 163: 처음 값이 바뀌어도 옛 알람은 저장된 값 그대로예요.
            assertThat(loaded().draft.ringAgain.count).isEqualTo(0)
        }
    }

    /** 화, 수 06:30 반복이고 화요일(11-17)을 쉬는 중인 알람(결정 159). */
    private val resting = Alarm(
        id = 7,
        time = LocalTime.of(6, 30),
        repeatDays = setOf(DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY),
        skipOnceDate = LocalDate.of(2026, 11, 17),
    )

    private suspend fun EditViewModel.savedExit(
        turbine: app.cash.turbine.ReceiveTurbine<EditUiState>,
    ): EditExit? {
        save()
        var state = turbine.awaitItem()
        while (state.exit == null) state = turbine.awaitItem()
        return state.exit
    }

    @Test
    fun resting_untilCountsRest_untilTimingChanges_andRevertCountsAgain() = runTest {
        val vm = viewModel(FakeAlarmBook(listOf(resting)), id = 7)
        vm.uiState.test {
            // 쉬기를 넣고 세면 수요일 06:30: 1일 7시간 20분.
            assertThat(loaded().until).isEqualTo(TimeUntil(1, 7, 20))
            vm.setTime(7, 0)
            // 시각을 바꾸면 쉬기를 뺀 새 울림(화 07:00).
            assertThat(awaitItem().until).isEqualTo(TimeUntil(0, 7, 50))
            vm.setTime(6, 30)
            // 처음 값으로 되돌리면 다시 쉬기를 넣고 세요.
            assertThat(awaitItem().until).isEqualTo(TimeUntil(1, 7, 20))
        }
    }

    @Test
    fun resting_saveWithTimeChange_releasesRest_andTellsHome() = runTest {
        val book = FakeAlarmBook(listOf(resting))
        val vm = viewModel(book, id = 7)
        vm.uiState.test {
            loaded()
            vm.setTime(7, 0)
            awaitItem()
            assertThat(vm.savedExit(this)).isEqualTo(EditExit.Saved(7, restReleased = true))
            assertThat(book.get(7)!!.skipOnceDate).isNull()
        }
    }

    @Test
    fun resting_saveWithDayOrHolidayChange_releasesRest() = runTest {
        val changes = listOf<(EditViewModel) -> Unit>({ it.toggleDay(DayOfWeek.FRIDAY) }, { it.setSkipHolidays(true) })
        for (change in changes) {
            val book = FakeAlarmBook(listOf(resting))
            val vm = viewModel(book, id = 7)
            vm.uiState.test {
                loaded()
                change(vm)
                awaitItem()
                assertThat(vm.savedExit(this)).isEqualTo(EditExit.Saved(7, restReleased = true))
                assertThat(book.get(7)!!.skipOnceDate).isNull()
            }
        }
    }

    @Test
    fun resting_saveWithOtherChange_keepsRest() = runTest {
        val book = FakeAlarmBook(listOf(resting))
        val vm = viewModel(book, id = 7)
        vm.uiState.test {
            loaded()
            vm.setVibrate(false)
            awaitItem()
            vm.setTime(7, 0)
            awaitItem()
            vm.setTime(6, 30)
            awaitItem()
            assertThat(vm.savedExit(this)).isEqualTo(EditExit.Saved(7))
            assertThat(book.get(7)!!.skipOnceDate).isEqualTo(LocalDate.of(2026, 11, 17))
        }
    }

    @Test
    fun staleSkip_timeChange_clearsSilently() = runTest {
        // 지난주에 쉬고 끝난 날짜가 남아 있으면 지금 쉬는 중이 아니라 '풀었어요'를 띄우지 않아요.
        val book = FakeAlarmBook(listOf(resting.copy(skipOnceDate = LocalDate.of(2026, 11, 10))))
        val vm = viewModel(book, id = 7)
        vm.uiState.test {
            loaded()
            vm.setTime(7, 0)
            awaitItem()
            assertThat(vm.savedExit(this)).isEqualTo(EditExit.Saved(7))
            assertThat(book.get(7)!!.skipOnceDate).isNull()
        }
    }

    @Test
    fun saveNew_doesNotTouchChecks() = runTest {
        val checks = FakeWakeCheckGate()
        val vm = viewModel(FakeAlarmBook(listOf(withMission)), checks = checks)
        vm.uiState.test {
            loaded()
            vm.save()
            var state = awaitItem()
            while (state.exit == null) state = awaitItem()
            assertThat(state.exit).isEqualTo(EditExit.Saved(5))
            assertThat(checks.calls).isEmpty()
        }
    }

    @Test
    fun otherDetails_doNotOpen() = runTest {
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            loaded()
            vm.openDetail(EditDetail.Preview)
            expectNoEvents()
        }
    }

    @Test
    fun soundDetail_opensS5() = runTest {
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            loaded()
            vm.openDetail(EditDetail.Sound)
            assertThat(awaitItem().detail).isEqualTo(EditDetail.Sound)
        }
    }

    @Test
    fun pickMissions_inOrder_fillsFourSlots_andRemoveTakesOneOut() = runTest {
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            assertThat(loaded().missionOrder).containsExactlyElementsIn(EditUiState.MISSION_LIST_ORDER).inOrder()
            var full = EditUiState()
            val picks =
                listOf(MissionType.QuickMath, MissionType.ShakeShake, MissionType.PopQuiz, MissionType.DropWords)
            picks.forEach { type ->
                vm.openMissionSheet(type)
                val sheet = awaitItem().missionSheet!!
                assertThat(sheet.selected).isFalse()
                assertThat(sheet.setting).isEqualTo(MissionSetting.default(type))
                vm.confirmMissionSheet()
                full = awaitItem()
                assertThat(full.missionSheet).isNull()
            }
            // 칸 4개(결정 87)를 고른 순서대로 채워요.
            assertThat(full.draft.missionTypes).containsExactlyElementsIn(picks).inOrder()
            assertThat(full.missionsFull).isTrue()
            assertThat(full.canOpen(MissionType.QuickMath)).isTrue()
            // 칸 오른쪽 위 X: 확인 없이 바로 빠지고 나머지 순서는 그대로예요.
            vm.removeMission(MissionType.ShakeShake)
            val after = awaitItem()
            assertThat(after.draft.missionTypes)
                .containsExactly(MissionType.QuickMath, MissionType.PopQuiz, MissionType.DropWords).inOrder()
            assertThat(after.missionsFull).isFalse()
        }
    }

    @Test
    fun shakeWithoutSensor_cannotBePicked() = runTest {
        val vm = viewModel(FakeAlarmBook(), shake = false)
        vm.uiState.test {
            val state = loaded()
            assertThat(state.shakeAvailable).isFalse()
            assertThat(state.canOpen(MissionType.ShakeShake)).isFalse()
            vm.openMissionSheet(MissionType.ShakeShake)
            expectNoEvents()
        }
    }

    @Test
    fun sheetValues_stayInRange_andEndsDisableButtons() = runTest {
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            loaded()
            vm.openMissionSheet(MissionType.DropWords)
            var sheet = awaitItem().missionSheet!!
            // 처음 값은 모두 1이라 < 가 막혀 있어요(결정 187).
            assertThat(sheet.canLevelDown).isFalse()
            assertThat(sheet.canSpeedDown).isFalse()
            assertThat(sheet.canCountDown).isFalse()
            assertThat(sheet.canLevelUp).isTrue()
            vm.stepMissionLevel(-1)
            vm.stepMissionCount(1)
            repeat(5) { vm.stepMissionCount(-1) }
            sheet = expectMostRecentItem().missionSheet!!
            assertThat(sheet.setting.level).isEqualTo(1)
            assertThat(sheet.setting.count).isEqualTo(1)
            assertThat(sheet.canCountDown).isFalse()
            repeat(10) { vm.stepMissionLevel(1) }
            repeat(10) { vm.stepMissionSpeed(1) }
            repeat(30) { vm.stepMissionCount(1) }
            val top = expectMostRecentItem()
            sheet = top.missionSheet!!
            assertThat(sheet.setting.level).isEqualTo(7)
            assertThat(sheet.setting.speed).isEqualTo(4)
            assertThat(sheet.setting.count).isEqualTo(30)
            assertThat(sheet.canLevelUp).isFalse()
            assertThat(sheet.canSpeedUp).isFalse()
            assertThat(sheet.canCountUp).isFalse()
            assertThat(sheet.canLevelDown).isTrue()
            // 안 고른 미션이라 아직 목록에는 없어요.
            assertThat(top.hasMissions).isFalse()
        }
    }

    @Test
    fun closingSheet_withoutConfirm_keepsNoMission_andDropsChangedValues() = runTest {
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            loaded()
            vm.openMissionSheet(MissionType.PopQuiz)
            vm.stepMissionCount(1)
            vm.closeMissionSheet()
            val closed = expectMostRecentItem()
            assertThat(closed.missionSheet).isNull()
            assertThat(closed.hasMissions).isFalse()
            vm.openMissionSheet(MissionType.PopQuiz)
            // 11-18: 고르지 않고 닫으면 바꾼 값은 버리고 처음 값으로 다시 열려요.
            assertThat(awaitItem().missionSheet!!.setting).isEqualTo(MissionSetting.default(MissionType.PopQuiz))
        }
    }

    @Test
    fun selectedMission_changesSaveRightAway_andDeselectRemoves() = runTest {
        val vm = viewModel(FakeAlarmBook(listOf(withMission)), id = 4)
        vm.uiState.test {
            loaded()
            vm.openMissionSheet(MissionType.DropWords)
            assertThat(awaitItem().missionSheet!!.selected).isTrue()
            vm.stepMissionLevel(1)
            assertThat(awaitItem().draft.missions.single().level).isEqualTo(2)
            vm.deselectMission()
            val after = awaitItem()
            assertThat(after.missionSheet).isNull()
            assertThat(after.hasMissions).isFalse()
        }
    }

    @Test
    fun noMissionAlarm_savesWithEmptyMissions() = runTest {
        val book = FakeAlarmBook(listOf(withMission))
        val vm = viewModel(book, id = 4)
        vm.uiState.test {
            loaded()
            vm.setMissionsEnabled(false)
            awaitItem()
            vm.save()
            while (awaitItem().exit == null) Unit
        }
        assertThat(book.alarms.value.single().missions).isEmpty()
    }

    @Test
    fun backClosesSheetFirst_thenPage() = runTest {
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            loaded()
            vm.setMissionsEnabled(true)
            awaitItem()
            vm.openMissionSheet(MissionType.QuickMath)
            awaitItem()
            vm.onBack()
            val sheetClosed = awaitItem()
            assertThat(sheetClosed.missionSheet).isNull()
            assertThat(sheetClosed.detail).isEqualTo(EditDetail.Mission)
            vm.onBack()
            assertThat(awaitItem().detail).isNull()
        }
    }

    @Test
    fun detailPages_ringAgainVolumeGentleBackground() = runTest {
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            loaded()
            vm.openDetail(EditDetail.RingAgain)
            assertThat(awaitItem().detail).isEqualTo(EditDetail.RingAgain)
            vm.setRingAgainInterval(10)
            assertThat(awaitItem().draft.ringAgain.intervalMinutes).isEqualTo(10)
            vm.setRingAgainInterval(7)
            vm.setRingAgainCount(null)
            assertThat(awaitItem().draft.ringAgain).isEqualTo(RingAgain(10, null))
            vm.setVolume(35)
            assertThat(awaitItem().draft.volumePercent).isEqualTo(35)
            vm.setVolume(140)
            assertThat(awaitItem().draft.volumePercent).isEqualTo(100)
            vm.setVibrate(false)
            awaitItem()
            vm.setVibrationPattern(VibrationPattern.LongOnce)
            val patterned = awaitItem().draft
            assertThat(patterned.vibrationPattern).isEqualTo(VibrationPattern.LongOnce)
            assertThat(patterned.vibrate).isTrue()
            vm.setGentleSeconds(45)
            vm.setGentleSeconds(15)
            assertThat(awaitItem().draft.gentleSeconds).isEqualTo(15)
            vm.setBackgroundId("nope")
            vm.setBackgroundId("forest")
            assertThat(awaitItem().draft.background).isEqualTo("forest")
            vm.setBackground(false)
            assertThat(awaitItem().draft.background).isEqualTo(Alarm.BACKGROUND_PLAIN)
            vm.setBackground(true)
            assertThat(awaitItem().draft.background).isEqualTo("forest")
            vm.setBackgroundId("photo:bg_1.jpg")
            assertThat(awaitItem().draft.background).isEqualTo("photo:bg_1.jpg")
        }
    }

    /** ISS-036: 무제한(null)을 고른 뒤 간격을 바꿔도 무제한이 남아요. 꺼짐(0)일 때만 3번으로 켜져요. */
    @Test
    fun ringAgainInterval_keepsUnlimitedCount_andOffStartsAtThree() = runTest {
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            loaded()
            vm.setRingAgainCount(null)
            assertThat(awaitItem().draft.ringAgain.count).isNull()
            vm.setRingAgainInterval(10)
            assertThat(awaitItem().draft.ringAgain).isEqualTo(RingAgain(10, null))
            vm.setRingAgain(true)
            vm.setRingAgainCount(5)
            assertThat(awaitItem().draft.ringAgain.count).isEqualTo(5)
            vm.setRingAgainInterval(3)
            assertThat(awaitItem().draft.ringAgain).isEqualTo(RingAgain(3, 5))
            vm.setRingAgain(false)
            assertThat(awaitItem().draft.ringAgain.count).isEqualTo(0)
            vm.setRingAgainInterval(5)
            assertThat(awaitItem().draft.ringAgain).isEqualTo(RingAgain(5, 3))
        }
    }

    @Test
    fun newAlarm_soundStartsRandom_andRandomCanBePicked() = runTest {
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            assertThat(loaded().draft.soundId).isEqualTo(Alarm.RANDOM_SOUND_ID)
            vm.setSoundId("aube_paisible")
            awaitItem()
            vm.setSoundId(Alarm.RANDOM_SOUND_ID)
            assertThat(awaitItem().draft.soundId).isEqualTo(Alarm.RANDOM_SOUND_ID)
            // '사용 안 함'은 목록에 없어서 고를 수 없어요.
            vm.setSoundId(Alarm.SILENT_SOUND_ID)
            expectNoEvents()
        }
    }
}
