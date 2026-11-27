package com.lumian.morningup.core.model.mission

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Shake Shake: 팔을 크게 흔들어야 세고, 0.25초 안 두 번은 한 번. */
class ShakeShakeTest {
    private val g = ShakeCounter.GRAVITY

    @Test
    fun restingPhone_doesNotCount() {
        val counter = ShakeCounter()
        assertThat(counter.onSample(0f, 0f, g, 0L)).isFalse()
    }

    @Test
    fun wristFlick_belowThreshold_doesNotCount() {
        val counter = ShakeCounter()
        val flick = g * (ShakeCounter.THRESHOLD_G - 0.3f)
        assertThat(counter.onSample(flick, 0f, 0f, 0L)).isFalse()
    }

    @Test
    fun bigSwing_counts() {
        val counter = ShakeCounter()
        val swing = g * (ShakeCounter.THRESHOLD_G + 0.2f)
        assertThat(counter.onSample(0f, swing, 0f, 0L)).isTrue()
    }

    @Test
    fun twoPeaksWithinQuarterSecond_countOnce() {
        val counter = ShakeCounter()
        val swing = g * (ShakeCounter.THRESHOLD_G + 1f)
        assertThat(counter.onSample(swing, 0f, 0f, 1_000L)).isTrue()
        assertThat(counter.onSample(-swing, 0f, 0f, 1_200L)).isFalse()
        assertThat(counter.onSample(swing, 0f, 0f, 1_249L)).isFalse()
        assertThat(counter.onSample(swing, 0f, 0f, 1_250L)).isTrue()
    }

    @Test
    fun reset_forgetsLastShake() {
        val counter = ShakeCounter()
        val swing = g * (ShakeCounter.THRESHOLD_G + 1f)
        counter.onSample(swing, 0f, 0f, 1_000L)
        counter.reset()
        assertThat(counter.onSample(swing, 0f, 0f, 1_010L)).isTrue()
    }

    @Test
    fun game_countsDown_toZero_thenFinished() {
        var s = ShakeShake.start(10)
        assertThat(s.remaining).isEqualTo(10)
        repeat(9) { s = ShakeShake.shake(s) }
        assertThat(s.finished).isFalse()
        s = ShakeShake.shake(s)
        assertThat(s.remaining).isEqualTo(0)
        assertThat(s.finished).isTrue()
        assertThat(ShakeShake.shake(s).remaining).isEqualTo(0)
    }

    @Test
    fun game_targetClampedToRange() {
        assertThat(ShakeShake.start(0).target).isEqualTo(1)
        assertThat(ShakeShake.start(500).target).isEqualTo(200)
    }
}
