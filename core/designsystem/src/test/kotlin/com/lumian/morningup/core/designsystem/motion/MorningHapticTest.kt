package com.lumian.morningup.core.designsystem.motion

import android.view.HapticFeedbackConstants
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MorningHapticTest {
    @Test
    fun confirm_fallsBackToVirtualKey_beforeApi30() {
        assertThat(MorningHaptic.Confirm.toFeedbackConstant(29)).isEqualTo(HapticFeedbackConstants.VIRTUAL_KEY)
        assertThat(MorningHaptic.Confirm.toFeedbackConstant(30)).isEqualTo(HapticFeedbackConstants.CONFIRM)
    }

    @Test
    fun toggle_usesToggleConstants_fromApi34() {
        assertThat(MorningHaptic.ToggleOn.toFeedbackConstant(34)).isEqualTo(HapticFeedbackConstants.TOGGLE_ON)
        assertThat(MorningHaptic.ToggleOff.toFeedbackConstant(34)).isEqualTo(HapticFeedbackConstants.TOGGLE_OFF)
        assertThat(MorningHaptic.ToggleOn.toFeedbackConstant(33)).isEqualTo(HapticFeedbackConstants.VIRTUAL_KEY)
        assertThat(MorningHaptic.ToggleOff.toFeedbackConstant(33)).isEqualTo(HapticFeedbackConstants.CLOCK_TICK)
    }

    @Test
    fun reject_fallsBackToLongPress_beforeApi30() {
        assertThat(MorningHaptic.Reject.toFeedbackConstant(26)).isEqualTo(HapticFeedbackConstants.LONG_PRESS)
    }

    @Test
    fun segmentTick_usesFrequentTick_fromApi34() {
        assertThat(MorningHaptic.SegmentTick.toFeedbackConstant(34))
            .isEqualTo(HapticFeedbackConstants.SEGMENT_FREQUENT_TICK)
        assertThat(MorningHaptic.SegmentTick.toFeedbackConstant(33)).isEqualTo(HapticFeedbackConstants.CLOCK_TICK)
    }

    @Test
    fun keyTap_usesKeyboardTap() {
        assertThat(MorningHaptic.KeyTap.toFeedbackConstant(30)).isEqualTo(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}
