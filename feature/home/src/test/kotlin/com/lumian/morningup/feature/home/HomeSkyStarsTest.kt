package com.lumian.morningup.feature.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** TSK-211, 결정 173: 다크 모드 홈 별 무작위 반짝임. */
class HomeSkyStarsTest {

    @Test
    fun starCount_isWithinRange_andChangesWithRandom() {
        val counts = (0 until 50).map { randomStars(Random(it)).size }
        assertTrue(counts.all { it in 24..40 })
        assertTrue(counts.toSet().size > 1)
    }

    @Test
    fun starPositions_differBetweenOpens() {
        val a = randomStars(Random(1)).map { it.x to it.y }
        val b = randomStars(Random(2)).map { it.x to it.y }
        assertNotEquals(a, b)
    }

    @Test
    fun twinkle_goesFromZeroToFullAndBack() {
        val star = randomStars(Random(3)).first().copy(periodSeconds = 4, phase = 0f)
        assertEquals(0f, starTwinkleAlpha(star, 0f), 0.001f)
        assertEquals(1f, starTwinkleAlpha(star, 2f), 0.001f)
        assertEquals(0f, starTwinkleAlpha(star, 4f), 0.001f)
    }

    @Test
    fun twinkle_loopsWithoutJump() {
        // 시계가 60초에서 0초로 돌아가도 모든 별의 밝기가 그대로예요.
        randomStars(Random(4)).forEach { star ->
            assertEquals(starTwinkleAlpha(star, 0f), starTwinkleAlpha(star, 60f), 0.001f)
        }
    }

    @Test
    fun stars_startAtDifferentMoments() {
        val alphas = randomStars(Random(5)).map { starTwinkleAlpha(it, 0f) }
        assertTrue(alphas.toSet().size > 1)
    }
}
