package com.lumian.morningup.core.alarm

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.Ringtones
import com.lumian.morningup.core.model.SleepTracks
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** TSK-273: 목록의 모든 id, key에 화면 이름 리소스가 있어요. */
@RunWith(RobolectricTestRunner::class)
class SoundNamesTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun everyIdHasName() {
        assertThat(SoundNames.ringtoneIds).containsExactlyElementsIn(Ringtones.all.map { it.id })
        assertThat(SoundNames.ringtoneGroupKeys).containsExactlyElementsIn(Ringtones.groups.map { it.key })
        assertThat(SoundNames.sleepTrackIds).containsExactlyElementsIn(SleepTracks.all.map { it.id })
        assertThat(SoundNames.sleepGroupKeys).containsExactlyElementsIn(SleepTracks.groups.map { it.key })
    }

    @Test
    fun koreanNames() {
        assertThat(context.getString(SoundNames.ringtone(Alarm.RANDOM_SOUND_ID))).isEqualTo("랜덤")
        assertThat(context.getString(SoundNames.ringtone("petits_rebonds"))).isEqualTo("말랑 통통")
        assertThat(context.getString(SoundNames.ringtone("deleted_sound"))).isEqualTo("기본 알람음")
        assertThat(context.getString(SoundNames.sleepGroup("hard_to_sleep"))).isEqualTo("잠에 들기 어려울 때")
    }
}
