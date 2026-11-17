package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.Alarm
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class NextAlarmTest {
    private val seoul = ZoneId.of("Asia/Seoul")
    private val noHolidays = object : HolidayCalendar {
        override fun isHoliday(date: LocalDate) = false

        override fun covers(year: Int) = true
    }

    /** 2026-11-17 화요일 23:10. */
    private val now = ZonedDateTime.of(2026, 11, 17, 23, 10, 0, 0, seoul)

    private fun alarm(id: Long, h: Int, m: Int, enabled: Boolean = true, days: Set<DayOfWeek> = emptySet()) =
        Alarm(id = id, time = LocalTime.of(h, m), enabled = enabled, repeatDays = days)

    @Test
    fun noAlarms_isNull() {
        assertThat(NextAlarm.find(emptyList(), now, noHolidays)).isNull()
    }

    @Test
    fun allOff_isNull() {
        assertThat(NextAlarm.find(listOf(alarm(1, 6, 30, enabled = false)), now, noHolidays)).isNull()
    }

    @Test
    fun picksEarliest_acrossMidnight() {
        val alarms = listOf(alarm(1, 23, 5), alarm(2, 6, 30), alarm(3, 23, 30))
        val next = NextAlarm.find(alarms, now, noHolidays)!!
        // 23:05는 이미 지나 내일이고, 23:30이 오늘 가장 먼저예요.
        assertThat(next.alarm.id).isEqualTo(3)
        assertThat(next.at).isEqualTo(ZonedDateTime.of(2026, 11, 17, 23, 30, 0, 0, seoul))
        assertThat(next.until).isEqualTo(TimeUntil(0, 0, 20))
    }

    @Test
    fun repeatingAlarm_usesNextMatchingDay() {
        val alarms = listOf(alarm(1, 6, 30, days = setOf(DayOfWeek.MONDAY)))
        val next = NextAlarm.find(alarms, now, noHolidays)!!
        assertThat(next.at.toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 23))
        assertThat(next.until).isEqualTo(TimeUntil(5, 7, 20))
    }

    @Test
    fun forAlarm_ignoresEnabledFlag_forEditPreview() {
        // 편집 화면 안내 글은 꺼진 알람이라도 저장하면 켜지니 울릴 시각을 보여 줘요.
        val at = NextAlarm.ringTimeIfSaved(alarm(1, 6, 30, enabled = false), now, noHolidays)
        assertThat(at).isEqualTo(ZonedDateTime.of(2026, 11, 18, 6, 30, 0, 0, seoul))
    }
}
