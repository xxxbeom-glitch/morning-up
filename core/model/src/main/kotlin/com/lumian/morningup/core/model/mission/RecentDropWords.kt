package com.lumian.morningup.core.model.mission

import java.time.LocalDate

/** Drop Words에서 [date]에 [level] 단계로 낸 단어 하나. */
data class DropWordUse(val date: LocalDate, val level: Int, val word: String)

/**
 * Drop Words 최근 단어 기록(TSK-205). 며칠째 같은 단어만 나온다는 사용자 보고(11-25)로, 최근 사흘 동안 낸 단어를
 * 기기에 기억해 다음 날 같은 단계에서 빼요. 구현은 `:feature:mission`의 기기 보호 저장소예요. 미리보기는 남기지 않아요.
 */
interface DropWordHistory {
    /** [level] 단계에서 최근 사흘 동안 낸 단어. 오래된 것부터예요. */
    fun recent(level: Int): List<String>

    /** [level] 단계에서 [word]를 냈어요. */
    fun record(level: Int, word: String)

    /** 기억하지 않아요(미리보기, 시험). */
    data object None : DropWordHistory {
        override fun recent(level: Int): List<String> = emptyList()

        override fun record(level: Int, word: String) = Unit
    }
}

/** 최근 단어 기록을 고르고 줄이는 규칙. 저장 방식과 떼어 두어 시험하기 쉬워요. */
object RecentDropWords {
    /** 오늘을 넣어 이 날 수 안에 낸 단어를 빼요. 사흘이면 오늘, 어제, 그제예요. */
    const val DAYS = 3L

    /** [today] 기준 최근 [DAYS]일 안에 [level]에서 낸 단어. 오래된 것부터, 같은 단어는 마지막에 낸 때 자리 하나만이에요. */
    fun recent(uses: List<DropWordUse>, level: Int, today: LocalDate): List<String> {
        val from = today.minusDays(DAYS - 1)
        val inWindow = uses.filter { it.level == level && !it.date.isBefore(from) && !it.date.isAfter(today) }
        return inWindow.asReversed().map { it.word }.distinct().asReversed()
    }

    /** [use]를 더하고 기간이 지난 기록을 지워요. 순서는 낸 차례 그대로예요. */
    fun add(uses: List<DropWordUse>, use: DropWordUse): List<DropWordUse> {
        val from = use.date.minusDays(DAYS - 1)
        return uses.filter { !it.date.isBefore(from) && !it.date.isAfter(use.date) } + use
    }
}
