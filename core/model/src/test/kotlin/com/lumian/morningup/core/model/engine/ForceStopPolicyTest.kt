package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ForceStopPolicyTest {
    @Test
    fun exitRecordWins_onAndroid11Up() {
        assertThat(ForceStopPolicy.detected(true, userStopSinceLastOpen = true, scheduleWiped = false)).isTrue()
        assertThat(ForceStopPolicy.detected(true, userStopSinceLastOpen = false, scheduleWiped = true)).isFalse()
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
