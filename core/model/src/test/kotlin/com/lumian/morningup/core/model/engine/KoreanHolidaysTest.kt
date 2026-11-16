package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import org.junit.Test

class KoreanHolidaysTest {
    private fun d(y: Int, m: Int, day: Int) = LocalDate.of(y, m, day)

    @Test
    fun seollal2026_andChuseok2026_areHolidays() {
        listOf(d(2026, 2, 16), d(2026, 2, 17), d(2026, 2, 18), d(2026, 9, 24), d(2026, 9, 25), d(2026, 9, 26))
            .forEach { assertThat(KoreanHolidays.isHoliday(it)).isTrue() }
    }

    @Test
    fun substituteHolidays2026() {
        // 3/1 일요일 → 3/2, 부처님오신날 5/24 일요일 → 5/25, 8/15 토요일 → 8/17, 10/3 토요일 → 10/5
        listOf(d(2026, 3, 2), d(2026, 5, 25), d(2026, 8, 17), d(2026, 10, 5))
            .forEach { assertThat(KoreanHolidays.isHoliday(it)).isTrue() }
    }

    @Test
    fun newHolidays2026_laborDayAndConstitutionDay() {
        assertThat(KoreanHolidays.isHoliday(d(2026, 5, 1))).isTrue()
        assertThat(KoreanHolidays.isHoliday(d(2026, 7, 17))).isTrue()
    }

    @Test
    fun localElection2026() {
        assertThat(KoreanHolidays.isHoliday(d(2026, 6, 3))).isTrue()
    }

    @Test
    fun noSubstitute_forMemorialDay_orChuseokOnSaturday() {
        assertThat(KoreanHolidays.isHoliday(d(2026, 6, 8))).isFalse()
        assertThat(KoreanHolidays.isHoliday(d(2026, 9, 28))).isFalse()
    }

    @Test
    fun holidays2027_withSubstitutes() {
        listOf(
            d(2027, 1, 1), d(2027, 2, 6), d(2027, 2, 7), d(2027, 2, 8), d(2027, 2, 9),
            d(2027, 3, 1), d(2027, 5, 5), d(2027, 5, 13), d(2027, 8, 16), d(2027, 9, 14),
            d(2027, 9, 15), d(2027, 9, 16), d(2027, 10, 4), d(2027, 10, 11), d(2027, 12, 27),
        ).forEach { assertThat(KoreanHolidays.isHoliday(it)).isTrue() }
    }

    @Test
    fun ordinaryWeekday_isNotHoliday() {
        assertThat(KoreanHolidays.isHoliday(d(2026, 11, 17))).isFalse()
    }

    @Test
    fun covers_onlyListedYears() {
        assertThat(KoreanHolidays.covers(2026)).isTrue()
        assertThat(KoreanHolidays.covers(2027)).isTrue()
        assertThat(KoreanHolidays.covers(2028)).isFalse()
        assertThat(KoreanHolidays.covers(2025)).isFalse()
    }
}
