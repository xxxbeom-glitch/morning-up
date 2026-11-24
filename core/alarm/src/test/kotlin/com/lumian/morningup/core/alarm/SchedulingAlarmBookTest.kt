package com.lumian.morningup.core.alarm

import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.data.AlarmRepository
import com.lumian.morningup.core.data.db.MorningDatabase
import com.lumian.morningup.core.data.diagnostics.DiagnosticLog
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.engine.AlarmRescheduler
import com.lumian.morningup.core.model.engine.CatchUpHandler
import com.lumian.morningup.core.model.engine.KoreanHolidays
import com.lumian.morningup.core.testing.FakeWakeCheckGate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
class SchedulingAlarmBookTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val shadow = shadowOf(alarmManager)
    private val direct = Executor { it.run() }
    private val db = Room.inMemoryDatabaseBuilder(context, MorningDatabase::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor(direct)
        .setTransactionExecutor(direct)
        .build()

    /** 2026-11-17(화) 23:10 서울. */
    private val start = ZonedDateTime.of(2026, 11, 17, 23, 10, 0, 0, ZoneId.of("Asia/Seoul"))
    private val now = NowProvider { start }
    private val diagnostics =
        AlarmDiagnostics(DiagnosticLog(db.diagnosticLogDao()), now, CoroutineScope(Dispatchers.Unconfined))
    private val exact = ExactAlarms(context, alarmManager, diagnostics)
    private val scheduler = AndroidAlarmScheduler(context, exact, diagnostics)
    private val repository = AlarmRepository(db.alarmDao())
    private val noCatchUp = object : CatchUpHandler {
        override fun ringNow(alarm: Alarm) = Unit

        override fun notifyMissed(alarm: Alarm) = Unit
    }
    private val checks = FakeWakeCheckGate()
    private val book = SchedulingAlarmBook(
        repository,
        AlarmRescheduler(repository, scheduler, noCatchUp, KoreanHolidays),
        scheduler,
        now,
        checks,
    )
    private val tomorrow0630 = ZonedDateTime.of(2026, 11, 18, 6, 30, 0, 0, start.zone).toInstant().toEpochMilli()

    @Before
    fun setUp() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        // 상태 표시줄 아이콘이 열 앱 첫 화면을 테스트 패키지에 등록해요.
        val main = ComponentName(context.packageName, "com.lumian.morningup.MainActivity")
        shadowOf(context.packageManager).apply {
            addActivityIfNotPresent(main)
            addIntentFilterForActivity(
                main,
                IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) },
            )
        }
    }

    @After
    fun tearDown() {
        db.close()
        ShadowAlarmManager.reset()
    }

    @Test
    fun save_newAlarm_getsId_andIsScheduled() = runTest {
        val saved = book.save(Alarm(time = LocalTime.of(6, 30)))
        assertThat(saved.id).isGreaterThan(0)
        assertThat(saved.nextTriggerAt).isEqualTo(tomorrow0630)
        assertThat(shadow.nextScheduledAlarm!!.triggerAtTime).isEqualTo(tomorrow0630)
        assertThat(book.alarms.first()).containsExactly(saved)
    }

    @Test
    fun statusBarIcon_opensEditOfThatAlarm() = runTest {
        val saved = book.save(Alarm(time = LocalTime.of(6, 30)))
        val show = shadow.nextScheduledAlarm!!.alarmClockInfo!!.showIntent
        val intent = shadowOf(show).savedIntent
        assertThat(intent.getLongExtra(AlarmIntents.EXTRA_EDIT_ALARM_ID, -1)).isEqualTo(saved.id)
        assertThat(intent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP).isNotEqualTo(0)
    }

    @Test
    fun save_editedTime_reschedules() = runTest {
        val saved = book.save(Alarm(time = LocalTime.of(6, 30)))
        book.save(saved.copy(time = LocalTime.of(23, 30)))
        assertThat(shadow.scheduledAlarms).hasSize(1)
        assertThat(shadow.nextScheduledAlarm!!.triggerAtTime)
            .isEqualTo(start.withHour(23).withMinute(30).toInstant().toEpochMilli())
    }

    @Test
    fun setEnabled_offCancels_onSchedulesAgain() = runTest {
        val saved = book.save(Alarm(time = LocalTime.of(6, 30)))
        book.setEnabled(saved.id, false)
        assertThat(shadow.scheduledAlarms).isEmpty()
        assertThat(book.get(saved.id)!!.enabled).isFalse()

        book.setEnabled(saved.id, true)
        assertThat(shadow.nextScheduledAlarm!!.triggerAtTime).isEqualTo(tomorrow0630)
        assertThat(book.get(saved.id)!!.enabled).isTrue()
    }

    @Test
    fun delete_cancels_andRestoreBringsItBack() = runTest {
        val saved = book.save(Alarm(time = LocalTime.of(6, 30)))
        val deleted = book.delete(saved.id)!!
        assertThat(shadow.scheduledAlarms).isEmpty()
        assertThat(book.alarms.first()).isEmpty()

        book.restore(deleted)
        assertThat(book.get(saved.id)).isNotNull()
        assertThat(shadow.nextScheduledAlarm!!.triggerAtTime).isEqualTo(tomorrow0630)
    }

    @Test
    fun delete_missing_returnsNull() = runTest {
        assertThat(book.delete(99)).isNull()
    }

    @Test
    fun turningOffOrDeleting_stopsLeftChecks_turningOnDoesNot() = runTest {
        val saved = book.save(Alarm(time = LocalTime.of(6, 30)))
        book.setEnabled(saved.id, true)
        assertThat(checks.calls).isEmpty()
        book.setEnabled(saved.id, false)
        book.delete(saved.id)
        assertThat(checks.calls).containsExactly("cancel:${saved.id}", "cancel:${saved.id}").inOrder()
    }
}
