package com.lumian.morningup.core.designsystem.motion

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MorningMotionTest {
    @Test
    fun tokens_matchDevPlan() {
        assertThat(MorningMotion.SpatialFast).isEqualTo(SpringToken(0.6f, 1400f))
        assertThat(MorningMotion.SpatialDefault).isEqualTo(SpringToken(0.8f, 700f))
        assertThat(MorningMotion.SpatialSlow).isEqualTo(SpringToken(0.85f, 300f))
        assertThat(MorningMotion.EffectsFast).isEqualTo(SpringToken(1f, 1600f))
        assertThat(MorningMotion.EffectsDefault).isEqualTo(SpringToken(1f, 700f))
    }

    @Test
    fun spatial_isSpring_normally() {
        val spec = MorningMotion.spatial<Float>(MorningMotion.SpatialDefault, reduceMotion = false)
        assertThat(spec).isInstanceOf(SpringSpec::class.java)
        spec as SpringSpec<Float>
        assertThat(spec.dampingRatio).isEqualTo(0.8f)
        assertThat(spec.stiffness).isEqualTo(700f)
    }

    @Test
    fun spatial_isShortTween_whenReduceMotion() {
        val spec = MorningMotion.spatial<Float>(MorningMotion.SpatialSlow, reduceMotion = true)
        assertThat(spec).isInstanceOf(TweenSpec::class.java)
        assertThat((spec as TweenSpec<Float>).durationMillis).isEqualTo(150)
    }

    @Test
    fun effects_neverBounce() {
        val spec = MorningMotion.effects<Float>(MorningMotion.EffectsFast) as SpringSpec<Float>
        assertThat(spec.dampingRatio).isEqualTo(1f)
    }

    @Test
    fun pressScale_is096() {
        assertThat(MorningMotion.PRESS_SCALE).isEqualTo(0.96f)
    }
}
