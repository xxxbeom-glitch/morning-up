package com.lumian.morningup.core.alarm

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.ring.RingController
import com.lumian.morningup.core.alarm.ring.RingEffectRunner
import com.lumian.morningup.core.alarm.ring.RingScreen
import com.lumian.morningup.core.alarm.ring.RingService
import com.lumian.morningup.core.data.db.MorningDatabase
import com.lumian.morningup.core.data.diagnostics.DiagnosticLog
import com.lumian.morningup.core.data.session.RingSessionStore
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.RingAgain
import com.lumian.morningup.core.model.engine.AlarmStore
import com.lumian.morningup.core.model.engine.RingEffect
import com.lumian.morningup.core.model.engine.RingEvent
import com.lumian.morningup.core.model.engine.RingPhase
import com.lumian.morningup.core.model.engine.RingSession
import com.lumian.morningup.core.model.engine.TimerKind
import java.io.File
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
class AlarmWiringTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val shadow = shadowOf(alarmManager)
    private val direct = Executor { it.run() }
    private val db = Room.inMemoryDatabaseBuilder(context, MorningDatabase::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor(direct)
        .setTransactionExecutor(direct)
        .build()
    private val log = DiagnosticLog(db.diagnosticLogDao())
    private val start = ZonedDateTime.of(2026, 11, 17, 6, 0, 0, 0, ZoneId.of("Asia/Seoul"))
    private var clock = start.toInstant().toEpochMilli()
    private val now = NowProvider { ZonedDateTime.ofInstant(Instant.ofEpochMilli(clock), start.zone) }
    private val diagnostics = AlarmDiagnostics(log, now, CoroutineScope(Dispatchers.Unconfined))
    private val exact = ExactAlarms(context, alarmManager, diagnostics)

    @Before
    fun grantExact() {
        // Robolectric은 기본값이 '권한 없음'이에요. 알람 시계 앱은 USE_EXACT_ALARM으로 처음부터 받아요.
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    @After
    fun tearDown() {
        db.close()
        ShadowAlarmManager.reset()
    }

    private fun next() = checkNotNull(shadow.nextScheduledAlarm)

    private fun events() = runBlocking { log.entries().map { it.event } }

    // ---- 예약 ----

    @Test
    fun schedule_usesAlarmClock_andCancelRemovesIt() {
        val scheduler = AndroidAlarmScheduler(context, exact, diagnostics)
        scheduler.schedule(alarmId = 7, triggerAt = clock + 60_000)

        val next = next()
        assertThat(next.triggerAtTime).isEqualTo(clock + 60_000)
        assertThat(next.alarmClockInfo).isNotNull()
        assertThat(events()).contains("scheduled")

        scheduler.cancel(7)
        assertThat(shadow.scheduledAlarms).isEmpty()
    }

    @Test
    fun scheduleSameAlarmTwice_keepsOneEntry() {
        val scheduler = AndroidAlarmScheduler(context, exact, diagnostics)
        scheduler.schedule(7, clock + 60_000)
        scheduler.schedule(7, clock + 120_000)
        assertThat(shadow.scheduledAlarms).hasSize(1)
        assertThat(next().triggerAtTime).isEqualTo(clock + 120_000)
    }

    @Test
    fun noExactPermission_fallsBackAndRecords() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        AndroidAlarmScheduler(context, exact, diagnostics).schedule(7, clock + 60_000)

        val next = next()
        assertThat(next.triggerAtTime).isEqualTo(clock + 60_000)
        assertThat(next.alarmClockInfo).isNull()
        assertThat(events()).contains("fallback_schedule")
    }

    @Test
    fun ringTimers_holdOneSlot_andWakeUpKindsUseAlarmClock() {
        val timers = RingTimers(context, exact)
        timers.schedule(clock + 59_000, TimerKind.MissionTimeout)
        assertThat(next().alarmClockInfo).isNull()

        timers.schedule(clock + 300_000, TimerKind.RestEnded)
        assertThat(shadow.scheduledAlarms).hasSize(1)
        assertThat(next().alarmClockInfo).isNotNull()

        timers.cancel()
        assertThat(shadow.scheduledAlarms).isEmpty()
    }

    @Test
    fun timerAndAlarm_doNotOverwriteEachOther() {
        AndroidAlarmScheduler(context, exact, diagnostics).schedule(1, clock + 600_000)
        RingTimers(context, exact).schedule(clock + 60_000, TimerKind.RingTimeout)
        assertThat(shadow.scheduledAlarms).hasSize(2)
    }

    // ---- Intent ----

    @Test
    fun timerKind_andUserEvents_roundTripThroughIntents() {
        TimerKind.entries.forEach { kind ->
            val intent = Intent().putExtra(AlarmIntents.EXTRA_TIMER_KIND, kind.name)
            assertThat(AlarmIntents.timerKindOf(intent)).isEqualTo(kind)
        }
        assertThat(AlarmIntents.timerKindOf(Intent().putExtra(AlarmIntents.EXTRA_TIMER_KIND, "Old"))).isNull()
        RingEvent.userEvents.forEach { event ->
            assertThat(RingService.eventOf(RingService.eventIntent(context, event))).isEqualTo(event)
        }
        assertThat(RingService.eventOf(Intent().putExtra("event", "MissionCompleted"))).isNull()
    }

    // ---- 울림 흐름 ----

    private class FakeAlarms(vararg alarms: Alarm) : AlarmStore {
        val map = alarms.associateBy { it.id }.toMutableMap()

        override suspend fun all() = map.values.toList()

        override suspend fun get(id: Long) = map[id]

        override suspend fun save(alarm: Alarm) {
            map[alarm.id] = alarm
        }
    }

    private class RecordingRunner : RingEffectRunner {
        val effects = mutableListOf<RingEffect>()

        override suspend fun run(session: RingSession?, effects: List<RingEffect>) {
            this.effects += effects
        }
    }

    private fun TestScope.sessionStore(file: File) =
        RingSessionStore(PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file }))

    private val mathAlarm = Alarm(id = 3, time = LocalTime.of(6, 0), missions = listOf(MissionType.QuickMath))

    @Test
    fun fire_mission_complete_endsWithWakeUpScreen() = runTest {
        val sessions = sessionStore(tmp.newFile("a.preferences_pb").also { it.delete() })
        val runner = RecordingRunner()
        val controller = RingController(sessions, FakeAlarms(mathAlarm), runner, now)

        assertThat(controller.fire(3)).isTrue()
        assertThat(sessions.load()?.phase).isEqualTo(RingPhase.Ringing)
        assertThat(controller.screen.value).isEqualTo(RingScreen.Ringing)
        assertThat(runner.effects).contains(RingEffect.RescheduleAlarm(3))

        clock += 5_000
        controller.send(RingEvent.MissionStarted)
        assertThat(controller.screen.value).isEqualTo(RingScreen.Mission(0))

        controller.send(RingEvent.MissionCompleted)
        assertThat(sessions.load()).isNull()
        assertThat(controller.session.value).isNull()
        assertThat(controller.screen.value).isEqualTo(RingScreen.WakeUp)
        assertThat(runner.effects.last()).isInstanceOf(RingEffect.SessionEnded::class.java)

        controller.clearScreen()
        assertThat(controller.screen.value).isEqualTo(RingScreen.None)
    }

    @Test
    fun fire_deletedAlarm_doesNothing() = runTest {
        val sessions = sessionStore(tmp.newFile("b.preferences_pb").also { it.delete() })
        val runner = RecordingRunner()
        val controller = RingController(sessions, FakeAlarms(), runner, now)
        assertThat(controller.fire(99)).isFalse()
        assertThat(sessions.load()).isNull()
        assertThat(runner.effects).isEmpty()
    }

    @Test
    fun newController_resumesSavedSession_withSoundScreenAndTimer() = runTest {
        val file = tmp.newFile("c.preferences_pb").also { it.delete() }
        val sessions = sessionStore(file)
        RingController(sessions, FakeAlarms(mathAlarm), RecordingRunner(), now).fire(3)

        // 앱이 죽었다 살아난 것처럼 새 컨트롤러로 이어가요.
        val runner = RecordingRunner()
        val revived = RingController(sessions, FakeAlarms(mathAlarm), runner, now)
        assertThat(revived.resume()?.phase).isEqualTo(RingPhase.Ringing)
        assertThat(revived.screen.value).isEqualTo(RingScreen.Ringing)
        assertThat(runner.effects.filterIsInstance<RingEffect.PlaySound>()).isNotEmpty()
        assertThat(runner.effects.filterIsInstance<RingEffect.ScheduleTimer>().single().kind)
            .isEqualTo(TimerKind.RingTimeout)
    }

    @Test
    fun resume_staleSession_endsAsMissed_insteadOfRinging() = runTest {
        val sessions = sessionStore(tmp.newFile("e.preferences_pb").also { it.delete() })
        RingController(sessions, FakeAlarms(mathAlarm), RecordingRunner(), now).fire(3)

        // 울리던 중 폰이 꺼졌다가 한참 뒤(밤)에 켜졌어요.
        clock += 15 * 60_000L
        val runner = RecordingRunner()
        val revived = RingController(sessions, FakeAlarms(mathAlarm), runner, now)
        assertThat(revived.resume()).isNull()
        assertThat(sessions.load()).isNull()
        assertThat(runner.effects).contains(RingEffect.PostMissedNotification(3))
        assertThat(runner.effects.filterIsInstance<RingEffect.PlaySound>()).isEmpty()
    }

    @Test
    fun resume_recentSession_keepsRinging() = runTest {
        val sessions = sessionStore(tmp.newFile("f.preferences_pb").also { it.delete() })
        RingController(sessions, FakeAlarms(mathAlarm), RecordingRunner(), now).fire(3)
        clock += 5 * 60_000L
        val revived = RingController(sessions, FakeAlarms(mathAlarm), RecordingRunner(), now)
        assertThat(revived.resume()?.phase).isEqualTo(RingPhase.Ringing)
    }

    @Test
    fun ringTimeout_withNoRingsLeft_closesScreen() = runTest {
        val sessions = sessionStore(tmp.newFile("d.preferences_pb").also { it.delete() })
        val once = mathAlarm.copy(ringAgain = RingAgain(5, 0))
        val controller = RingController(sessions, FakeAlarms(once), RecordingRunner(), now)
        controller.fire(3)
        clock += 60_000
        controller.send(RingEvent.TimerFired(TimerKind.RingTimeout))
        assertThat(controller.session.value).isNull()
        assertThat(controller.screen.value).isEqualTo(RingScreen.None)
    }
}
