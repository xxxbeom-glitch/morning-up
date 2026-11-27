package com.lumian.morningup.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** S5 알람음 목록 정의. */
class RingtonesTest {
    @Test
    fun randomFirst_thenDefault() {
        assertThat(Ringtones.all.take(2).map { it.id })
            .containsExactly(Alarm.RANDOM_SOUND_ID, Alarm.DEFAULT_SOUND_ID).inOrder()
    }

    @Test
    fun newAlarm_startsRandom() {
        assertThat(Alarm(time = java.time.LocalTime.of(7, 0)).soundId).isEqualTo(Alarm.RANDOM_SOUND_ID)
    }

    @Test
    fun randomPool_hasOnlyRealTones() {
        assertThat(Ringtones.randomPool).doesNotContain(Alarm.RANDOM_SOUND_ID)
        assertThat(Ringtones.randomPool).doesNotContain(Alarm.DEFAULT_SOUND_ID)
        assertThat(Ringtones.randomPool).hasSize(Ringtones.all.size - 2)
    }

    @Test
    fun ids_areUnique_andGroupsNotEmpty() {
        assertThat(Ringtones.all.map { it.id }).containsNoDuplicates()
        assertThat(Ringtones.groups.all { it.ringtones.isNotEmpty() }).isTrue()
    }

    @Test
    fun find_unknownId_fallsBackToDefault() {
        assertThat(Ringtones.find("deleted_sound").id).isEqualTo(Alarm.DEFAULT_SOUND_ID)
        assertThat(Ringtones.find(Alarm.SILENT_SOUND_ID).id).isEqualTo(Alarm.DEFAULT_SOUND_ID)
        assertThat(Ringtones.find("petits_rebonds").id).isEqualTo("petits_rebonds")
        assertThat(Ringtones.find("morning").id).isEqualTo("petits_rebonds")
    }
}
