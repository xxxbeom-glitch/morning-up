package com.lumian.morningup.core.model.engine

import com.lumian.morningup.core.model.Alarm
import java.time.ZonedDateTime

/** 다음에 울릴 알람 하나와 그 시각. */
data class NextAlarm(val alarm: Alarm, val at: ZonedDateTime, val until: TimeUntil) {
    companion object {
        /** 켜진 알람 중 가장 먼저 울릴 것. 없으면 null. 저장된 nextTriggerAt 대신 지금 다시 계산해요. */
        fun find(alarms: List<Alarm>, now: ZonedDateTime, holidays: HolidayCalendar): NextAlarm? = alarms
            .mapNotNull { alarm -> NextRingCalculator.next(alarm, now, holidays)?.let { alarm to it } }
            .minWithOrNull(compareBy<Pair<Alarm, ZonedDateTime>> { it.second.toInstant() }.thenBy { it.first.id })
            ?.let { (alarm, at) -> NextAlarm(alarm, at, TimeUntil.between(now, at)) }

        /** 저장하면 켜지므로, 꺼진 알람이어도 저장했을 때 울릴 시각을 돌려줘요(편집 안내 글, 저장 토스트). */
        fun ringTimeIfSaved(alarm: Alarm, now: ZonedDateTime, holidays: HolidayCalendar): ZonedDateTime? =
            NextRingCalculator.next(alarm.copy(enabled = true), now, holidays)
    }
}
