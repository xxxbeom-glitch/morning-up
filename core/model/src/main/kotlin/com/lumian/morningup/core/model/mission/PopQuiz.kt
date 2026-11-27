package com.lumian.morningup.core.model.mission

import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import kotlin.random.Random
import kotlinx.serialization.Serializable

/** Pop Quiz 분야. 미션 설정 시트에서 여러 개 골라요(TSK-284). 화면 이름은 core/ui 글자 파일(ui_quiz_category_*)에 있어요. */
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
 * - 4지선다예요. [target]개 맞히면 끝나요(최소 1문제, 결정 187).
 * - 틀려도 맞힌 수는 그대로이고 새 문제로 바뀌어요(결정 92). 정답은 보여 주지 않아요.
 * - 그 단계 문제를 섞어 차례로 내요. 다 내기 전에는 같은 문제가 다시 나오지 않고, 연달아 같은 문제도 없어요.
 * - 고른 분야([categories])의 문제만 내요. 그 단계 문제가 [MIN_POOL]개보다 적으면 같은 분야의 한 단계 아래, 위를,
 *   그래도 적으면 두 단계 아래, 위까지 더해요(docs/plan-11-quiz-category.md 2절).
 */
class PopQuizGame(
    level: Int,
    target: Int,
    questions: List<QuizQuestion>,
    private val random: Random,
    categories: Set<QuizCategory> = MissionSetting.ALL_QUIZ_CATEGORIES,
) {
    private val range = MissionSetting.rangeOf(MissionType.PopQuiz)
    private val level = level.coerceIn(range.levels)
    private val target = target.coerceIn(range.counts)
    private val pool = poolOf(questions, this.level, categories)
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

    companion object {
        private const val WRONG_CHOICES = 3

        /** 문제 묶음이 이보다 적으면 옆 단계에서 빌려 채워요. */
        const val MIN_POOL = 10
        private const val MAX_BORROW = 2

        /** 2절 규칙의 문제 묶음. 고른 분야에 문제가 하나도 없으면 전체 분야로 내요. */
        fun poolOf(questions: List<QuizQuestion>, level: Int, categories: Set<QuizCategory>): List<QuizQuestion> {
            val chosen = questions.filter { it.category in categories }.ifEmpty { questions }
            val pool = chosen.filter { it.level == level }.toMutableList()
            for (step in 1..MAX_BORROW) {
                if (pool.size >= MIN_POOL) break
                pool += chosen.filter { it.level == level - step || it.level == level + step }
            }
            return pool.ifEmpty { chosen }.ifEmpty { error("문제가 없어요") }
        }
    }
}
