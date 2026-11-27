package com.lumian.morningup.core.alarm.guard

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.engine.RingPhase
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
        assertThat(
            PowerGuardService.isPowerMenu(
                "com.android.systemui",
                "com.android.systemui.globalactions.SecGlobalActionsDialog",
            ),
        ).isTrue()
        assertThat(PowerGuardService.isPowerMenu("com.android.systemui", "GlobalActionsDialogLite")).isTrue()
        assertThat(PowerGuardService.isPowerMenu("android", "com.android.internal.app.ShutdownDialog")).isTrue()
    }

    @Test
    fun guardsWhileAlarmIsRingingResting_andInMission_butNotDuringChecks() {
        assertThat(PowerGuardService.GUARDED_PHASES)
            .containsExactly(RingPhase.Ringing, RingPhase.Resting, RingPhase.InMission)
    }

    @Test
    fun otherWindows_areLeftAlone() {
        assertThat(PowerGuardService.isPowerMenu("com.android.systemui", "android.app.StatusBar")).isFalse()
        assertThat(PowerGuardService.isPowerMenu("com.lumian.morningup", "MainActivity")).isFalse()
        assertThat(PowerGuardService.isPowerMenu("com.lumian.morningup", "PowerMenuPreview")).isFalse()
        assertThat(PowerGuardService.isPowerMenu("com.android.systemui", "android.widget.FrameLayout")).isFalse()
        assertThat(PowerGuardService.isPowerMenu(null, null)).isFalse()
    }

    /** ISS-060: 삼성 전원 메뉴는 뒤로만으로 안 닫혀서 뒤로 → 홈 → 울림 화면 다시 맨 앞 순서로 닫아요. */
    @Test
    fun closeSteps_backThenHomeThenReopen_inTimeOrder() {
        val steps = PowerGuardService.CLOSE_STEPS
        assertThat(steps.map { it.action }).containsExactly(
            PowerGuardService.CloseAction.Back,
            PowerGuardService.CloseAction.Home,
            PowerGuardService.CloseAction.ReopenScreen,
        ).inOrder()
        assertThat(steps.map { it.delayMillis }).isInStrictOrder()
        assertThat(PowerGuardService.CLOSE_DEBOUNCE_MILLIS).isGreaterThan(steps.last().delayMillis)
    }

    @Test
    fun reopensRingScreen_onlyWhileRingingOrInMission() {
        assertThat(PowerGuardService.REOPEN_PHASES).containsExactly(RingPhase.Ringing, RingPhase.InMission)
    }
}
