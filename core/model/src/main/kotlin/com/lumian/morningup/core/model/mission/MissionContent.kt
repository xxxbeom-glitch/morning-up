package com.lumian.morningup.core.model.mission

import kotlinx.serialization.Serializable

/** Drop Words 한 단계의 단어 묶음. */
@Serializable
data class WordSet(val level: Int, val words: List<String>)

/**
 * 미션 문제와 단어 목록 전체(`feature/mission/src/main/res/raw/mission_content.json`).
 * 11-20 사용자 결정 '추천안대로': Pop Quiz 단계마다 50문제, Drop Words 단계마다 60단어.
 */
@Serializable
data class MissionContent(val popQuiz: List<QuizQuestion>, val dropWords: List<WordSet>) {
    fun words(level: Int): List<String> = dropWords.firstOrNull { it.level == level }?.words.orEmpty()
}
