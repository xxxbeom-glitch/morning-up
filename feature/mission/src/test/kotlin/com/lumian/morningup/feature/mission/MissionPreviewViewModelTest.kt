package com.lumian.morningup.feature.mission

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.engine.RingPhase
import com.lumian.morningup.core.model.engine.RingStateMachine
import com.lumian.morningup.core.testing.FakeRingLink
import com.lumian.morningup.core.testing.MainDispatcherRule
import com.lumian.morningup.core.testing.testRingSession
import com.lumian.morningup.feature.mission.play.MissionHostViewModel
import com.lumian.morningup.feature.mission.play.MissionPlay
import com.lumian.morningup.feature.mission.play.MissionPreviewRoute
import com.lumian.morningup.feature.mission.play.MissionPreviewViewModel
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** 미리보기: 시트 값 그대로, 엔진에 아무것도 보내지 않음, 59초 끝나면 닫힘, '잘했어요', 진짜 알람이 오면 닫힘. */
class MissionPreviewViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val clock = TestClock()
    private val quiz = MissionSetting(MissionType.PopQuiz, level = 1, count = 2)
    private val shake = MissionSetting(MissionType.ShakeShake, count = 10)

    private fun viewModel(link: FakeRingLink, vararg settings: MissionSetting) = MissionPreviewViewModel(
        SavedStateHandle(
            mapOf(
                MissionPreviewViewModel.SETTINGS_KEY to MissionPreviewRoute.of(settings.toList()).settingsJson,
            ),
        ),
        link,
        clock.provider,
        Random(SEED),
        testContentSource,
    )

    private fun MissionPreviewViewModel.solveQuiz() =
        onQuizAnswer((uiState.value.play as MissionPlay.Quiz).game.round.answerIndex)

    @Test
    fun opensWithSheetValues_andSendsNothingToEngine() = runTest {
        val link = FakeRingLink()
        val vm = viewModel(link, quiz)
        val play = vm.uiState.value.play as MissionPlay.Quiz
        assertThat(play.game.target).isEqualTo(2)
        assertThat(vm.uiState.value.preview).isTrue()
        assertThat(vm.uiState.value.timerEndsAt).isEqualTo(clock.millis + RingStateMachine.MISSION_MILLIS)
        vm.solveQuiz()
        vm.solveQuiz()
        assertThat(link.sent).isEmpty()
    }

    @Test
    fun timerRunsOut_closesInsteadOfRinging() = runTest {
        val vm = viewModel(FakeRingLink(), quiz)
        clock.millis += RingStateMachine.MISSION_MILLIS - 1
        vm.onFrame()
        assertThat(vm.uiState.value.closed).isFalse()
        clock.millis += 1
        vm.onFrame()
        assertThat(vm.uiState.value.closed).isTrue()
    }

    @Test
    fun allDone_showsWellDone_thenCloses() = runTest {
        val vm = viewModel(FakeRingLink(), quiz, shake)
        vm.solveQuiz()
        vm.solveQuiz()
        assertThat(vm.uiState.value.transitionTo).isEqualTo(2)
        clock.millis += MissionHostViewModel.TRANSITION_MILLIS
        vm.onFrame()
        assertThat(vm.uiState.value.play!!.type).isEqualTo(MissionType.ShakeShake)
        repeat(10) { vm.onShake() }
        assertThat(vm.uiState.value.completed).isTrue()
        assertThat(vm.uiState.value.closed).isFalse()
        clock.millis += MissionPreviewViewModel.COMPLETE_MILLIS
        vm.onFrame()
        assertThat(vm.uiState.value.closed).isTrue()
    }

    @Test
    fun realAlarm_closesPreview() = runTest {
        val link = FakeRingLink()
        val vm = viewModel(link, quiz)
        link.session.value = testRingSession(RingPhase.Ringing)
        assertThat(vm.uiState.value.closed).isTrue()
    }

    @Test
    fun closeButton_closes() = runTest {
        val vm = viewModel(FakeRingLink(), quiz)
        vm.close()
        assertThat(vm.uiState.value.closed).isTrue()
    }

    private companion object {
        const val SEED = 3
    }
}
