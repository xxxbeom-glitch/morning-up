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

    private fun viewModel(book: FakeAlarmBook, id: Long = EditRoute.NEW_ALARM, shake: Boolean = true) = EditViewModel(
        book,
        now,
        noHolidays,
        MinuteTicker { ticks },
        SavedStateHandle(mapOf("alarmId" to id)),
        ShakeSupport { shake },
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
            // 다시 울림은 꺼진 채 시작해요(켜면 5분 3번).
            assertThat(state.draft.ringAgain).isEqualTo(RingAgain(intervalMinutes = 5, count = 0))
            assertThat(state.draft.wakeCheck)
                .isEqualTo(WakeCheck(enabled = false, askAfterMinutes = 3, answerSeconds = 60))
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
    fun wakeCheckPage_opensClosesWithBack_andPickingValueTurnsOn() = runTest {
        val vm = viewModel(FakeAlarmBook())
        vm.uiState.test {
            loaded()
            vm.openDetail(EditDetail.WakeCheck)
            assertThat(awaitItem().detail).isEqualTo(EditDetail.WakeCheck)
            vm.setWakeCheckAsk(5)
            vm.setWakeCheckAnswer(180)
            var state = awaitItem()
            if (state.draft.wakeCheck.answerSeconds != 180) state = awaitItem()
            assertThat(
                state.draft.wakeCheck,
            ).isEqualTo(WakeCheck(enabled = true, askAfterMinutes = 5, answerSeconds = 180))
            // 고를 수 없는 값은 무시해요.
            vm.setWakeCheckAsk(7)
            vm.setWakeCheckAnswer(120)
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
            // 처음 값은 1단계, 속도 1이라 < 가 막혀 있어요. 문제 수는 10개이고 1씩 5개까지 줄일 수 있어요(11-19 사용자 결정).
            assertThat(sheet.canLevelDown).isFalse()
            assertThat(sheet.canSpeedDown).isFalse()
            assertThat(sheet.canCountDown).isTrue()
            assertThat(sheet.canLevelUp).isTrue()
            vm.stepMissionLevel(-1)
            repeat(5) { vm.stepMissionCount(-1) }
            sheet = expectMostRecentItem().missionSheet!!
            assertThat(sheet.setting.level).isEqualTo(1)
            assertThat(sheet.setting.count).isEqualTo(5)
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
