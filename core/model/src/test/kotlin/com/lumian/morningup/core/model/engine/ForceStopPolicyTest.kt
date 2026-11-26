package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ForceStopPolicyTest {
    @Test
    fun android11Up_exitRecordOrWipedSchedule() {
        assertThat(ForceStopPolicy.detected(true, userStopSinceLastOpen = true, scheduleWiped = false)).isTrue()
        // ISS-054: 앱이 떠 있지 않을 때 강제 종료하면 닫힘 기록이 없어도 예약은 지워져요.
        assertThat(ForceStopPolicy.detected(true, userStopSinceLastOpen = false, scheduleWiped = true)).isTrue()
        assertThat(ForceStopPolicy.detected(true, userStopSinceLastOpen = false, scheduleWiped = false)).isFalse()
    }

    @Test
    fun wipedSchedule_onOlderAndroid() {
        assertThat(ForceStopPolicy.detected(true, userStopSinceLastOpen = null, scheduleWiped = true)).isTrue()
        assertThat(ForceStopPolicy.detected(true, userStopSinceLastOpen = null, scheduleWiped = false)).isFalse()
    }

    @Test
    fun quiet_withoutEnabledAlarm() {
        assertThat(ForceStopPolicy.detected(false, userStopSinceLastOpen = true, scheduleWiped = true)).isFalse()
    }
}
