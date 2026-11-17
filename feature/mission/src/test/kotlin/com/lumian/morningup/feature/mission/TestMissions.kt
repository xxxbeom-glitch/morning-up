package com.lumian.morningup.feature.mission

import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.model.mission.MissionContent
import com.lumian.morningup.core.model.mission.QuizCategory
import com.lumian.morningup.core.model.mission.QuizQuestion
import com.lumian.morningup.core.model.mission.WordSet
import com.lumian.morningup.feature.mission.play.MissionContentSource
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** 테스트용 작은 문제, 단어 목록. */
val testContent = MissionContent(
    popQuiz = (1..4).map { n ->
        QuizQuestion(
            level = 1,
            category = QuizCategory.Science,
            question = "문제 $n",
            answer = "정답 $n",
            wrong = listOf("오답 ${n}가", "오답 ${n}나", "오답 ${n}다"),
        )
    },
    dropWords = listOf(WordSet(1, listOf("사과", "나무", "바다", "하늘", "구름", "기차"))),
)

val testContentSource = MissionContentSource { testContent }

/** 손으로 돌리는 시계. */
class TestClock(var millis: Long = 1_000_000L) {
    val provider = NowProvider { ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.of("Asia/Seoul")) }
}
