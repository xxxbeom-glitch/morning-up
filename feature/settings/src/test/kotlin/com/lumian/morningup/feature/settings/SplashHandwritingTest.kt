package com.lumian.morningup.feature.settings

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test

/** 결정 170: 스플래시 손글씨는 아이콘 8획을 그대로 쓰고, 약 3초 동안 고르지 않은 빠르기로 써요. */
class SplashHandwritingTest {
    @Test
    fun strokes_areTheLauncherIconStrokes() {
        // 시험은 모듈 폴더(feature/settings)에서 돌아요.
        val xml = File("../../app/src/main/res/drawable/ic_launcher_app_foreground.xml").readText()
        val icon = Regex("android:pathData=\"([^\"]+)\"").findAll(xml).map { it.groupValues[1] }.toList()
        assertThat(icon).hasSize(8)
        assertThat(SplashHandwriting.STROKES).containsExactlyElementsIn(icon).inOrder()
        assertThat(xml).contains("android:translateX=\"${SplashHandwriting.OFFSET.toInt()}\"")
        assertThat(xml).contains("android:strokeWidth=\"${SplashHandwriting.STROKE_WIDTH}\"")
    }

    @Test
    fun timeline_takesAboutThreeSeconds_withPausesBetweenStrokes() {
        val slots = SplashHandwriting.timeline(List(8) { 100f })
        assertThat(slots.first().start).isEqualTo(0f)
        assertThat(slots.last().end).isWithin(1f).of(SplashHandwriting.DRAW_MILLIS.toFloat())
        assertThat(SplashHandwriting.TOTAL_MILLIS).isEqualTo(3_500)
        // 획 사이마다 쉼이 있어요.
        slots.zipWithNext().forEach { (a, b) -> assertThat(b.start).isGreaterThan(a.end) }
        // 길이가 같아도 획마다 걸리는 시간이 달라요(고르지 않은 빠르기).
        assertThat(slots.map { it.end - it.start }.toSet().size).isGreaterThan(4)
    }

    @Test
    fun progress_isZeroBefore_andOneAfterEachStroke() {
        val slots = SplashHandwriting.timeline(List(8) { 100f })
        assertThat(SplashHandwriting.progress(slots, 3, slots[3].start - 1f)).isEqualTo(0f)
        assertThat(SplashHandwriting.progress(slots, 3, slots[3].end + 1f)).isEqualTo(1f)
        // 다 쓴 시각에는 모든 획이 다 그려져 있어요(움직임 줄이기도 이 값으로 바로 그려요).
        val done = SplashHandwriting.DRAW_MILLIS.toFloat()
        (0 until 8).forEach { assertThat(SplashHandwriting.progress(slots, it, done)).isEqualTo(1f) }
    }
}
