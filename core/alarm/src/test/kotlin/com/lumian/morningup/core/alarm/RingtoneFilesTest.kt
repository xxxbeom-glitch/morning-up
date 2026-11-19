package com.lumian.morningup.core.alarm

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.ring.RingtoneFiles
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.Ringtones
import org.junit.Test

/** 목록의 모든 알람음에 소리 파일이 붙어 있고, 모르는 소리는 기본음이에요. */
class RingtoneFilesTest {
    @Test
    fun everyListedSound_hasFile() {
        val listed = Ringtones.randomPool
        assertThat(RingtoneFiles.ids).containsExactlyElementsIn(listed)
        listed.forEach { assertThat(RingtoneFiles.rawFor(it)).isNotEqualTo(R.raw.alarm_default) }
    }

    @Test
    fun unknownOrSilent_usesDefault() {
        assertThat(RingtoneFiles.rawFor(null)).isEqualTo(R.raw.alarm_default)
        assertThat(RingtoneFiles.rawFor(Alarm.DEFAULT_SOUND_ID)).isEqualTo(R.raw.alarm_default)
        assertThat(RingtoneFiles.rawFor("deleted_sound")).isEqualTo(R.raw.alarm_default)
    }

    /** '랜덤'은 기본음이 아닌 벨소리 중에서 고르고, 여러 번 부르면 여러 곡이 나와요. */
    @Test
    fun random_picksRealTones_andVaries() {
        val rng = kotlin.random.Random(SEED)
        val picks = List(PICKS) { RingtoneFiles.rawFor(Alarm.RANDOM_SOUND_ID, rng) }
        assertThat(picks).doesNotContain(R.raw.alarm_default)
        assertThat(picks.toSet().size).isGreaterThan(PICKS / 10)
    }

    private companion object {
        const val SEED = 7
        const val PICKS = 100
    }
}
