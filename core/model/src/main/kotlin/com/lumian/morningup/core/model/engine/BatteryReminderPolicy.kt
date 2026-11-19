package com.lumian.morningup.core.model.engine

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * 배터리 전날 밤 알림(결정 113, 119). 배터리 사용 제한 해제가 꺼져 있으면 알람 전날 밤 하루 한 번 알려요.
 * 알림 권한이 있고, 다음에 울릴 알람이 내일 날짜일 때만 보내요.
 * 보내는 시각 [CHECK_TIME]은 문서에 없어 추천안(밤 9시)이에요.
 */
object BatteryReminderPolicy {
    val CHECK_TIME: LocalTime = LocalTime.of(21, 0)

    /** 다음 확인 시각: 오늘 밤 9시가 아직이면 오늘, 지났으면 내일 밤 9시. */
    fun nextCheckAt(now: ZonedDateTime): ZonedDateTime {
        val today = now.toLocalDate().atTime(CHECK_TIME).atZone(now.zone)
        return if (now.isBefore(today)) today else today.plusDays(1)
    }

    fun shouldNotify(
        now: ZonedDateTime,
        nextAlarmAt: ZonedDateTime?,
        notificationsOn: Boolean,
        batteryExempt: Boolean,
        lastNotified: LocalDate?,
    ): Boolean {
        val today = now.toLocalDate()
        return notificationsOn &&
            !batteryExempt &&
            lastNotified != today &&
            nextAlarmAt != null &&
            nextAlarmAt.withZoneSameInstant(now.zone).toLocalDate() == today.plusDays(1)
    }
}
