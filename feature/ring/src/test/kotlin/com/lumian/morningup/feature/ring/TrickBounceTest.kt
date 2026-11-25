package com.lumian.morningup.feature.ring

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.random.Random
import org.junit.Test

/** 공처럼 튕기기(결정 155): 같은 빠르기로 곧게 가다가 벽에서 반사, ±15도 틀기, 축에서 15도 이상 떨어지기. */
class TrickBounceTest {
    private val area = Size(1080f, 2200f)
    private val half = 60f
    private val speed = 0.7f * 1080f

    /** 수평에서 잰 각도(0~90도). */
    private fun axisAngle(v: Offset): Double = Math.toDegrees(atan2(abs(v.y), abs(v.x)).toDouble())

    @Test
    fun start_keepsSpeed_andAvoidsAxes() {
        repeat(500) { seed ->
            val b = TrickBounce.start(Offset(500f, 1000f), speed, Random(seed))
            assertThat(hypot(b.velocity.x, b.velocity.y)).isWithin(0.5f).of(speed)
            assertThat(axisAngle(b.velocity)).isAtLeast(TrickBounce.AXIS_GAP_DEGREES - 1e-3)
            assertThat(axisAngle(b.velocity)).isAtMost(90 - TrickBounce.AXIS_GAP_DEGREES + 1e-3)
        }
    }

    @Test
    fun start_goesEveryWay() {
        val ways = (0 until 200).map {
            val v = TrickBounce.start(Offset.Zero, speed, Random(it)).velocity
            (v.x > 0) to (v.y > 0)
        }.toSet()
        assertThat(ways).hasSize(4)
    }

    @Test
    fun step_withoutWall_goesStraight() {
        val b = Bounce(Offset(500f, 1000f), Offset(300f, -400f))
        val next = TrickBounce.step(b, 0.1f, area, half, Random(1))
        assertThat(next.center).isEqualTo(Offset(530f, 960f))
        assertThat(next.velocity).isEqualTo(b.velocity)
    }

    @Test
    fun step_reflectsOnRightWall_andTurnsAtMost15() {
        repeat(200) { seed ->
            val b = Bounce(Offset(area.width - half - 10f, 1000f), Offset(400f, 300f))
            val next = TrickBounce.step(b, 0.1f, area, half, Random(seed))
            // 오른쪽 벽에 닿아 x가 거울처럼 돌아오고 왼쪽으로 가요. 아래로 가던 건 그대로예요.
            assertThat(next.center.x).isWithin(0.01f).of(area.width - half - 30f)
            assertThat(next.velocity.x).isLessThan(0f)
            assertThat(next.velocity.y).isGreaterThan(0f)
            assertThat(hypot(next.velocity.x, next.velocity.y)).isWithin(0.5f).of(500f)
            val before = axisAngle(b.velocity)
            assertThat(abs(axisAngle(next.velocity) - before)).isAtMost(TrickBounce.JITTER_DEGREES + 1e-3)
        }
    }

    @Test
    fun step_reflectsInCorner_onBothAxes() {
        val b = Bounce(Offset(half + 5f, half + 5f), Offset(-300f, -300f))
        val next = TrickBounce.step(b, 0.1f, area, half, Random(3))
        assertThat(next.velocity.x).isGreaterThan(0f)
        assertThat(next.velocity.y).isGreaterThan(0f)
    }

    @Test
    fun step_nearAxis_isPushedOut() {
        // 거의 수평으로 가다 벽에 닿으면 틀어도 수평에서 15도 이상 떨어져요.
        repeat(200) { seed ->
            val b = Bounce(Offset(area.width - half - 1f, 1000f), Offset(500f, 5f))
            val next = TrickBounce.step(b, 0.05f, area, half, Random(seed))
            assertThat(axisAngle(next.velocity)).isAtLeast(TrickBounce.AXIS_GAP_DEGREES - 1e-3)
        }
    }

    @Test
    fun longRun_staysInside_andKeepsSpeed() {
        val random = Random(7)
        var b = TrickBounce.start(TrickBounce.randomCenter(area, half, random), speed, random)
        repeat(20 * 60) {
            b = TrickBounce.step(b, 1f / 60, area, half, random)
            assertThat(b.center.x).isIn(com.google.common.collect.Range.closed(half, area.width - half))
            assertThat(b.center.y).isIn(com.google.common.collect.Range.closed(half, area.height - half))
            assertThat(hypot(b.velocity.x, b.velocity.y)).isWithin(0.5f).of(speed)
        }
    }

    @Test
    fun randomCenter_staysInside_evenWhenAreaIsTiny() {
        repeat(100) {
            val c = TrickBounce.randomCenter(area, half, Random(it))
            assertThat(c.x).isAtLeast(half)
            assertThat(c.x).isAtMost(area.width - half)
        }
        val tiny = TrickBounce.randomCenter(Size(80f, 80f), half, Random(1))
        assertThat(tiny).isEqualTo(Offset(40f, 40f))
    }
}
