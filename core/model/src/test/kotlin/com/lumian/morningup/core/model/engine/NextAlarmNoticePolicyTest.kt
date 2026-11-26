package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.Alarm
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class NextAlarmNoticePolicyTest {
    private val seoul = ZoneId.of("Asia/Seoul")
    private val noHolidays = object : HolidayCalendar {
        override fun isHoliday(date: LocalDate) = false

        override fun covers(year: Int) = true
    }

    /** 2026-11-25 수요일 21:00(시안 TSK-224). */
    private val now = ZonedDateTime.of(2026, 11, 25, 21, 0, 0, 0, seoul)
    private val weekdays = setOf(
        DayOfWeek.MONDAY,
        DayOfWeek.TUESDAY,
        DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY,
        DayOfWeek.FRIDAY,
    )

    @Test
    fun noEnabledAlarm_isNull() {
        val off = Alarm(id = 1, time = LocalTime.of(6, 30), enabled = false)
        assertThat(NextAlarmNoticePolicy.content(listOf(off), now, noHolidays)).isNull()
        assertThat(NextAlarmNoticePolicy.content(emptyList(), now, noHolidays)).isNull()
    }

    @Test
    fun repeating_showsThursdayAndDays() {
        val alarm = Alarm(id = 1, time = LocalTime.of(6, 30), repeatDays = weekdays)
        val content = NextAlarmNoticePolicy.content(listOf(alarm), now, noHolidays)!!
        assertThat(content.at.toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 26))
        assertThat(content.at.dayOfWeek).isEqualTo(DayOfWeek.THURSDAY)
        assertThat(content.repeatDays).isEqualTo(weekdays)
        assertThat(content.restedDate).isNull()
    }

    @Test
    fun restingOnce_showsRealRingDayAndRestedDay() {
        // 결정 179: 이번만 쉬기 중이면 실제로 울리는 날(금요일)과 쉬는 날(목요일)을 보여요.
        val thursday = LocalDate.of(2026, 11, 26)
        val alarm = Alarm(id = 1, time = LocalTime.of(6, 30), repeatDays = weekdays, skipOnceDate = thursday)
        val content = NextAlarmNoticePolicy.content(listOf(alarm), now, noHolidays)!!
        assertThat(content.at.toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 27))
        assertThat(content.restedDate).isEqualTo(thursday)
    }

    @Test
    fun otherAlarmEarlier_noRestedDay() {
        val thursday = LocalDate.of(2026, 11, 26)
        val resting = Alarm(id = 1, time = LocalTime.of(6, 30), repeatDays = weekdays, skipOnceDate = thursday)
        val other = Alarm(id = 2, time = LocalTime.of(7, 10))
        val content = NextAlarmNoticePolicy.content(listOf(resting, other), now, noHolidays)!!
        assertThat(content.at.toLocalTime()).isEqualTo(LocalTime.of(7, 10))
        assertThat(content.restedDate).isNull()
    }
}
