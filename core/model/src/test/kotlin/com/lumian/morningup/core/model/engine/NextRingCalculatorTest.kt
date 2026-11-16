package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.Alarm
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Test

class NextRingCalculatorTest {
    private val seoul = ZoneId.of("Asia/Seoul")
    private val newYork = ZoneId.of("America/New_York")
    private val weekdays = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)

    /** 2026-11-17은 화요일이에요. */
    private fun at(y: Int, mo: Int, d: Int, h: Int, mi: Int, s: Int = 0, zone: ZoneId = seoul) =
        ZonedDateTime.of(y, mo, d, h, mi, s, 0, zone)

    private fun alarm(
        h: Int,
        m: Int,
        days: Set<DayOfWeek> = emptySet(),
        skipHolidays: Boolean = false,
        skipOnce: LocalDate? = null,
        enabled: Boolean = true,
    ) = Alarm(
        time = LocalTime.of(h, m),
        repeatDays = days,
        skipHolidays = skipHolidays,
        skipOnceDate = skipOnce,
        enabled = enabled,
    )

    private fun next(alarm: Alarm, now: ZonedDateTime) = NextRingCalculator.next(alarm, now, KoreanHolidays)

    @Test
    fun disabledAlarm_hasNoNext() {
        assertThat(next(alarm(7, 0, enabled = false), at(2026, 11, 17, 6, 0))).isNull()
    }

    @Test
    fun oneShot_laterToday_ringsToday() {
        assertThat(next(alarm(7, 0), at(2026, 11, 17, 6, 0))).isEqualTo(at(2026, 11, 17, 7, 0))
    }

    @Test
    fun oneShot_alreadyPassed_ringsTomorrow() {
        assertThat(next(alarm(7, 0), at(2026, 11, 17, 8, 0))).isEqualTo(at(2026, 11, 18, 7, 0))
    }

    @Test
    fun oneShot_sameMinute_isConsideredPassed() {
        // 지금이 정확히 7:00:00이면 이미 울린 것으로 보고 내일로 가요(같은 시각 두 번 울림 방지).
        assertThat(next(alarm(7, 0), at(2026, 11, 17, 7, 0))).isEqualTo(at(2026, 11, 18, 7, 0))
        assertThat(next(alarm(7, 0), at(2026, 11, 17, 6, 59, 59))).isEqualTo(at(2026, 11, 17, 7, 0))
    }

    @Test
    fun oneShot_ignoresHolidaysAndSkipOnce() {
        val a = alarm(7, 0, skipHolidays = true, skipOnce = LocalDate.of(2026, 12, 25))
        assertThat(next(a, at(2026, 12, 24, 8, 0))).isEqualTo(at(2026, 12, 25, 7, 0))
    }

    @Test
    fun midnight_0000_isNextDayStart() {
        assertThat(next(alarm(0, 0), at(2026, 11, 17, 23, 59))).isEqualTo(at(2026, 11, 18, 0, 0))
    }

    @Test
    fun alarmAt2359_justBefore() {
        assertThat(next(alarm(23, 59), at(2026, 11, 17, 23, 58))).isEqualTo(at(2026, 11, 17, 23, 59))
    }

    @Test
    fun weekdays_fromFridayEvening_goesToMonday() {
        // 2026-11-20 금요일 저녁 → 11-23 월요일
        assertThat(next(alarm(7, 0, weekdays), at(2026, 11, 20, 21, 0))).isEqualTo(at(2026, 11, 23, 7, 0))
    }

    @Test
    fun weekendOnly_fromTuesday_goesToSaturday() {
        assertThat(next(alarm(9, 30, setOf(SATURDAY, SUNDAY)), at(2026, 11, 17, 6, 0)))
            .isEqualTo(at(2026, 11, 21, 9, 30))
    }

    @Test
    fun repeating_todayStillAhead_ringsToday() {
        assertThat(next(alarm(7, 0, weekdays), at(2026, 11, 17, 6, 0))).isEqualTo(at(2026, 11, 17, 7, 0))
    }

    @Test
    fun repeating_crossesMidnight_whenNextDayIsInRepeat() {
        assertThat(next(alarm(0, 30, setOf(WEDNESDAY)), at(2026, 11, 17, 23, 50))).isEqualTo(at(2026, 11, 18, 0, 30))
    }

    @Test
    fun skipHolidays_skipsSeollalAndSubstitute() {
        // 2027 설: 2/6(토)~2/8(월), 대체공휴일 2/9(화). 평일 알람이면 2/10(수)에 울려요.
        assertThat(next(alarm(7, 0, weekdays, skipHolidays = true), at(2027, 2, 5, 8, 0)))
            .isEqualTo(at(2027, 2, 10, 7, 0))
    }

    @Test
    fun skipHolidaysOff_ringsOnHoliday() {
        assertThat(next(alarm(7, 0, weekdays), at(2027, 2, 5, 8, 0))).isEqualTo(at(2027, 2, 8, 7, 0))
    }

    @Test
    fun skipOnce_skipsOnlyThatDay() {
        val a = alarm(7, 0, weekdays, skipOnce = LocalDate.of(2026, 11, 18))
        assertThat(next(a, at(2026, 11, 17, 8, 0))).isEqualTo(at(2026, 11, 19, 7, 0))
    }

    @Test
    fun skipOnce_inThePast_isIgnored() {
        val a = alarm(7, 0, weekdays, skipOnce = LocalDate.of(2026, 11, 10))
        assertThat(next(a, at(2026, 11, 17, 6, 0))).isEqualTo(at(2026, 11, 17, 7, 0))
    }

    @Test
    fun skipOnce_andHoliday_stack() {
        // 2026-12-24(목) 이번만 쉬기 + 12-25(금) 성탄절 → 12-28(월)
        val a = alarm(7, 0, weekdays, skipHolidays = true, skipOnce = LocalDate.of(2026, 12, 24))
        assertThat(next(a, at(2026, 12, 23, 8, 0))).isEqualTo(at(2026, 12, 28, 7, 0))
    }

    @Test
    fun yearsOutsideHolidayList_ringByWeekdayOnly() {
        // 2028-01-03(월). 목록 밖이라 공휴일 끄기가 있어도 요일대로 울려요.
        assertThat(next(alarm(7, 0, weekdays, skipHolidays = true), at(2028, 1, 2, 12, 0)))
            .isEqualTo(at(2028, 1, 3, 7, 0))
    }

    @Test
    fun timeZone_usesLocalWallClockOfThatZone() {
        val now = at(2026, 11, 17, 6, 0, zone = newYork)
        assertThat(next(alarm(7, 0), now)).isEqualTo(at(2026, 11, 17, 7, 0, zone = newYork))
    }

    @Test
    fun timeZoneChange_sameInstantDifferentZone_givesDifferentResult() {
        val instant = at(2026, 11, 17, 6, 0).toInstant()
        val inSeoul = next(alarm(7, 0), instant.atZone(seoul))!!
        val inNy = next(alarm(7, 0), instant.atZone(newYork))!!
        assertThat(inSeoul.toLocalTime()).isEqualTo(LocalTime.of(7, 0))
        assertThat(inNy.toLocalTime()).isEqualTo(LocalTime.of(7, 0))
        assertThat(inSeoul.toInstant()).isNotEqualTo(inNy.toInstant())
    }

    @Test
    fun dstGap_missingTime_movesForwardOneHour() {
        // 뉴욕 2026-03-08(일) 2:00 → 3:00. 2:30은 없는 시각이라 3:30에 울려요.
        val result = next(alarm(2, 30, setOf(SUNDAY)), at(2026, 3, 7, 12, 0, zone = newYork))!!
        assertThat(result.toLocalDateTime()).isEqualTo(LocalDateTime.of(2026, 3, 8, 3, 30))
    }

    @Test
    fun dstOverlap_repeatedTime_usesEarlierOne() {
        // 뉴욕 2026-11-01(일) 2:00 → 1:00. 1:30이 두 번 생기면 앞(EDT, -4)을 써요.
        val result = next(alarm(1, 30, setOf(SUNDAY)), at(2026, 10, 31, 12, 0, zone = newYork))!!
        assertThat(result.toLocalDateTime()).isEqualTo(LocalDateTime.of(2026, 11, 1, 1, 30))
        assertThat(result.offset).isEqualTo(ZoneOffset.ofHours(-4))
    }

    @Test
    fun dstOverlap_afterFirstRing_doesNotRingAgainInSecondHalf() {
        // 첫 1:30(EDT)에 울린 직후 다시 계산하면 두 번째 1:30(EST)이 아니라 다음 주예요.
        val firstRing = ZonedDateTime.ofLocal(LocalDateTime.of(2026, 11, 1, 1, 30), newYork, ZoneOffset.ofHours(-4))
        val result = next(alarm(1, 30, setOf(SUNDAY)), firstRing.plusMinutes(1))!!
        assertThat(result.toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 8))
    }
}
