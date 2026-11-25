package com.lumian.morningup.feature.mission

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.engine.RingEvent
import com.lumian.morningup.core.model.engine.RingPhase
import com.lumian.morningup.core.model.engine.RingStateMachine
import com.lumian.morningup.core.model.mission.DropWordHistory
import com.lumian.morningup.core.model.mission.MathKey
import com.lumian.morningup.core.testing.FakeRingLink
import com.lumian.morningup.core.testing.MainDispatcherRule
import com.lumian.morningup.core.testing.testRingSession
import com.lumian.morningup.feature.mission.play.FeedbackKind
import com.lumian.morningup.feature.mission.play.MissionHostViewModel
import com.lumian.morningup.feature.mission.play.MissionPlay
import com.lumian.morningup.feature.mission.play.RingMissionViewModel
import com.lumian.morningup.feature.mission.play.MissionRunner
import kotlin.random.Random
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/** 울림 미션: 설정대로 열기, 정답, 오답, 59초 다시 채우기, 미션 사이 넘어가기, 다시 울린 뒤 이어 하기. */
class RingMissionViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val clock = TestClock()
    private val math = MissionSetting(MissionType.QuickMath, level = 1, count = 2)
    private val quiz = MissionSetting(MissionType.PopQuiz, level = 1, count = 2)
    private val drop = MissionSetting(MissionType.DropWords, level = 1, count = 5, speed = 1)
    private val shake = MissionSetting(MissionType.ShakeShake, count = 10)

    private fun viewModel(link: FakeRingLink) = RingMissionViewModel(
        link,
        clock.provider,
        Random(SEED),
        testContentSource,
        DropWordHistory.None,
    )

    private fun session(vararg missions: MissionSetting, done: Int = 0, endsAt: Long = 1_059_000L) =
        testRingSession(RingPhase.InMission, missions.toList(), missionsDone = done, phaseEndsAt = endsAt)

    private fun RingMissionViewModel.play() = uiState.value.play!!

    private fun RingMissionViewModel.solveMath() {
        val game = (play() as MissionPlay.Math).game
        game.problem.answer.toString().forEach { onMathKey(MathKey.Digit(it.digitToInt())) }
        onMathKey(MathKey.Confirm)
    }

    private fun RingMissionViewModel.solveQuiz() {
        onQuizAnswer((play() as MissionPlay.Quiz).game.round.answerIndex)
    }

    @Test
    fun opensEachMissionType_withSessionSetting() = runTest {
        listOf(
            math to MissionType.QuickMath,
            quiz to MissionType.PopQuiz,
            drop to MissionType.DropWords,
            shake to MissionType.ShakeShake,
        )
            .forEach { (setting, type) ->
                val vm = viewModel(FakeRingLink(session(setting)))
                assertThat(vm.play().type).isEqualTo(type)
                assertThat(vm.uiState.value.timerEndsAt).isEqualTo(1_059_000L)
                assertThat(vm.uiState.value.total).isEqualTo(1)
            }
    }

    @Test
    fun quickMath_correctTwice_finishesAndSendsCompleted() = runTest {
        val link = FakeRingLink(session(math))
        val vm = viewModel(link)
        vm.solveMath()
        assertThat(vm.play().feedback!!.kind).isEqualTo(FeedbackKind.Correct)
        vm.solveMath()
        assertThat(vm.play().finished).isTrue()
        assertThat(link.sent).contains(RingEvent.MissionCompleted)
    }

    @Test
    fun popQuiz_wrongKeepsCount_holdsOneSecond_thenNewQuestion_correctFinishes() = runTest {
        val link = FakeRingLink(session(quiz.copy(count = 3)))
        val vm = viewModel(link)
        vm.solveQuiz()
        assertThat((vm.play() as MissionPlay.Quiz).game.correct).isEqualTo(1)
        val before = (vm.play() as MissionPlay.Quiz).game.round
        vm.onQuizAnswer((before.answerIndex + 1) % before.choices.size)
        val after = vm.play() as MissionPlay.Quiz
        assertThat(after.feedback!!.kind).isEqualTo(FeedbackKind.Wrong)
        assertThat(after.feedback!!.round).isEqualTo(before)
        // 결정 92: 틀려도 맞힌 수는 그대로. 결정 93: 약 1초 동안 틀린 판을 그대로 보여 줘요.
        assertThat(after.game.correct).isEqualTo(1)
        assertThat(after.game.round.question).isNotEqualTo(before.question)
        assertThat(after.shown.round).isEqualTo(before)
        vm.onQuizAnswer(after.game.round.answerIndex)
        assertThat((vm.play() as MissionPlay.Quiz).game.correct).isEqualTo(1)
        clock.millis += MissionRunner.WRONG_HOLD_MILLIS
        vm.onFrame()
        assertThat((vm.play() as MissionPlay.Quiz).holding).isFalse()
        assertThat(link.sent).doesNotContain(RingEvent.MissionCompleted)
        vm.solveQuiz()
        vm.solveQuiz()
        assertThat(link.sent).contains(RingEvent.MissionCompleted)
    }

    @Test
    fun dropWords_typingWord_countsCorrect_wrongClearsInput() = runTest {
        val link = FakeRingLink(session(drop))
        val vm = viewModel(link)
        val word = (vm.play() as MissionPlay.Drop).game.words.single().text
        vm.onDropKey(word.first().toString())
        val wrong = (vm.play() as MissionPlay.Drop).game.keypad.first { !word.startsWith(word.first() + it) }
        vm.onDropKey(wrong)
        val held = vm.play() as MissionPlay.Drop
        assertThat(held.game.input).isEmpty()
        // 결정 93: 틀린 글자를 약 1초 빨갛게 보여 준 뒤 칸을 비워요.
        assertThat(held.wrongInput).isEqualTo(word.first() + wrong)
        assertThat(vm.play().feedback!!.kind).isEqualTo(FeedbackKind.Wrong)
        clock.millis += MissionRunner.WRONG_HOLD_MILLIS
        vm.onFrame()
        assertThat((vm.play() as MissionPlay.Drop).wrongInput).isNull()
        word.forEach { vm.onDropKey(it.toString()) }
        val after = vm.play() as MissionPlay.Drop
        assertThat(after.game.correct).isEqualTo(1)
        assertThat(after.feedback!!.word!!.text).isEqualTo(word)
    }

    @Test
    fun dropWords_frameMovesTime_wordReachesFloorWithoutPenalty() = runTest {
        val vm = viewModel(FakeRingLink(session(drop)))
        val first = (vm.play() as MissionPlay.Drop).game.words.single()
        clock.millis += 10_000
        vm.onFrame()
        val after = vm.play() as MissionPlay.Drop
        assertThat(after.game.words.map { it.id }).doesNotContain(first.id)
        assertThat(after.game.correct).isEqualTo(0)
    }

    @Test
    fun shake_countsDown_andFinishes() = runTest {
        val link = FakeRingLink(session(shake))
        val vm = viewModel(link)
        repeat(9) { vm.onShake() }
        assertThat((vm.play() as MissionPlay.Shake).game.remaining).isEqualTo(1)
        assertThat(link.sent).doesNotContain(RingEvent.MissionCompleted)
        vm.onShake()
        assertThat(link.sent).contains(RingEvent.MissionCompleted)
    }

    @Test
    fun interaction_refillsTimer_andSignalsAreBatched() = runTest {
        val link = FakeRingLink(session(math))
        val vm = viewModel(link)
        clock.millis += 20_000
        vm.onMathKey(MathKey.Digit(1))
        assertThat(vm.uiState.value.timerEndsAt).isEqualTo(clock.millis + RingStateMachine.MISSION_MILLIS)
        vm.onMathKey(MathKey.Digit(2))
        vm.onMathKey(MathKey.Delete)
        assertThat(link.sent.count { it == RingEvent.MissionInteraction }).isEqualTo(1)
        advanceTimeBy(InteractionSignal.WINDOW_MILLIS + 1)
        assertThat(link.sent.count { it == RingEvent.MissionInteraction }).isEqualTo(2)
    }

    @Test
    fun nextMission_showsTransition_thenOpens_withFreshTimer() = runTest {
        val link = FakeRingLink(session(math, quiz, shake))
        val vm = viewModel(link)
        vm.solveMath()
        vm.solveMath()
        // 엔진이 다음 미션으로 넘겼어요.
        link.session.value =
            session(math, quiz, shake, done = 1, endsAt = clock.millis + RingStateMachine.MISSION_MILLIS)
        assertThat(vm.uiState.value.transitionTo).isEqualTo(2)
        assertThat(vm.uiState.value.total).isEqualTo(3)
        vm.onQuizAnswer(0)
        assertThat(vm.play().type).isEqualTo(MissionType.QuickMath)
        clock.millis += MissionHostViewModel.TRANSITION_MILLIS - 1
        vm.onFrame()
        assertThat(vm.uiState.value.transitionTo).isEqualTo(2)
        clock.millis += 1
        val sentBefore = link.sent.count { it == RingEvent.MissionInteraction }
        vm.onFrame()
        assertThat(vm.uiState.value.transitionTo).isNull()
        assertThat(vm.play().type).isEqualTo(MissionType.PopQuiz)
        assertThat(vm.uiState.value.index).isEqualTo(1)
        assertThat(vm.uiState.value.timerEndsAt).isEqualTo(clock.millis + RingStateMachine.MISSION_MILLIS)
        // 59초를 새로 시작하도록 엔진에도 알려요.
        assertThat(link.sent.count { it == RingEvent.MissionInteraction }).isEqualTo(sentBefore + 1)
    }

    @Test
    fun timeoutInSecondMission_ringsAgain_thenResumesFromThatMission() = runTest {
        val link = FakeRingLink(session(math, quiz, done = 1))
        val vm = viewModel(link)
        assertThat(vm.play().type).isEqualTo(MissionType.PopQuiz)
        vm.solveQuiz()
        link.session.value = session(math, quiz, done = 1).copy(phase = RingPhase.Ringing)
        assertThat(vm.uiState.value.play).isNull()
        link.session.value = session(math, quiz, done = 1, endsAt = 2_000_000L)
        // 끝낸 Quick Math는 건너뛰고, 하던 Pop Quiz는 처음부터예요(넘어가기 없이 바로).
        assertThat(vm.uiState.value.transitionTo).isNull()
        assertThat(vm.play().type).isEqualTo(MissionType.PopQuiz)
        assertThat((vm.play() as MissionPlay.Quiz).game.correct).isEqualTo(0)
        assertThat(vm.uiState.value.timerEndsAt).isEqualTo(2_000_000L)
    }

    private companion object {
        const val SEED = 42
    }
}
