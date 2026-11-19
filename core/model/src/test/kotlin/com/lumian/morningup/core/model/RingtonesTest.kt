package com.lumian.morningup.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** S5 알람음 목록 정의. */
class RingtonesTest {
    @Test
    fun defaultSound_isFirst() {
        assertThat(Ringtones.all.first().id).isEqualTo(Alarm.DEFAULT_SOUND_ID)
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
        assertThat(Ringtones.find("petits_rebonds").name).isEqualTo("말랑 통통")
        assertThat(Ringtones.find("morning").id).isEqualTo("petits_rebonds")
    }
}
