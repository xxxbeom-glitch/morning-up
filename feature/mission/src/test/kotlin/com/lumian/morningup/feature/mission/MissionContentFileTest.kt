package com.lumian.morningup.feature.mission

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.mission.Hangul
import com.lumian.morningup.core.model.mission.MissionContent
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Test

/** 임시 문제, 단어 파일 검사: 단계마다 10개, 보기 4개가 서로 다름, 단어는 한글만. */
class MissionContentFileTest {
    private val content: MissionContent =
        Json.decodeFromString(MissionContent.serializer(), File("src/main/res/raw/mission_content.json").readText())

    @Test
    fun popQuiz_hasTenPerLevel_withFourDistinctChoices() {
        (1..7).forEach { level ->
            val questions = content.popQuiz.filter { it.level == level }
            assertThat(questions).hasSize(10)
            questions.forEach { q ->
                assertThat(q.wrong).hasSize(3)
                assertThat((q.wrong + q.answer).toSet()).hasSize(4)
                assertThat(q.question).isNotEmpty()
            }
        }
    }

    @Test
    fun dropWords_hasTenHangulWordsPerLevel() {
        (1..7).forEach { level ->
            val words = content.words(level)
            assertThat(words).hasSize(10)
            assertThat(words.toSet()).hasSize(10)
            words.forEach { assertThat(Hangul.isHangulWord(it)).isTrue() }
        }
    }
}
