package com.lumian.morningup.core.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

/** TSK-228, 결정 181: 버튼 속이기 빠르기를 끝에서 끝까지 걸리는 ms로. */
class TrickSpeedTest {

    @Test
    fun default_is1250_sameAsOldLevel6() {
        assertThat(TrickSpeed.Default.millis).isEqualTo(1250)
        assertThat(TrickSpeed.fromOldLevel(6)).isEqualTo(TrickSpeed.Default)
        assertThat(LabSettings().trickSpeed).isEqualTo(TrickSpeed.Default)
        assertThat(TrickSpeed.Default.widthsPerSecond).isWithin(0.001f).of(0.8f)
    }

    @Test
    fun oldLevelsAndWords_moveToSameSpeedMillis() {
        assertThat(TrickSpeed.of("6").millis).isEqualTo(1250)
        assertThat(TrickSpeed.of("10").millis).isEqualTo(833)
        assertThat(TrickSpeed.of("1").millis).isEqualTo(3333)
        assertThat(TrickSpeed.of("normal").widthsPerSecond).isWithin(0.001f).of(0.7f)
        assertThat(TrickSpeed.of("slow").millis).isEqualTo(2500)
        assertThat(TrickSpeed.of("fast").millis).isEqualTo(909)
    }

    @Test
    fun millisKeys_roundTrip_outOfRangeClamped_unknownIsDefault() {
        assertThat(TrickSpeed.of(TrickSpeed(300).key).millis).isEqualTo(300)
        assertThat(TrickSpeed.of("20").millis).isEqualTo(TrickSpeed.MIN_MILLIS)
        assertThat(TrickSpeed.of("99999").millis).isEqualTo(TrickSpeed.MAX_MILLIS)
        assertThat(TrickSpeed.ofMillis(0).millis).isEqualTo(50)
        assertThat(TrickSpeed.ofMillis(6000).millis).isEqualTo(5000)
        assertThat(TrickSpeed.of(null)).isEqualTo(TrickSpeed.Default)
        assertThat(TrickSpeed.of("abc")).isEqualTo(TrickSpeed.Default)
    }

    @Test
    fun smallerMillis_isFaster() {
        assertThat(TrickSpeed(100).widthsPerSecond).isGreaterThan(TrickSpeed(1250).widthsPerSecond)
    }

    @Test
    fun ringJson_readsOldWordAndLevel_writesMillis() {
        val old = Json.decodeFromString(ButtonTrick.serializer(), """{"speed":"fast"}""")
        assertThat(old.speed).isEqualTo(TrickSpeed.Fast)
        val level = Json.decodeFromString(ButtonTrick.serializer(), """{"speed":"7"}""")
        assertThat(level.speed).isEqualTo(TrickSpeed.fromOldLevel(7))
        val text = Json.encodeToString(ButtonTrick.serializer(), ButtonTrick(TrickSpeed(120)))
        assertThat(Json.decodeFromString(ButtonTrick.serializer(), text).speed.millis).isEqualTo(120)
    }
}
