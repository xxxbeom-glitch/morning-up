package com.lumian.morningup.feature.mission

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test

class InteractionSignalTest {
    @Test
    fun firstPoke_sendsAtOnce_restAreBatchedPerWindow() = runTest {
        var sent = 0
        val signal = InteractionSignal(backgroundScope, windowMillis = 1_000) { sent++ }
        signal.poke()
        assertThat(sent).isEqualTo(1)
        repeat(5) { signal.poke() }
        assertThat(sent).isEqualTo(1)
        advanceTimeBy(1_001)
        assertThat(sent).isEqualTo(2)
        advanceTimeBy(5_000)
        assertThat(sent).isEqualTo(2)
        signal.poke()
        assertThat(sent).isEqualTo(3)
    }

    @Test
    fun cancel_dropsPending() = runTest {
        var sent = 0
        val signal = InteractionSignal(backgroundScope, windowMillis = 1_000) { sent++ }
        signal.poke()
        signal.poke()
        signal.cancel()
        advanceTimeBy(2_000)
        assertThat(sent).isEqualTo(1)
    }
}
