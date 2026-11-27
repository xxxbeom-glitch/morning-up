package com.lumian.morningup.core.model.mission

import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test

/** Pop Quiz: 4지선다, 연속 정답, 틀리면 0부터 새 문제. */
class PopQuizGameTest {
    private val bank = (1..10).map { n ->
        QuizQuestion(
            level = 1,
            category = QuizCategory.entries[n % QuizCategory.entries.size],
            question = "q$n",
            answer = "a$n",
            wrong = listOf("w${n}x", "w${n}y", "w${n}z"),
        )
    } + QuizQuestion(2, QuizCategory.Science, "other", "o", listOf("1", "2", "3"))

    private val game = PopQuizGame(level = 1, target = 3, questions = bank, random = Random(8))

    @Test
    fun start_usesOnlyThatLevel_withFourShuffledChoices() {
        val s = game.start()
        assertThat(s.round.question.level).isEqualTo(1)
        assertThat(s.round.choices).hasSize(4)
        assertThat(s.round.choices).containsExactlyElementsIn(listOf(s.round.question.answer) + s.round.question.wrong)
        assertThat(s.correct).isEqualTo(0)
        assertThat(s.target).isEqualTo(3)
    }

    @Test
    fun answerPosition_isNotAlwaysFirst() {
        val positions = (1..30).map { PopQuizGame(1, 3, bank, Random(it)).start().round.answerIndex }.toSet()
        assertThat(positions.size).isAtLeast(3)
    }

    @Test
    fun correct_raisesCount_andNextQuestionDiffers() {
        val s = game.start()
        val (next, result) = game.answer(s, s.round.answerIndex)
        assertThat(result).isEqualTo(QuizResult.Correct)
        assertThat(next.correct).isEqualTo(1)
        assertThat(next.round.question).isNotEqualTo(s.round.question)
    }

    @Test
    fun wrong_keepsCount_andChangesQuestion() {
        var s = game.start()
        s = game.answer(s, s.round.answerIndex).first
        val wrongIndex = (0..3).first { it != s.round.answerIndex }
        val (next, result) = game.answer(s, wrongIndex)
        assertThat(result).isEqualTo(QuizResult.Wrong)
        assertThat(next.correct).isEqualTo(1)
        assertThat(next.round.question).isNotEqualTo(s.round.question)
    }

    @Test
    fun targetStreak_finishes_thenIgnores() {
        var s = game.start()
        var result = QuizResult.Ignored
        repeat(3) {
            val (n, r) = game.answer(s, s.round.answerIndex)
            s = n
            result = r
        }
        assertThat(result).isEqualTo(QuizResult.Finished)
        assertThat(s.finished).isTrue()
        assertThat(game.answer(s, 0).second).isEqualTo(QuizResult.Ignored)
    }

    @Test
    fun noRepeat_untilAllLevelQuestionsUsed() {
        var s = game.start()
        val seen = mutableListOf(s.round.question.question)
        repeat(9) {
            val wrongIndex = (0..3).first { it != s.round.answerIndex }
            s = game.answer(s, wrongIndex).first
            seen += s.round.question.question
        }
        assertThat(seen.toSet()).hasSize(10)
    }

    @Test
    fun targetBelowMinimum_isOne() {
        // 결정 187로 최소가 1문제예요.
        assertThat(PopQuizGame(1, 0, bank, Random(1)).start().target).isEqualTo(1)
    }

    @Test
    fun categories_filterQuestions_andEnoughLevelPoolDoesNotBorrow() {
        val g = PopQuizGame(1, 3, bank, Random(2), categories = setOf(QuizCategory.Science))
        val asked = generateSequence(g.start()) { g.answer(it, (it.round.answerIndex + 1) % 4).first }
            .take(10).map { it.round.question }.toList()
        // 과학 1단계는 1문제뿐이라 2단계 'other'까지 빌려요. 다른 분야는 나오지 않아요.
        assertThat(asked.map { it.category }.toSet()).containsExactly(QuizCategory.Science)
        assertThat(asked.map { it.question }.toSet()).contains("other")
        assertThat(PopQuizGame.poolOf(bank, 1, MissionSettingAll)).hasSize(10)
    }

    private companion object {
        val MissionSettingAll = QuizCategory.entries.toSet()
    }
}
