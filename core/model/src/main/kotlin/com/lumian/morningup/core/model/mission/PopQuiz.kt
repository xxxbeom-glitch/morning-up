package com.lumian.morningup.core.model.mission

import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import kotlin.random.Random
import kotlinx.serialization.Serializable

/** Pop Quiz 분야(11-16 사용자 확정: 고르는 줄 없이 섞어서 무작위). 화면 이름은 feature 쪽 글자 파일에 있어요. */
@Serializable
enum class QuizCategory { Economy, Society, KoreanHistory, WorldHistory, Science, People, Issues }

/** 문제 하나. 오답 보기는 3개예요. */
@Serializable
data class QuizQuestion(
    val level: Int,
    val category: QuizCategory,
    val question: String,
    val answer: String,
    val wrong: List<String>,
)

/** 화면에 낸 문제 한 판. 보기 순서는 낼 때마다 섞어요. */
data class QuizRound(val question: QuizQuestion, val choices: List<String>) {
    val answerIndex: Int get() = choices.indexOf(question.answer)
}

data class PopQuizState(
    val level: Int,
    val target: Int,
    /** 지금까지 맞힌 문제 수. 틀려도 줄지 않아요(결정 92). */
    val correct: Int,
    val round: QuizRound,
    val finished: Boolean = false,
)

enum class QuizResult { Correct, Wrong, Finished, Ignored }

/**
 * Pop Quiz 규칙(mission-levels.md Pop Quiz, dev-plan 모션 표).
 * - 4지선다예요. [target]개 맞히면 끝나요(최소 2문제).
 * - 틀려도 맞힌 수는 그대로이고 새 문제로 바뀌어요(결정 92). 정답은 보여 주지 않아요.
 * - 그 단계 문제를 섞어 차례로 내요. 다 내기 전에는 같은 문제가 다시 나오지 않고, 연달아 같은 문제도 없어요.
 */
class PopQuizGame(level: Int, target: Int, questions: List<QuizQuestion>, private val random: Random) {
    private val range = MissionSetting.rangeOf(MissionType.PopQuiz)
    private val level = level.coerceIn(range.levels)
    private val target = target.coerceIn(range.counts)
    private val pool = questions.filter { it.level == this.level }.ifEmpty { questions }.ifEmpty { error("문제가 없어요") }
    private var deck = ArrayDeque<QuizQuestion>()

    fun start(): PopQuizState = PopQuizState(level = level, target = target, correct = 0, round = nextRound(null))

    fun answer(state: PopQuizState, index: Int): Pair<PopQuizState, QuizResult> {
        if (state.finished || index !in state.round.choices.indices) return state to QuizResult.Ignored
        if (index != state.round.answerIndex) {
            return state.copy(round = nextRound(state.round.question)) to QuizResult.Wrong
        }
        val correct = state.correct + 1
        if (correct >= state.target) return state.copy(correct = correct, finished = true) to QuizResult.Finished
        return state.copy(correct = correct, round = nextRound(state.round.question)) to QuizResult.Correct
    }

    private fun nextRound(previous: QuizQuestion?): QuizRound {
        if (deck.isEmpty()) {
            var shuffled = pool.shuffled(random)
            if (pool.size > 1 && shuffled.first() == previous) shuffled = shuffled.drop(1) + shuffled.first()
            deck = ArrayDeque(shuffled)
        }
        val question = deck.removeFirst()
        return QuizRound(question, (question.wrong.take(WRONG_CHOICES) + question.answer).shuffled(random))
    }

    private companion object {
        const val WRONG_CHOICES = 3
    }
}
