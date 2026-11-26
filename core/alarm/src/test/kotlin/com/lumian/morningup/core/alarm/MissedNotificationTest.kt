package com.lumian.morningup.core.alarm

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.ring.MissionEscapeStore
import com.lumian.morningup.core.alarm.ring.RingNotifications
import com.lumian.morningup.core.data.db.MorningDatabase
import com.lumian.morningup.core.data.diagnostics.DiagnosticLog
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** 놓친 알람, 강제 종료 알림은 누르면 앱 홈이 열리고, 울림 알림과 따로 묶여요(TSK-222). */
@RunWith(RobolectricTestRunner::class)
class MissedNotificationTest {
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
    private val notifications = RingNotifications(context, { Intent() }, diagnostics, MissionEscapeStore(context))
    private val manager = context.getSystemService(NotificationManager::class.java)

    @After
    fun close() = db.close()

    @Test
    fun missed_opensHome_inOwnGroup() {
        notifications.missed(1L, LocalTime.of(6, 30), RingNotifications.MissedReason.Ignored)
        val posted = shadowOf(manager).allNotifications.single()
        assertThat(posted.group).isEqualTo("missed")
        assertThat(posted.flags and android.app.Notification.FLAG_AUTO_CANCEL).isNotEqualTo(0)
    }

    @Test
    fun forceStopped_inOwnGroup() {
        notifications.forceStopped()
        assertThat(shadowOf(manager).allNotifications.single().group).isEqualTo("missed")
    }
}
