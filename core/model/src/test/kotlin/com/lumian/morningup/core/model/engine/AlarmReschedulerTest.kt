package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.Alarm
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.test.runTest
import org.junit.Test

class AlarmReschedulerTest {
    private val seoul = ZoneId.of("Asia/Seoul")
    private val newYork = ZoneId.of("America/New_York")
    private val everyDay = DayOfWeek.entries.toSet()

    private fun at(d: Int, h: Int, m: Int, zone: ZoneId = seoul) = ZonedDateTime.of(2026, 11, d, h, m, 0, 0, zone)
    private fun ms(z: ZonedDateTime) = z.toInstant().toEpochMilli()

    private class FakeStore(vararg alarms: Alarm) : AlarmStore {
        val alarms = alarms.associateBy { it.id }.toMutableMap()
        override suspend fun all(): List<Alarm> = alarms.values.toList()
        override suspend fun get(id: Long): Alarm? = alarms[id]
        override suspend fun save(alarm: Alarm) {
            alarms[alarm.id] = alarm
        }
    }

    private class FakeScheduler : AlarmScheduler {
        val scheduled = mutableMapOf<Long, Long>()
        val cancelled = mutableListOf<Long>()
        override fun schedule(alarmId: Long, triggerAt: Long) {
            scheduled[alarmId] = triggerAt
        }

        override fun cancel(alarmId: Long) {
            scheduled.remove(alarmId)
            cancelled += alarmId
        }
    }

    private class FakeCatchUp : CatchUpHandler {
        val rang = mutableListOf<Long>()
        val missed = mutableListOf<Long>()
        override fun ringNow(alarm: Alarm) {
            rang += alarm.id
        }

        override fun notifyMissed(alarm: Alarm) {
            missed += alarm.id
        }
    }

    private val scheduler = FakeScheduler()
    private val catchUp = FakeCatchUp()

    private fun rescheduler(store: FakeStore) = AlarmRescheduler(store, scheduler, catchUp, KoreanHolidays)

    private fun alarm(
        id: Long,
        h: Int,
        m: Int,
        days: Set<DayOfWeek> = everyDay,
        enabled: Boolean = true,
        next: Long? = null,
    ) =
        Alarm(id = id, time = LocalTime.of(h, m), repeatDays = days, enabled = enabled, nextTriggerAt = next)

    // 놓친 알람 판단 -----------------------------------------------------------

    @Test
    fun policy_within10Minutes_ringsNow() {
        assertThat(BootCatchUpPolicy.decide(triggerAt = 0, now = 10 * 60_000)).isEqualTo(CatchUp.RingNow)
    }

    @Test
    fun policy_over10Minutes_notifiesMissed() {
        assertThat(BootCatchUpPolicy.decide(triggerAt = 0, now = 10 * 60_000 + 1)).isEqualTo(CatchUp.NotifyMissed)
    }

    @Test
    fun policy_notYetDue_doesNothing() {
        assertThat(BootCatchUpPolicy.decide(triggerAt = 1_000, now = 999)).isEqualTo(CatchUp.None)
    }

    // 전체 다시 예약 ----------------------------------------------------------

    @Test
    fun rescheduleAll_schedulesEnabled_andSavesNextTrigger() = runTest {
        val store = FakeStore(alarm(1, 7, 0))
        rescheduler(store).rescheduleAll(at(17, 6, 0))
        assertThat(scheduler.scheduled[1]).isEqualTo(ms(at(17, 7, 0)))
        assertThat(store.alarms[1]!!.nextTriggerAt).isEqualTo(ms(at(17, 7, 0)))
    }

    @Test
    fun rescheduleAll_cancelsDisabled_andClearsNextTrigger() = runTest {
        val store = FakeStore(alarm(1, 7, 0, enabled = false, next = 123))
        rescheduler(store).rescheduleAll(at(17, 6, 0))
        assertThat(scheduler.cancelled).contains(1L)
        assertThat(store.alarms[1]!!.nextTriggerAt).isNull()
    }

    @Test
    fun bootJustAfterMissedTime_ringsNow_andSchedulesNext() = runTest {
        // 7:00 알람, 폰이 꺼져 있다 7:05에 켜짐
        val store = FakeStore(alarm(1, 7, 0, next = ms(at(17, 7, 0))))
        rescheduler(store).rescheduleAll(at(17, 7, 5))
        assertThat(catchUp.rang).containsExactly(1L)
        assertThat(scheduler.scheduled[1]).isEqualTo(ms(at(18, 7, 0)))
    }

    @Test
    fun bootLongAfterMissedTime_notifiesMissed() = runTest {
        val store = FakeStore(alarm(1, 7, 0, next = ms(at(17, 7, 0))))
        rescheduler(store).rescheduleAll(at(17, 9, 0))
        assertThat(catchUp.rang).isEmpty()
        assertThat(catchUp.missed).containsExactly(1L)
        assertThat(scheduler.scheduled[1]).isEqualTo(ms(at(18, 7, 0)))
    }

    @Test
    fun missedOneShot_isNotifiedThenTurnedOff() = runTest {
        val store = FakeStore(alarm(1, 7, 0, days = emptySet(), next = ms(at(17, 7, 0))))
        rescheduler(store).rescheduleAll(at(17, 9, 0))
        assertThat(catchUp.missed).containsExactly(1L)
        assertThat(store.alarms[1]!!.enabled).isFalse()
        assertThat(scheduler.scheduled).doesNotContainKey(1L)
    }

    @Test
    fun timeZoneChange_reschedulesToNewLocalTime() = runTest {
        val store = FakeStore(alarm(1, 7, 0, next = ms(at(18, 7, 0))))
        // 서울 17일 7:00 예약이 남아 있는데 뉴욕으로 바뀜. 뉴욕 시각 16일 17:00 → 뉴욕 17일 7:00
        val nowInNy = at(17, 6, 0).withZoneSameInstant(newYork)
        rescheduler(store).rescheduleAll(nowInNy)
        assertThat(scheduler.scheduled[1]).isEqualTo(ms(ZonedDateTime.of(2026, 11, 17, 7, 0, 0, 0, newYork)))
        assertThat(catchUp.rang).isEmpty()
    }

    @Test
    fun appUpdate_withFutureTrigger_keepsSameTime_noCatchUp() = runTest {
        val store = FakeStore(alarm(1, 7, 0, next = ms(at(18, 7, 0))))
        rescheduler(store).rescheduleAll(at(17, 23, 0))
        assertThat(scheduler.scheduled[1]).isEqualTo(ms(at(18, 7, 0)))
        assertThat(catchUp.rang + catchUp.missed).isEmpty()
    }

    // 울린 뒤 ---------------------------------------------------------------

    @Test
    fun afterFired_repeating_schedulesNextDay() = runTest {
        val store = FakeStore(alarm(1, 7, 0, next = ms(at(17, 7, 0))))
        rescheduler(store).afterFired(1, at(17, 7, 0))
        assertThat(scheduler.scheduled[1]).isEqualTo(ms(at(18, 7, 0)))
    }

    @Test
    fun afterFired_slightlyEarly_doesNotRescheduleSameTime() = runTest {
        val store = FakeStore(alarm(1, 7, 0, next = ms(at(17, 7, 0))))
        rescheduler(store).afterFired(1, at(17, 7, 0).minusNanos(5_000_000))
        assertThat(scheduler.scheduled[1]).isEqualTo(ms(at(18, 7, 0)))
    }

    @Test
    fun afterFired_oneShot_turnsOff() = runTest {
        val store = FakeStore(alarm(1, 7, 0, days = emptySet(), next = ms(at(17, 7, 0))))
        rescheduler(store).afterFired(1, at(17, 7, 0))
        assertThat(store.alarms[1]!!.enabled).isFalse()
        assertThat(store.alarms[1]!!.nextTriggerAt).isNull()
        assertThat(scheduler.cancelled).contains(1L)
    }

    @Test
    fun afterFired_skipOnceDateUsed_isCleared() = runTest {
        val skipped = alarm(1, 7, 0, next = ms(at(18, 7, 0))).copy(skipOnceDate = LocalDate.of(2026, 11, 17))
        val store = FakeStore(skipped)
        rescheduler(store).afterFired(1, at(18, 7, 0))
        assertThat(store.alarms[1]!!.skipOnceDate).isNull()
    }

    @Test
    fun afterFired_unknownAlarm_doesNothing() = runTest {
        rescheduler(FakeStore()).afterFired(42, at(17, 7, 0))
        assertThat(scheduler.scheduled).isEmpty()
    }
}
