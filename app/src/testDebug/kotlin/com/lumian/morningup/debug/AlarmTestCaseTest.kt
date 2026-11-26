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

    @Test
    fun silent_mutesEveryCase_andKeepsTheRest() {
        // 소리 없음(TSK-194)은 볼륨만 0이고, 벨소리, 진동, 미션 같은 나머지는 경우 설정 그대로예요.
        AlarmTestCase.entries.forEach { case ->
            val loud = alarm(case)
            val silent = AlarmTestCases.alarmFor(case, time, silent = true)
            assertEquals(0, silent.volumePercent)
            assertEquals(loud.copy(volumePercent = 0), silent)
            assertEquals(100, loud.volumePercent)
        }
    }

    @Test
    fun volume_setsRingVolume_beforeSilent_andClamps() {
        // TSK-250: `--ei volume N`은 알람 벨소리 볼륨(%)이고 silent보다 먼저예요.
        val quiet = AlarmTestCases.alarmFor(AlarmTestCase.A, time, silent = true, volume = 1)
        assertEquals(1, quiet.volumePercent)
        assertEquals(alarm(AlarmTestCase.A).copy(volumePercent = 1), quiet)
        assertEquals(100, AlarmTestCases.alarmFor(AlarmTestCase.A, time, volume = 150).volumePercent)
        assertEquals(0, AlarmTestCases.alarmFor(AlarmTestCase.A, time, volume = -5).volumePercent)
    }
}
