package com.lumian.morningup.core.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

/** TSK-212, 결정 174: 버튼 속이기 빠르기 1~10단계. */
class TrickSpeedTest {

    @Test
    fun oldKeys_moveToNearestLevel() {
        assertThat(TrickSpeed.of("slow").level).isEqualTo(2)
        assertThat(TrickSpeed.of("normal").level).isEqualTo(5)
        assertThat(TrickSpeed.of("fast").level).isEqualTo(9)
    }

    @Test
    fun levelKeys_roundTrip_andUnknownIsDefault() {
        (1..10).forEach { assertThat(TrickSpeed.of(TrickSpeed(it).key).level).isEqualTo(it) }
        assertThat(TrickSpeed.of(null)).isEqualTo(TrickSpeed.Default)
        assertThat(TrickSpeed.of("abc")).isEqualTo(TrickSpeed.Default)
        assertThat(TrickSpeed.of("15").level).isEqualTo(10)
    }

    @Test
    fun default_isFasterThanOldNormal_andLevelsGetFaster() {
        assertThat(TrickSpeed.Default.widthsPerSecond).isGreaterThan(0.7f)
        assertThat(LabSettings().trickSpeed).isEqualTo(TrickSpeed.Default)
        (1 until 10).forEach {
            assertThat(TrickSpeed(it + 1).widthsPerSecond).isGreaterThan(TrickSpeed(it).widthsPerSecond)
        }
        assertThat(TrickSpeed.Normal.widthsPerSecond).isWithin(0.001f).of(0.7f)
    }

    @Test
    fun ringJson_readsOldWordAndWritesLevel() {
        val old = Json.decodeFromString(ButtonTrick.serializer(), """{"speed":"fast"}""")
        assertThat(old.speed).isEqualTo(TrickSpeed.Fast)
        val text = Json.encodeToString(ButtonTrick.serializer(), ButtonTrick(TrickSpeed(7)))
        assertThat(Json.decodeFromString(ButtonTrick.serializer(), text).speed.level).isEqualTo(7)
    }
}
