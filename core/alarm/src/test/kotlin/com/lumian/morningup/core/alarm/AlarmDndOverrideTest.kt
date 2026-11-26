package com.lumian.morningup.core.alarm

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.ring.AlarmDndOverride
import com.lumian.morningup.core.alarm.ring.DndControl
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 울리는 동안만 방해 금지를 '알람만 허용'으로 바꾸고, 끈 뒤 돌려요(TSK-177 답 3). 켠 사람만 해요. */
@RunWith(RobolectricTestRunner::class)
class AlarmDndOverrideTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = context.getSharedPreferences("dnd_override_test", Context.MODE_PRIVATE)
    private val control = FakeDnd()
    private val events = mutableListOf<String>()
    private val override = AlarmDndOverride(prefs, control) { event, _ -> events += event }

    @Test
    fun off_byDefault_untouched() {
        override.hold()
        override.release()
        assertThat(override.enabled.value).isFalse()
        assertThat(control.now).isEqualTo(PRIORITY)
        assertThat(control.changes).isEmpty()
    }

    @Test
    fun on_silenced_alarmsOnlyWhileHeld_thenRestored() {
        override.setEnabled(true)
        override.hold()
        assertThat(control.now).isEqualTo(NotificationManager.INTERRUPTION_FILTER_ALARMS)
        override.release()
        assertThat(control.now).isEqualTo(PRIORITY)
        assertThat(events).containsExactly(AlarmDndOverride.EVENT_CHANGED, AlarmDndOverride.EVENT_RESTORED).inOrder()
    }

    @Test
    fun on_notSilenced_untouched() {
        override.setEnabled(true)
        control.silenced = false
        override.hold()
        override.release()
        assertThat(control.changes).isEmpty()
    }

    @Test
    fun on_noAccess_onlyRecorded() {
        override.setEnabled(true)
        control.access = false
        override.hold()
        override.release()
        assertThat(control.changes).isEmpty()
        assertThat(events).containsExactly(AlarmDndOverride.EVENT_NO_ACCESS)
    }

    @Test
    fun changedWhileRinging_keepsNewValue() {
        override.setEnabled(true)
        override.hold()
        control.now = NotificationManager.INTERRUPTION_FILTER_ALL
        override.release()
        assertThat(control.now).isEqualTo(NotificationManager.INTERRUPTION_FILTER_ALL)
        assertThat(events.last()).isEqualTo(AlarmDndOverride.EVENT_KEPT)
    }

    @Test
    fun twoHolders_restoreAfterBothRelease() {
        override.setEnabled(true)
        override.hold()
        override.hold()
        override.release()
        assertThat(control.now).isEqualTo(NotificationManager.INTERRUPTION_FILTER_ALARMS)
        override.release()
        assertThat(control.now).isEqualTo(PRIORITY)
    }

    @Test
    fun enabled_savedAcrossInstances() {
        override.setEnabled(true)
        assertThat(AlarmDndOverride(prefs, control) { _, _ -> }.enabled.value).isTrue()
    }

    private class FakeDnd : DndControl {
        var access = true
        var silenced = true
        var now = PRIORITY
        val changes = mutableListOf<Int>()

        override fun accessGranted() = access

        override fun silenced() = silenced

        override fun filter() = now

        override fun setFilter(filter: Int) {
            changes += filter
            now = filter
        }
    }

    private companion object {
        const val PRIORITY = NotificationManager.INTERRUPTION_FILTER_PRIORITY
    }
}
