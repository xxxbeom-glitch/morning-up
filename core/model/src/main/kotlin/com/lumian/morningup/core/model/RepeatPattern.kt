package com.lumian.morningup.core.model

import java.time.DayOfWeek

/** 반복 요일을 사람이 읽는 묶음으로. 다이얼로그 '오전 6:30 평일 알람이에요' 같은 데 써요. */
sealed interface RepeatPattern {
    data object Once : RepeatPattern

    data object Everyday : RepeatPattern

    data object Weekdays : RepeatPattern

    data object Weekend : RepeatPattern

    /** 월요일부터 순서대로. */
    data class Custom(val days: List<DayOfWeek>) : RepeatPattern

    companion object {
        private val WEEKDAYS = DayOfWeek.entries.filter { it != DayOfWeek.SATURDAY && it != DayOfWeek.SUNDAY }.toSet()
        private val WEEKEND = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

        fun of(days: Set<DayOfWeek>): RepeatPattern = when (days) {
            emptySet<DayOfWeek>() -> Once
            DayOfWeek.entries.toSet() -> Everyday
            WEEKDAYS -> Weekdays
            WEEKEND -> Weekend
            else -> Custom(days.sorted())
        }
    }
}
