package com.lumian.morningup.core.alarm

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.receiver.SystemEventReceiver
import org.junit.Test

/** 폰을 다시 켠 뒤 이어가는 울림은 화면을 맨 앞으로 띄우는 신호를 달아요(TSK-255). */
class BootResumeTest {
    @Test
    fun bootSignals_areMarkedAsAfterBoot_andAreAlsoRescheduleSignals() {
        assertThat(SystemEventReceiver.BOOT_ACTIONS)
            .containsExactly(Intent.ACTION_LOCKED_BOOT_COMPLETED, Intent.ACTION_BOOT_COMPLETED)
        assertThat(SystemEventReceiver.ACTIONS).containsAtLeastElementsIn(SystemEventReceiver.BOOT_ACTIONS)
    }

    @Test
    fun appUpdateAndTimeChange_areNotAfterBoot() {
        assertThat(SystemEventReceiver.BOOT_ACTIONS).doesNotContain(Intent.ACTION_MY_PACKAGE_REPLACED)
        assertThat(SystemEventReceiver.BOOT_ACTIONS).doesNotContain(Intent.ACTION_TIME_CHANGED)
    }
}
