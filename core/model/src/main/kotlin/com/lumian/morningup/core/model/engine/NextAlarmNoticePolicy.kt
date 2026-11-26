package com.lumian.morningup.core.model.engine

import com.lumian.morningup.core.model.Alarm
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * '다음 알람 늘 보이기' 알림(N3, 결정 179)에 쓸 내용.
 * - [at]: 실제로 울릴 시각. 이번만 쉬기 중이면 쉬는 날을 건너뛴 다음 울림이에요.
 * - [repeatDays]: 반복 요일. 비어 있으면 한 번 알람이에요.
 * - [restedDate]: 이번만 쉬기로 쉬는 날. 그날이 [at]보다 앞에 있을 때만 넣어요('목요일은 쉬어요').
 */
data class NextAlarmNoticeContent(
    val at: ZonedDateTime,
    val repeatDays: Set<DayOfWeek>,
    val restedDate: LocalDate? = null,
)

object NextAlarmNoticePolicy {
    /** 켜진 알람 중 가장 먼저 울릴 것의 알림 내용. 켜진 알람이 없으면 null이라 알림을 지워요. */
    fun content(alarms: List<Alarm>, now: ZonedDateTime, holidays: HolidayCalendar): NextAlarmNoticeContent? {
        val next = NextAlarm.find(alarms, now, holidays) ?: return null
        val skip = next.alarm.skipOnceDate
        val rested = skip?.takeIf { !it.isBefore(now.toLocalDate()) && it.isBefore(next.at.toLocalDate()) }
        return NextAlarmNoticeContent(next.at, next.alarm.repeatDays, rested)
    }
}
