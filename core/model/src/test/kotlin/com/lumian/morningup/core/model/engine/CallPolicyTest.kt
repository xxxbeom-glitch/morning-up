package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CallPolicyTest {
    @Test
    fun notInCall_keepsRequested() {
        val requested = SoundOutput(0.8f, vibrate = false)
        assertThat(CallPolicy.output(requested, inCall = false)).isEqualTo(requested)
    }

    @Test
    fun inCall_quietAndVibrate() {
        assertThat(CallPolicy.output(SoundOutput(1f, vibrate = false), inCall = true))
            .isEqualTo(SoundOutput(CallPolicy.CALL_VOLUME, vibrate = true))
    }

    @Test
    fun inCall_neverLouderThanRequested() {
        assertThat(CallPolicy.output(SoundOutput(0.05f, vibrate = true), inCall = true).volume).isEqualTo(0.05f)
    }

    @Test
    fun callEnds_backToRequested() {
        val requested = SoundOutput(0.2f, vibrate = false)
        CallPolicy.output(requested, inCall = true)
        assertThat(CallPolicy.output(requested, inCall = false)).isEqualTo(requested)
    }
}
