package com.lumian.morningup.feature.mission

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.engine.RingEvent
import com.lumian.morningup.core.model.engine.RingPhase
import com.lumian.morningup.core.model.engine.RingStateMachine
import com.lumian.morningup.core.model.mission.MathKey
import com.lumian.morningup.core.model.mission.SubmitResult
import com.lumian.morningup.core.testing.FakeRingLink
import com.lumian.morningup.core.testing.MainDispatcherRule
import com.lumian.morningup.core.testing.testRingSession
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class QuickMathViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private var clockMillis = 1_000_000L
    private val now =
        NowProvider { ZonedDateTime.ofInstant(Instant.ofEpochMilli(clockMillis), ZoneId.of("Asia/Seoul")) }

    private val math = MissionSetting(MissionType.QuickMath, level = 1, count = 2)
    private val inMission = testRingSession(RingPhase.InMission, listOf(math), phaseEndsAt = 1_059_000L)

    private fun viewModel(link: FakeRingLink) = QuickMathViewModel(link, now, Random(SEED))

    private fun QuickMathViewModel.answer(value: Int) {
        value.toString().forEach { onKey(MathKey.Digit(it.digitToInt())) }
        onKey(MathKey.Confirm)
    }

    private fun QuickMathViewModel.game() = uiState.value.game!!

    @Test
    fun opensGame_withSessionSetting() = runTest {
        val vm = viewModel(FakeRingLink(inMission))
        assertThat(vm.game().level).isEqualTo(1)
        assertThat(vm.game().target).isEqualTo(2)
        assertThat(vm.game().streak).isEqualTo(0)
        assertThat(vm.uiState.value.timerEndsAt).isEqualTo(1_059_000L)
    }

    @Test
    fun otherMissionType_fallsBackToQuickMathDefault() = runTest {
        val words = MissionSetting(MissionType.DropWords, level = 3, count = 20)
        val vm = viewModel(FakeRingLink(testRingSession(RingPhase.InMission, listOf(words))))
        assertThat(vm.game().level).isEqualTo(MissionSetting.default(MissionType.QuickMath).level)
        assertThat(vm.game().target).isEqualTo(MissionSetting.default(MissionType.QuickMath).count)
    }

    @Test
    fun correctAnswers_finishMission_andSendCompleted() = runTest {
        val link = FakeRingLink(inMission)
        val vm = viewModel(link)
        vm.answer(vm.game().problem.answer)
        assertThat(vm.game().streak).isEqualTo(1)
        assertThat(vm.uiState.value.feedback!!.result).isEqualTo(SubmitResult.Correct)
        vm.answer(vm.game().problem.answer)
        assertThat(vm.game().finished).isTrue()
        assertThat(vm.uiState.value.feedback!!.result).isEqualTo(SubmitResult.Finished)
        assertThat(link.sent).contains(RingEvent.MissionCompleted)
    }

    @Test
    fun wrongAnswer_resetsStreak_withNewProblem_andKeepsKeypad() = runTest {
        val link = FakeRingLink(inMission)
        val vm = viewModel(link)
        vm.answer(vm.game().problem.answer)
        val before = vm.game()
        vm.answer(before.problem.answer + 1)
        val after = vm.game()
        assertThat(after.streak).isEqualTo(0)
        assertThat(after.problem).isNotEqualTo(before.problem)
        assertThat(after.keypad).isEqualTo(before.keypad)
        assertThat(vm.uiState.value.feedback!!.result).isEqualTo(SubmitResult.Wrong)
        assertThat(link.sent).doesNotContain(RingEvent.MissionCompleted)
    }

    @Test
    fun correctAnswer_reshufflesKeypad() = runTest {
        val vm = viewModel(FakeRingLink(inMission))
        val before = vm.game().keypad
        vm.answer(vm.game().problem.answer)
        assertThat(vm.game().keypad).isNotEqualTo(before)
        assertThat(vm.game().keypad.sorted()).isEqualTo((0..9).toList())
    }

    @Test
    fun typing_refillsTimer_andSignalsAreBatched() = runTest {
        val link = FakeRingLink(inMission)
        val vm = viewModel(link)
        clockMillis += 20_000
        vm.onKey(MathKey.Digit(1))
        assertThat(vm.uiState.value.timerEndsAt).isEqualTo(clockMillis + RingStateMachine.MISSION_MILLIS)
        vm.onKey(MathKey.Digit(2))
        vm.onKey(MathKey.Delete)
        // 처음 한 번은 바로, 나머지는 창이 끝날 때 한 번.
        assertThat(link.sent.count { it == RingEvent.MissionInteraction }).isEqualTo(1)
        advanceTimeBy(InteractionSignal.WINDOW_MILLIS + 1)
        assertThat(link.sent.count { it == RingEvent.MissionInteraction }).isEqualTo(2)
        advanceTimeBy(InteractionSignal.WINDOW_MILLIS * 3)
        assertThat(link.sent.count { it == RingEvent.MissionInteraction }).isEqualTo(2)
    }

    @Test
    fun leavingMission_resetsGame_andRestartStartsOver() = runTest {
        val link = FakeRingLink(inMission)
        val vm = viewModel(link)
        vm.answer(vm.game().problem.answer)
        assertThat(vm.game().streak).isEqualTo(1)
        link.session.value = inMission.copy(phase = RingPhase.Ringing)
        assertThat(vm.uiState.value.game).isNull()
        link.session.value = inMission.copy(phaseEndsAt = 2_000_000L)
        assertThat(vm.game().streak).isEqualTo(0)
        assertThat(vm.uiState.value.timerEndsAt).isEqualTo(2_000_000L)
    }

    @Test
    fun nextMission_opensNewGame() = runTest {
        val link = FakeRingLink(testRingSession(RingPhase.InMission, listOf(math, math)))
        val vm = viewModel(link)
        vm.answer(vm.game().problem.answer)
        vm.answer(vm.game().problem.answer)
        assertThat(vm.game().finished).isTrue()
        link.session.value = link.session.value!!.copy(missionsDone = 1)
        assertThat(vm.game().finished).isFalse()
        assertThat(vm.game().streak).isEqualTo(0)
    }

    private companion object {
        const val SEED = 42
    }
}
