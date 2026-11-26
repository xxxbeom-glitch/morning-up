package com.lumian.morningup.core.model.engine

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

/**
 * 밤 9시 '내일 알람 점검'(결정 113, 119, TSK-177 답 5). 내일 알람이 제때 소리 나게 울리지 못할 문제를 모아
 * 하루 한 번 푸시 하나로 알려요. 제목은 [issues]의 첫 문제, 나머지는 '그 밖에 n개 더'예요(시안 N1). 문제가 없으면 보내지 않아요.
 * 알림 권한이 있고, 다음에 울릴 알람이 내일 날짜일 때만 보내요.
 */
object BatteryReminderPolicy {
    val CHECK_TIME: LocalTime = LocalTime.of(21, 0)

    /** 이 아래로 남고 충전 중이 아니면 알려요(TSK-177 B-04). */
    const val LOW_BATTERY_PERCENT = 20

    /** 다음 확인 시각: 오늘 밤 9시가 아직이면 오늘, 지났으면 내일 밤 9시. */
    fun nextCheckAt(now: ZonedDateTime): ZonedDateTime {
        val today = now.toLocalDate().atTime(CHECK_TIME).atZone(now.zone)
        return if (now.isBefore(today)) today else today.plusDays(1)
    }

    /**
     * 지금 걸린 문제, 급한 순서. 홈 배너 순서(TSK-177 답 7)를 따르고, 배터리 부족은 폰이 꺼지면 아예 못 울려서
     * 배터리 예외보다 앞에 둬요. [batteryPercent]가 null이면 배터리 양을 못 읽은 거예요.
     */
    fun issues(
        backgroundRestricted: Boolean,
        alarmsSilenced: Boolean,
        batteryPercent: Int?,
        charging: Boolean,
        batteryExempt: Boolean,
    ): List<TomorrowIssue> = buildList {
        if (backgroundRestricted) add(TomorrowIssue.BatteryRestricted)
        if (alarmsSilenced) add(TomorrowIssue.AlarmsSilenced)
        if (batteryPercent != null && batteryPercent < LOW_BATTERY_PERCENT && !charging) add(TomorrowIssue.LowBattery)
        if (!batteryExempt) add(TomorrowIssue.BatteryExempt)
    }

    fun shouldNotify(
        now: ZonedDateTime,
        nextAlarmAt: ZonedDateTime?,
        notificationsOn: Boolean,
        issues: List<TomorrowIssue>,
        lastNotified: LocalDate?,
    ): Boolean {
        val today = now.toLocalDate()
        return notificationsOn &&
            issues.isNotEmpty() &&
            lastNotified != today &&
            nextAlarmAt != null &&
            nextAlarmAt.withZoneSameInstant(now.zone).toLocalDate() == today.plusDays(1)
    }
}

/** 밤 9시 점검에서 찾는 문제(시안 N1). */
enum class TomorrowIssue {
    /** 앱 배터리 사용이 '제한'. 알람이 아예 안 울릴 수 있어요. */
    BatteryRestricted,

    /** 방해 금지가 알람 소리를 막는 중. 소리 없이 울릴 수 있어요. */
    AlarmsSilenced,

    /** 배터리가 [BatteryReminderPolicy.LOW_BATTERY_PERCENT] 아래이고 충전 중이 아님. */
    LowBattery,

    /** 배터리 사용 제한 해제(절전 예외)가 꺼짐. */
    BatteryExempt,
}
