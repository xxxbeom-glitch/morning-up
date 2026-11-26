package com.lumian.morningup.core.alarm

import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.ring.AlarmVolumeGuard
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 울리는 동안만 알람 볼륨 0을 최소로 올리고, 끈 뒤 돌려요(결정 143, C-14). */
@RunWith(RobolectricTestRunner::class)
class AlarmVolumeGuardTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val audio = context.getSystemService(AudioManager::class.java)
    private val events = mutableListOf<String>()
    private val guard = AlarmVolumeGuard(audio) { event, _, _ -> events += event }
    private val stream = AudioManager.STREAM_ALARM
    private val max get() = audio.getStreamMaxVolume(stream)
    private val volume get() = audio.getStreamVolume(stream)

    @Test
    fun zero_raisedWhileHeld_thenRestored() {
        audio.setStreamVolume(stream, 0, 0)
        guard.hold(7)
        assertThat(volume).isGreaterThan(0)
        assertThat(volume).isAtMost(max)
        guard.release()
        assertThat(volume).isEqualTo(0)
        assertThat(events).containsExactly(AlarmVolumeGuard.EVENT_RAISED, AlarmVolumeGuard.EVENT_RESTORED).inOrder()
    }

    @Test
    fun audible_untouched() {
        audio.setStreamVolume(stream, 3, 0)
        guard.hold(7)
        guard.release()
        assertThat(volume).isEqualTo(3)
        assertThat(events).isEmpty()
    }

    @Test
    fun userChangedWhileRinging_keepsUserValue() {
        audio.setStreamVolume(stream, 0, 0)
        guard.hold(7)
        audio.setStreamVolume(stream, max, 0)
        guard.release()
        assertThat(volume).isEqualTo(max)
        assertThat(events.last()).isEqualTo(AlarmVolumeGuard.EVENT_KEPT)
    }

    @Test
    fun twoHolders_restoreAfterBothRelease() {
        audio.setStreamVolume(stream, 0, 0)
        guard.hold(7)
        guard.hold(7)
        guard.release()
        assertThat(volume).isGreaterThan(0)
        guard.release()
        assertThat(volume).isEqualTo(0)
        guard.release()
        assertThat(volume).isEqualTo(0)
    }
}
