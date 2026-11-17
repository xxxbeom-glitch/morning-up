package com.lumian.morningup.core.model.mission

import kotlinx.serialization.Serializable

/** Drop Words 한 단계의 단어 묶음. */
@Serializable
data class WordSet(val level: Int, val words: List<String>)

/**
 * 미션 문제와 단어 목록 전체. 지금은 개발용 임시 데이터(단계마다 10개)이고, 실제 목록(F15, F16)은
 * 같은 모양의 파일(`feature/mission/src/main/res/raw/mission_content.json`)만 바꾸면 돼요.
 */
@Serializable
data class MissionContent(val popQuiz: List<QuizQuestion>, val dropWords: List<WordSet>) {
    fun words(level: Int): List<String> = dropWords.firstOrNull { it.level == level }?.words.orEmpty()
}
