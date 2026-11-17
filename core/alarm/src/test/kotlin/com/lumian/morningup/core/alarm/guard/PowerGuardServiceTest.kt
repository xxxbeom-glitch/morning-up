package com.lumian.morningup.core.alarm.guard

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PowerGuardServiceTest {
    @Test
    fun powerMenu_isRecognizedOnStockAndSamsung() {
        assertThat(
            PowerGuardService.isPowerMenu(
                "com.android.systemui",
                "com.android.systemui.globalactions.GlobalActionsDialog",
            ),
        ).isTrue()
        assertThat(PowerGuardService.isPowerMenu("com.samsung.android.globalactions", "android.app.Dialog")).isTrue()
    }

    @Test
    fun otherWindows_areLeftAlone() {
        assertThat(PowerGuardService.isPowerMenu("com.android.systemui", "android.app.StatusBar")).isFalse()
        assertThat(PowerGuardService.isPowerMenu("com.lumian.morningup", "MainActivity")).isFalse()
        assertThat(PowerGuardService.isPowerMenu(null, null)).isFalse()
    }
}
