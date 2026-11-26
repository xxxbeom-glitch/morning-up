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
    fun missionNotification_becomesEscapeDoor_whenEscapeOpen() {
        store.close()
        notifications.showRinging(RingPhase.InMission)
        assertThat(active().single().actions).isNull()

        store.openFresh()
        assertThat(notifications.refreshEscape()).isTrue()
        val posted = active().single()
        assertThat(posted.channelId).isEqualTo(RingNotifications.CHANNEL_ESCAPE)
        assertThat(posted.group).isNull()
        // 결정 171: 바로 답장 칸 없이, 누르면 M5 미션 포기 화면이 열려요.
        assertThat(posted.actions).isNull()
        assertThat(posted.extras.getCharSequence(NotificationCompat.EXTRA_TITLE).toString())
            .isEqualTo("미션을 못 하겠으면")
        assertThat(posted.extras.getCharSequence(NotificationCompat.EXTRA_TEXT).toString())
            .isEqualTo("여기를 누르고 문장을 따라 쓰면 미션 없이 알람이 꺼져요")
        val opened = shadowOf(posted.contentIntent).savedIntent
        assertThat(opened.getBooleanExtra(RingNotifications.EXTRA_OPEN_ESCAPE, false)).isTrue()
    }

    @Test
    fun restingNotification_carriesEscape_andCloseRemovesIt() {
        store.openFresh()
        notifications.showRinging(RingPhase.Resting, phaseEndsAt = System.currentTimeMillis() + 60_000)
        assertThat(active().single().channelId).isEqualTo(RingNotifications.CHANNEL_ESCAPE)

        store.close()
        notifications.refreshEscape()
        assertThat(active().single().channelId).isNotEqualTo(RingNotifications.CHANNEL_ESCAPE)
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
