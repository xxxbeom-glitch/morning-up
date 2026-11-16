package com.lumian.morningup.core.model.engine

import com.lumian.morningup.core.model.Alarm
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * 다음 울릴 시각 계산(순수 함수).
 * - 지금과 같은 시각은 이미 지난 것으로 봐요(같은 알람이 두 번 울리지 않게).
 * - 시각은 그 시간대의 벽시계 기준이에요. 서머타임으로 없는 시각은 뒤로 밀고(2:30 → 3:30),
 *   두 번 생기는 시각은 앞의 것을 써요(`ZonedDateTime.of` 기본 동작).
 * - 공휴일 끄기와 '이번만 쉬기'는 반복 알람에만 적용해요.
 */
object NextRingCalculator {
    /** 공휴일과 이번만 쉬기가 겹쳐도 충분히 넘을 만큼 찾아봐요. */
    private const val SEARCH_DAYS = 400L

    fun next(alarm: Alarm, now: ZonedDateTime, holidays: HolidayCalendar): ZonedDateTime? {
        if (!alarm.enabled) return null
        val today = now.toLocalDate()
        for (offset in 0..SEARCH_DAYS) {
            val day = today.plusDays(offset)
            val candidate = ZonedDateTime.of(day, alarm.time, now.zone)
            if (!candidate.isAfter(now)) continue
            if (!alarm.isRepeating) return candidate
            if (rings(alarm, day, holidays)) return candidate
        }
        return null
    }

    private fun rings(alarm: Alarm, day: LocalDate, holidays: HolidayCalendar): Boolean =
        day.dayOfWeek in alarm.repeatDays &&
            !(alarm.skipHolidays && holidays.isHoliday(day)) &&
            day != alarm.skipOnceDate
}
