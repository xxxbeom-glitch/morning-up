package com.lumian.morningup.feature.edit

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.ShakeSupport
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.testing.FakeAlarmBook
import com.lumian.morningup.core.testing.MainDispatcherRule
import java.time.DayOfWeek
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class FirstRunViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private fun viewModel(book: FakeAlarmBook = FakeAlarmBook(), shake: Boolean = true) =
        FirstRunViewModel(book, ShakeSupport { shake })

    @Test
    fun steps_goInPlanOrder() {
        assertThat(FirstRunStep.entries).containsExactly(
            FirstRunStep.Time,
            FirstRunStep.Background,
            FirstRunStep.Sound,
            FirstRunStep.Mission,
        ).inOrder()
        assertThat(FirstRunStep.Time.previous).isNull()
        assertThat(FirstRunStep.Mission.next).isNull()
    }

    @Test
    fun firstAlarm_isSevenDaily_withoutMission_timeSky() {
        val draft = viewModel().uiState.value.draft
        assertThat(draft.time).isEqualTo(LocalTime.of(7, 0))
        assertThat(draft.repeatDays).containsExactlyElementsIn(DayOfWeek.entries)
        assertThat(draft.missions).isEmpty()
        assertThat(draft.background).isNull()
        assertThat(draft.soundId).isEqualTo(EditViewModel.newAlarm().soundId)
    }

    @Test
    fun walkingAllSteps_savesOneEnabledAlarm_withChoices() = runTest {
        val book = FakeAlarmBook()
        val vm = viewModel(book)
        vm.setTime(6, 30)
        vm.next()
        assertThat(vm.uiState.value.step).isEqualTo(FirstRunStep.Background)
        vm.setBackground("sunrise")
        vm.next()
        vm.setSound("douceur")
        vm.next()
        vm.setMission(MissionType.PopQuiz)
        vm.next()

        val exit = vm.uiState.value.exit
        assertThat(exit).isInstanceOf(FirstRunExit.Saved::class.java)
        val saved = book.alarms.value.single()
        assertThat((exit as FirstRunExit.Saved).alarmId).isEqualTo(saved.id)
        assertThat(saved.enabled).isTrue()
        assertThat(saved.time).isEqualTo(LocalTime.of(6, 30))
        assertThat(saved.background).isEqualTo("sunrise")
        assertThat(saved.soundId).isEqualTo("douceur")
        // 결정 187: 미션 값은 처음 값 그대로예요.
        assertThat(saved.missions).containsExactly(MissionSetting.default(MissionType.PopQuiz))
    }

    @Test
    fun mission_isSingleChoice_andNullMeansNone() {
        val vm = viewModel()
        vm.setMission(MissionType.QuickMath)
        vm.setMission(MissionType.DropWords)
        assertThat(vm.uiState.value.draft.missionTypes).containsExactly(MissionType.DropWords)
        vm.setMission(null)
        assertThat(vm.uiState.value.mission).isNull()
    }

    @Test
    fun shakeWithoutSensor_cannotBePicked() {
        val vm = viewModel(shake = false)
        vm.setMission(MissionType.ShakeShake)
        assertThat(vm.uiState.value.mission).isNull()
    }

    @Test
    fun skip_endsWithoutSaving() {
        val book = FakeAlarmBook()
        val vm = viewModel(book)
        vm.next()
        vm.skip()
        assertThat(vm.uiState.value.exit).isEqualTo(FirstRunExit.Skipped)
        assertThat(book.alarms.value).isEmpty()
    }

    @Test
    fun back_goesToPreviousStep_andFirstStepLetsAppExit() {
        val vm = viewModel()
        assertThat(vm.back()).isFalse()
        vm.next()
        vm.next()
        assertThat(vm.back()).isTrue()
        assertThat(vm.uiState.value.step).isEqualTo(FirstRunStep.Background)
    }

    @Test
    fun demoRun_doesNotSave_andRestartGoesBackToTime() {
        val book = FakeAlarmBook()
        val vm = viewModel(book)
        repeat(FirstRunStep.entries.size) { vm.next(save = false) }
        assertThat(vm.uiState.value.exit).isEqualTo(FirstRunExit.Skipped)
        assertThat(book.alarms.value).isEmpty()
        vm.restart()
        assertThat(vm.uiState.value.step).isEqualTo(FirstRunStep.Time)
        assertThat(vm.uiState.value.exit).isNull()
    }
}
