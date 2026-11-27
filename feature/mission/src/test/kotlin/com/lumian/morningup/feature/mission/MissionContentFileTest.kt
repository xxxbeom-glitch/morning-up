package com.lumian.morningup.feature.mission

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.mission.Hangul
import com.lumian.morningup.core.model.mission.MissionContent
import com.lumian.morningup.core.model.mission.PopQuizGame
import com.lumian.morningup.core.model.mission.QuizCategory
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Test

/**
 * 문제, 단어 파일 검사(11-20 사용자 결정 '추천안대로': Pop Quiz 단계마다 50문제, Drop Words 단계마다 60단어).
 * 은행 크기, 중복 없음, 보기 4개가 서로 다름, 단어는 한글만, 화면 글에 가운뎃점과 긴 줄표 없음.
 */
class MissionContentFileTest {
    private val content: MissionContent =
        Json.decodeFromString(MissionContent.serializer(), File("src/main/res/raw/mission_content.json").readText())

    @Test
    fun popQuiz_hasFiftyPerLevel_withFourDistinctChoices() {
        (1..7).forEach { level ->
            val questions = content.popQuiz.filter { it.level == level }
            assertThat(questions).hasSize(QUIZ_PER_LEVEL)
            questions.forEach { q ->
                assertThat(q.wrong).hasSize(3)
                assertThat((q.wrong + q.answer).toSet()).hasSize(4)
                assertThat(q.question).isNotEmpty()
            }
        }
    }

    /** TSK-284 2절: 한 분야만 골라도 단계마다 문제 묶음이 10개 이상이에요(옆 단계에서 빌려 채움). */
    @Test
    fun popQuiz_singleCategory_poolHasAtLeastTenEveryLevel() {
        QuizCategory.entries.forEach { category ->
            (1..7).forEach { level ->
                val pool = PopQuizGame.poolOf(content.popQuiz, level, setOf(category))
                assertThat(pool.size).isAtLeast(PopQuizGame.MIN_POOL)
                assertThat(pool.map { it.category }.toSet()).containsExactly(category)
            }
        }
    }

    @Test
    fun popQuiz_issuesOnlyLevel3_borrowsNeighbourLevels() {
        val pool = PopQuizGame.poolOf(content.popQuiz, 3, setOf(QuizCategory.Issues))
        assertThat(pool.size).isAtLeast(PopQuizGame.MIN_POOL)
        assertThat(pool.map { it.level }.toSet()).containsAtLeast(2, 3, 4)
    }

    @Test
    fun popQuiz_questionsAreUniqueAcrossLevels() {
        val texts = content.popQuiz.map { it.question.normalized() }
        assertThat(texts.toSet()).hasSize(texts.size)
    }

    @Test
    fun dropWords_hasSixtyHangulWordsPerLevel_uniqueAcrossLevels() {
        (1..7).forEach { level ->
            val words = content.words(level)
            assertThat(words).hasSize(WORDS_PER_LEVEL)
            words.forEach { assertThat(Hangul.isHangulWord(it)).isTrue() }
        }
        val all = content.dropWords.flatMap { it.words }
        assertThat(all.toSet()).hasSize(all.size)
    }

    @Test
    fun screenText_hasNoMiddleDotOrLongDash() {
        val texts = content.popQuiz.flatMap { listOf(it.question, it.answer) + it.wrong } +
            content.dropWords.flatMap { it.words }
        texts.forEach { text -> FORBIDDEN.forEach { assertThat(text).doesNotContain(it) } }
    }

    private fun String.normalized() = filterNot { it.isWhitespace() || it == '?' }

    private companion object {
        const val QUIZ_PER_LEVEL = 50
        const val WORDS_PER_LEVEL = 60
        val FORBIDDEN = listOf("·", "—", "–")
    }
}
