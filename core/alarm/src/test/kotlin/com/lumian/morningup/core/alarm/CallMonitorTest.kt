package com.lumian.morningup.core.alarm

import android.content.Context
import android.media.AudioManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.ring.CallMonitor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 전화 권한 없이 소리 장치 모드로 통화를 알아채요. */
@RunWith(RobolectricTestRunner::class)
class CallMonitorTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val audio = context.getSystemService(AudioManager::class.java)

    @Test
    fun callModes() {
        assertThat(CallMonitor.isCallMode(AudioManager.MODE_IN_CALL)).isTrue()
        assertThat(CallMonitor.isCallMode(AudioManager.MODE_IN_COMMUNICATION)).isTrue()
        assertThat(CallMonitor.isCallMode(AudioManager.MODE_RINGTONE)).isTrue()
        assertThat(CallMonitor.isCallMode(AudioManager.MODE_NORMAL)).isFalse()
    }

    @Test
    fun inCall_followsAudioMode() {
        val monitor = CallMonitor(context)
        audio.mode = AudioManager.MODE_NORMAL
        assertThat(monitor.inCall()).isFalse()
        audio.mode = AudioManager.MODE_IN_CALL
        assertThat(monitor.inCall()).isTrue()
        audio.mode = AudioManager.MODE_NORMAL
        assertThat(monitor.inCall()).isFalse()
    }
}
