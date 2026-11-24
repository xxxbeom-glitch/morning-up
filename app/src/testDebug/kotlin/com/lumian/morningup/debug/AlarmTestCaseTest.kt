package com.lumian.morningup.debug

import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.MissionType
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 시험 알람 A~G가 docs/test-alarm-cases.md 표대로 만들어지는지 봐요. */
class AlarmTestCaseTest {
    private val time = LocalTime.of(7, 0)

    private fun alarm(case: AlarmTestCase): Alarm = AlarmTestCases.alarmFor(case, time)

    @Test
    fun a_isAllDefaults_ringOnceThenMissed() {
        val a = alarm(AlarmTestCase.A)
        assertTrue(a.missions.isEmpty())
        assertEquals(0, a.ringAgain.count)
        assertFalse(a.wakeCheck.enabled)
        assertTrue(a.vibrate)
        assertFalse(a.gentleWake)
        assertFalse(a.powerOffGuard)
    }

    @Test
    fun b_ringsAgainThreeTimesEveryThreeMinutes() {
        val b = alarm(AlarmTestCase.B)
        assertEquals(3, b.ringAgain.intervalMinutes)
        assertEquals(3, b.ringAgain.count)
    }

    @Test
    fun c_dropWordsThreeWithPowerGuard() {
        val c = alarm(AlarmTestCase.C)
        assertTrue(c.powerOffGuard)
        val mission = c.missions.single()
        assertEquals(MissionType.DropWords, mission.type)
        assertEquals(3, mission.normalized().count)
    }

    @Test
    fun d_wakeCheckOnWithGlobalTimes() {
        val d = alarm(AlarmTestCase.D)
        assertEquals(MissionType.QuickMath, d.missions.single().type)
        assertTrue(d.wakeCheck.enabled)
    }

    @Test
    fun e_noVibrationGentleThirtySeconds() {
        val e = alarm(AlarmTestCase.E)
        assertFalse(e.vibrate)
        assertTrue(e.gentleWake)
        assertEquals(30, e.gentleSeconds)
    }

    @Test
    fun f_vibrationOnly() {
        assertEquals(Alarm.SILENT_SOUND_ID, alarm(AlarmTestCase.F).soundId)
    }

    @Test
    fun g_ringsAgainUntilDismissed() {
        val g = alarm(AlarmTestCase.G)
        assertEquals(3, g.ringAgain.intervalMinutes)
        assertNull(g.ringAgain.count)
    }
}
