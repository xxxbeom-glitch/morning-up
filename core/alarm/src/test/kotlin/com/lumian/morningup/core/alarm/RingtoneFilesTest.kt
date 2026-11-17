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
        val listed = Ringtones.all.map { it.id }.filter { it != Alarm.DEFAULT_SOUND_ID }
        assertThat(RingtoneFiles.ids).containsExactlyElementsIn(listed)
        listed.forEach { assertThat(RingtoneFiles.rawFor(it)).isNotEqualTo(R.raw.alarm_default) }
    }

    @Test
    fun unknownOrSilent_usesDefault() {
        assertThat(RingtoneFiles.rawFor(null)).isEqualTo(R.raw.alarm_default)
        assertThat(RingtoneFiles.rawFor(Alarm.DEFAULT_SOUND_ID)).isEqualTo(R.raw.alarm_default)
        assertThat(RingtoneFiles.rawFor("deleted_sound")).isEqualTo(R.raw.alarm_default)
    }
}
