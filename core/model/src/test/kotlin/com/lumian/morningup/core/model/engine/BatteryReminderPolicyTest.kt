package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test

class BatteryReminderPolicyTest {
    private val zone = ZoneId.of("Asia/Seoul")

    private fun at(day: Int, hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, zone)

    private fun notify(
        nextAlarm: ZonedDateTime?,
        notifications: Boolean = true,
        exempt: Boolean = false,
        last: LocalDate? = null,
    ) = BatteryReminderPolicy.shouldNotify(at(19, 21), nextAlarm, notifications, exempt, last)

    @Test
    fun nextCheck_tonightBeforeNine_tomorrowAfter() {
        assertThat(BatteryReminderPolicy.nextCheckAt(at(19, 14))).isEqualTo(at(19, 21))
        assertThat(BatteryReminderPolicy.nextCheckAt(at(19, 21))).isEqualTo(at(20, 21))
        assertThat(BatteryReminderPolicy.nextCheckAt(at(19, 23))).isEqualTo(at(20, 21))
    }

    @Test
    fun notifies_whenLimitedAndAlarmTomorrow() {
        assertThat(notify(at(20, 7))).isTrue()
        assertThat(notify(at(20, 0, 30), last = LocalDate.of(2026, 11, 18))).isTrue()
    }

    @Test
    fun quiet_whenExemptOrNoNotificationsOrAlreadySent() {
        assertThat(notify(at(20, 7), exempt = true)).isFalse()
        assertThat(notify(at(20, 7), notifications = false)).isFalse()
        assertThat(notify(at(20, 7), last = LocalDate.of(2026, 11, 19))).isFalse()
    }

    @Test
    fun quiet_whenNoAlarmTomorrow() {
        assertThat(notify(null)).isFalse()
        assertThat(notify(at(19, 23))).isFalse()
        assertThat(notify(at(21, 7))).isFalse()
    }
}
