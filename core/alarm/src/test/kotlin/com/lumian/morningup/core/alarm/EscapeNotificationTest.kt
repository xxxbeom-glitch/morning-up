package com.lumian.morningup.core.alarm

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.ring.MissionEscapeStore
import com.lumian.morningup.core.alarm.ring.RingNotifications
import com.lumian.morningup.core.data.db.MorningDatabase
import com.lumian.morningup.core.data.diagnostics.DiagnosticLog
import com.lumian.morningup.core.model.engine.RingPhase
import java.time.ZonedDateTime
import java.util.concurrent.Executor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * 미션 비상구는 따로 알림이 아니라 지금 울림 알림 하나에 붙어요(ISS-042). 앱 알림이 둘 이상이면
 * 안드로이드(삼성 One UI 포함)가 그룹 키와 상관없이 묶어 접어서 예문과 버튼이 안 보였어요.
 */
@RunWith(RobolectricTestRunner::class)
class EscapeNotificationTest {
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
    private val store = MissionEscapeStore(context)
    private val notifications = RingNotifications(context, { Intent() }, diagnostics, store)
    private val manager = context.getSystemService(NotificationManager::class.java)

    @After
    fun close() = db.close()

    private fun active(): List<Notification> = shadowOf(manager).allNotifications

    @Test
    fun missionNotification_carriesSentenceAndReply_whenEscapeOpen() {
        store.close()
        notifications.showRinging(RingPhase.InMission)
        assertThat(active().single().actions).isNull()

        store.openFresh()
        assertThat(notifications.refreshEscape()).isTrue()
        val posted = active().single()
        assertThat(posted.channelId).isEqualTo(RingNotifications.CHANNEL_ESCAPE)
        assertThat(posted.group).isNull()
        val action = posted.actions.single()
        assertThat(action.title.toString()).isEqualTo("미션 포기")
        assertThat(action.remoteInputs).hasLength(1)
        val big = posted.extras.getCharSequence(NotificationCompat.EXTRA_BIG_TEXT).toString()
        assertThat(big).contains(store.current()!!.sentence)
    }

    @Test
    fun restingNotification_carriesEscape_andCloseRemovesIt() {
        store.openFresh()
        notifications.showRinging(RingPhase.Resting, phaseEndsAt = System.currentTimeMillis() + 60_000)
        assertThat(active().single().actions.single().title.toString()).isEqualTo("미션 포기")

        store.close()
        notifications.refreshEscape()
        assertThat(active().single().actions).isNull()
    }

    @Test
    fun refreshAfterRingingEnds_postsNothing() {
        store.openFresh()
        notifications.showRinging(RingPhase.InMission)
        notifications.cancelRinging()
        assertThat(notifications.refreshEscape()).isFalse()
        assertThat(active()).isEmpty()
        store.close()
    }
}
