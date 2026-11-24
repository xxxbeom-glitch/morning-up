package com.lumian.morningup.feature.ring

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import kotlin.math.hypot
import kotlin.math.max
import kotlin.random.Random
import org.junit.Test

/** 버튼 속이기 자리 고르기(11-24 사용자 추가: 화면 어디든, 크기 40~112, 시각 글자와 화면 밖은 빼기). */
class TrickPlacerTest {
    private val area = Size(1_000f, 2_000f)
    private val avoid = Rect(200f, 700f, 800f, 1_000f)
    private val touchMin = 48f

    private fun place(seed: Int, count: Int = 3) =
        TrickPlacer.place(count, area, avoid, 40f, 112f, touchMin, 8f, Random(seed))

    @Test
    fun spots_stayInside_avoidText_andKeepApart() {
        repeat(300) { seed ->
            val spots = place(seed)
            assertThat(spots).hasSize(3)
            spots.forEach { spot ->
                val half = max(spot.size, touchMin) / 2
                assertThat(spot.size in 40f..112f).isTrue()
                assertThat(spot.center.x in half..area.width - half).isTrue()
                assertThat(spot.center.y in half..area.height - half).isTrue()
                assertThat(TrickPlacer.circleHitsRect(spot.center, half, avoid)).isFalse()
            }
            for (i in spots.indices) for (j in i + 1 until spots.size) {
                val a = spots[i]
                val b = spots[j]
                val apart = hypot(a.center.x - b.center.x, a.center.y - b.center.y)
                assertThat(apart).isAtLeast(max(a.size, touchMin) / 2 + max(b.size, touchMin) / 2 + 8f)
            }
        }
    }

    @Test
    fun spots_reachEveryPartOfTheScreen() {
        val all = (0 until 200).flatMap { place(it, count = 1) }
        // 위, 아래, 왼쪽, 오른쪽 끝 가까이도 가요(정해 둔 자리 6곳이 아니라 어디든).
        assertThat(all.any { it.center.y < 300f }).isTrue()
        assertThat(all.any { it.center.y > 1_700f }).isTrue()
        assertThat(all.any { it.center.x < 150f }).isTrue()
        assertThat(all.any { it.center.x > 850f }).isTrue()
        assertThat(all.any { it.size < 50f }).isTrue()
        assertThat(all.any { it.size > 100f }).isTrue()
    }
}
