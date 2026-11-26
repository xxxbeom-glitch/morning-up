package com.lumian.morningup.core.alarm

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.ring.AlarmDndOverride
import com.lumian.morningup.core.alarm.ring.DndControl
import org.junit.After
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

    @After
    fun clear() {
        prefs.edit().clear().commit()
    }
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

    @Test
    fun android15_userDndWins_untouched_andRecorded() {
        // ISS-057: 안드로이드 15부터는 앱 규칙이 사용자 방해 금지를 못 이겨서 바꾸지 않아요.
        override.setEnabled(true)
        control.canOverride = false
        override.hold()
        override.release()
        assertThat(control.changes).isEmpty()
        assertThat(events).containsExactly(AlarmDndOverride.EVENT_UNSUPPORTED)
        assertThat(override.supported).isFalse()
    }

    @Test
    fun killedWhileRinging_nextStartRestores() {
        override.setEnabled(true)
        override.hold()
        // 앱이 강제 종료돼 release가 오지 않았어요. 다음 시작에 새로 만들어 돌려요.
        val next = AlarmDndOverride(prefs, control) { event, _ -> events += event }
        next.recoverLeftover()
        assertThat(control.now).isEqualTo(PRIORITY)
        assertThat(events.last()).isEqualTo(AlarmDndOverride.EVENT_LEFTOVER)
        // 한 번 돌리면 다시 하지 않아요.
        control.changes.clear()
        next.recoverLeftover()
        assertThat(control.changes).isEmpty()
    }

    @Test
    fun android15_leftoverAppRule_turnedOffOnce() {
        // 예전 판이 남긴 앱 규칙은 시작 때 한 번 꺼요(앱 규칙만 꺼지고 사용자 방해 금지는 그대로).
        control.canOverride = false
        override.recoverLeftover()
        override.recoverLeftover()
        assertThat(control.changes).containsExactly(NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    @Test
    fun normalRelease_noLeftoverOnNextStart() {
        override.setEnabled(true)
        override.hold()
        override.release()
        control.changes.clear()
        AlarmDndOverride(prefs, control) { _, _ -> }.recoverLeftover()
        assertThat(control.changes).isEmpty()
    }

    private class FakeDnd : DndControl {
        var canOverride = true
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

        override fun canOverride() = canOverride
    }

    private companion object {
        const val PRIORITY = NotificationManager.INTERRUPTION_FILTER_PRIORITY
    }
}
