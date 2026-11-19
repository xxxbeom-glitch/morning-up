package com.lumian.morningup.core.alarm

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.PowerManager
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.ring.RingNotifications
import com.lumian.morningup.core.data.db.MorningDatabase
import com.lumian.morningup.core.data.diagnostics.DiagnosticLog
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionReport
import java.time.ZonedDateTime
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

/** 기기 설정을 바꾸고 다시 확인하면 권한 상태가 따라 바뀌어요. */
@RunWith(RobolectricTestRunner::class)
class AndroidPermissionCheckerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val direct = Executor { it.run() }
    private val db = Room.inMemoryDatabaseBuilder(context, MorningDatabase::class.java)
        .allowMainThreadQueries()
        .setQueryExecutor(direct)
        .setTransactionExecutor(direct)
        .build()
    private val diagnostics = AlarmDiagnostics(
        DiagnosticLog(db.diagnosticLogDao()),
        NowProvider { ZonedDateTime.now() },
        CoroutineScope(Dispatchers.Unconfined),
    )
    private val exact = ExactAlarms(context, context.getSystemService(AlarmManager::class.java), diagnostics)
    private val notifications = RingNotifications(context, { Intent() }, diagnostics)
    private val checker = AndroidPermissionChecker(context, exact, notifications, diagnostics)
    private val power = shadowOf(context.getSystemService(PowerManager::class.java))
    private val audio = context.getSystemService(AudioManager::class.java)

    @Before
    fun allOn() {
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        power.setIgnoringBatteryOptimizations(context.packageName, true)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, 5, 0)
    }

    @After
    fun close() = db.close()

    @Test
    fun beforeRefresh_isUnknown() {
        assertThat(checker.report.value).isEqualTo(PermissionReport.Unknown)
    }

    @Test
    fun batteryOff_thenOn_followsDevice() {
        power.setIgnoringBatteryOptimizations(context.packageName, false)
        checker.refresh()
        assertThat(checker.report.value.isGranted(AlarmPermission.Battery)).isFalse()
        // 배터리는 선택 권한이라 필수 경고에는 안 잡혀요(결정 130).
        assertThat(checker.report.value.missingRequired).doesNotContain(AlarmPermission.Battery)

        power.setIgnoringBatteryOptimizations(context.packageName, true)
        checker.refresh()
        assertThat(checker.report.value.isGranted(AlarmPermission.Battery)).isTrue()
    }

    @Test
    fun exactAlarmOff_isReadButNotRequired() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        checker.refresh()
        assertThat(checker.report.value.isGranted(AlarmPermission.ExactAlarm)).isFalse()
        assertThat(checker.report.value.missingRequired).doesNotContain(AlarmPermission.ExactAlarm)
    }

    @Test
    fun alarmVolumeZero_isReadButNoWarning() {
        audio.setStreamVolume(AudioManager.STREAM_ALARM, 0, 0)
        checker.refresh()
        assertThat(checker.report.value.isGranted(AlarmPermission.AlarmVolume)).isFalse()
        assertThat(checker.report.value.missingRequired).doesNotContain(AlarmPermission.AlarmVolume)
    }
}
