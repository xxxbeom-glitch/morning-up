package com.lumian.morningup.core.model.engine

import com.lumian.morningup.core.model.Alarm
import java.time.Instant
import java.time.ZonedDateTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** 알람 저장소(Room이 구현해요). */
interface AlarmStore {
    suspend fun all(): List<Alarm>

    suspend fun get(id: Long): Alarm?

    suspend fun save(alarm: Alarm)
}

/** 운영체제 예약(AlarmManager가 구현해요). 같은 알람 ID로 다시 예약하면 앞의 것을 바꿔요. */
interface AlarmScheduler {
    fun schedule(alarmId: Long, triggerAt: Long)

    fun cancel(alarmId: Long)
}

/** 꺼져 있던 동안 놓친 알람을 바로 울리거나 알려요. */
interface CatchUpHandler {
    fun ringNow(alarm: Alarm)

    fun notifyMissed(alarm: Alarm)
}

enum class CatchUp { None, RingNow, NotifyMissed }

/** 폰이 꺼져 있다 켜졌을 때: 10분 안이면 바로 울리고, 넘으면 '놓친 알람' 알림만 남겨요(ADR 0002). */
object BootCatchUpPolicy {
    const val RING_NOW_WINDOW_MILLIS = 10 * 60_000L

    fun decide(triggerAt: Long, now: Long): CatchUp = when {
        now < triggerAt -> CatchUp.None
        now - triggerAt <= RING_NOW_WINDOW_MILLIS -> CatchUp.RingNow
        else -> CatchUp.NotifyMissed
    }
}

/**
 * 알람 예약을 맞추는 한 곳. 부팅, 시간 변경, 시간대 변경, 앱 업데이트, 권한 변경 신호는 모두 [rescheduleAll]을,
 * 알람이 울리면 [afterFired]를, 알람을 저장하면 [reschedule]을 불러요.
 * 여러 곳(앱 열기, 부팅 신호 두 개, 울림)에서 동시에 불려도 읽고 고치고 저장하는 일이 섞이지 않게 하나씩 처리해요.
 */
class AlarmRescheduler(
    private val store: AlarmStore,
    private val scheduler: AlarmScheduler,
    private val catchUp: CatchUpHandler,
    private val holidays: HolidayCalendar,
) {
    private val mutex = Mutex()

    suspend fun rescheduleAll(now: ZonedDateTime) = mutex.withLock {
        val nowMillis = now.toInstant().toEpochMilli()
        store.all().forEach { alarm ->
            val trigger = alarm.nextTriggerAt
            val decision = when {
                alarm.enabled && trigger != null -> BootCatchUpPolicy.decide(trigger, nowMillis)
                else -> CatchUp.None
            }
            when (decision) {
                CatchUp.None -> scheduleNext(alarm, now)
                CatchUp.RingNow -> {
                    catchUp.ringNow(alarm)
                    afterFired(alarm, now)
                }
                CatchUp.NotifyMissed -> {
                    catchUp.notifyMissed(alarm)
                    afterFired(alarm, now)
                }
            }
        }
    }

    suspend fun afterFired(alarmId: Long, now: ZonedDateTime) = mutex.withLock {
        store.get(alarmId)?.let { afterFired(it, now) }
    }

    /** 한 번 알람은 끄고, 반복 알람은 다음 시각을 예약해요. 일찍 깨어나도 같은 시각을 다시 잡지 않아요. */
    private suspend fun afterFired(alarm: Alarm, now: ZonedDateTime) {
        if (!alarm.isRepeating) {
            disable(alarm.copy(enabled = false))
            return
        }
        val firedAt = alarm.nextTriggerAt
        val nowMillis = now.toInstant().toEpochMilli()
        // 예약 시각보다 조금 일찍 깨어났을 때만 예약 시각을 기준으로 해요.
        // 그보다 먼 미래면 이미 다음 시각으로 다시 예약된 것이라 지금을 기준으로 같은 시각을 다시 계산해요.
        val earlyWake = firedAt != null && firedAt > nowMillis && firedAt - nowMillis <= EARLY_WAKE_TOLERANCE_MILLIS
        val base = if (earlyWake && firedAt != null) {
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(firedAt), now.zone)
        } else {
            now
        }
        val today = base.toLocalDate()
        val skip = alarm.skipOnceDate?.takeIf { !it.isBefore(today) }
        scheduleNext(alarm.copy(skipOnceDate = skip), base)
    }

    suspend fun reschedule(alarm: Alarm, now: ZonedDateTime) = mutex.withLock { scheduleNext(alarm, now) }

    private suspend fun scheduleNext(alarm: Alarm, now: ZonedDateTime) {
        val next = NextRingCalculator.next(alarm, now, holidays)
        if (next == null) {
            disable(alarm)
            return
        }
        val at = next.toInstant().toEpochMilli()
        scheduler.schedule(alarm.id, at)
        store.save(alarm.copy(nextTriggerAt = at))
    }

    private suspend fun disable(alarm: Alarm) {
        scheduler.cancel(alarm.id)
        store.save(alarm.copy(nextTriggerAt = null))
    }

    private companion object {
        const val EARLY_WAKE_TOLERANCE_MILLIS = 60_000L
    }
}
