package com.lumian.morningup.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** 잠들기 소리는 처음부터 종료 타이머가 걸려 밤새 재생되지 않아요(TSK-278). */
class SleepTimerDefaultTest {
    @Test
    fun defaultTimer_isOneOfTheSheetChoices_andNotOff() {
        assertThat(SleepTracks.DEFAULT_TIMER_MINUTES).isEqualTo(30)
        assertThat(SleepTracks.TIMER_MINUTES).contains(SleepTracks.DEFAULT_TIMER_MINUTES)
    }
}
