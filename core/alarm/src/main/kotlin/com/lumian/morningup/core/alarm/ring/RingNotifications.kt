package com.lumian.morningup.core.alarm.ring

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lumian.morningup.core.alarm.R
import com.lumian.morningup.core.model.engine.RingPhase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** 울림 화면(app의 RingActivity)을 여는 Intent. app 모듈이 채워 넣어요. */
fun interface RingScreenIntentFactory {
    fun create(context: Context): Intent
}

/**
 * 울리는 중 알림과 놓친 알람 알림. 소리와 진동은 [AlarmSoundPlayer]가 직접 내니 알림 통로는 조용하게 둬요.
 * 울리는 중 알림은 잠금 화면 위에 울림 화면을 띄우는 전체 화면 Intent를 달아요.
 */
@Singleton
class RingNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
    private val screenIntent: RingScreenIntentFactory,
) {
    private val manager = NotificationManagerCompat.from(context)

    fun ensureChannels() {
        val high = NotificationManager.IMPORTANCE_HIGH
        val ringing = NotificationChannel(CHANNEL_RINGING, text(R.string.channel_ringing), high)
            .apply {
                description = text(R.string.channel_ringing_description)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
        val normal = NotificationManager.IMPORTANCE_DEFAULT
        val missed = NotificationChannel(CHANNEL_MISSED, text(R.string.channel_missed), normal)
            .apply { description = text(R.string.channel_missed_description) }
        manager.createNotificationChannels(listOf(ringing, missed))
    }

    fun ringing(phase: RingPhase?): Notification {
        ensureChannels()
        val open = PendingIntent.getActivity(
            context,
            0,
            screenIntent.create(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val urgent = phase == RingPhase.Ringing || phase == RingPhase.WakeCheckAsking || phase == RingPhase.InMission
        return NotificationCompat.Builder(context, CHANNEL_RINGING)
            .setSmallIcon(R.drawable.ic_alarm_clock)
            .setContentTitle(text(R.string.ring_title))
            .setContentText(text(phaseText(phase)))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(open)
            .apply { if (urgent) setFullScreenIntent(open, true) }
            .build()
    }

    /** 쉬는 중이나 W2 기다림에는 서비스를 멈추고 알림만 남겨요. */
    @SuppressLint("MissingPermission")
    fun showRinging(phase: RingPhase?) {
        if (manager.areNotificationsEnabled()) manager.notify(RING_NOTIFICATION_ID, ringing(phase))
    }

    fun cancelRinging() = manager.cancel(RING_NOTIFICATION_ID)

    @SuppressLint("MissingPermission")
    fun missed(alarmId: Long, time: LocalTime, poweredOff: Boolean) {
        ensureChannels()
        if (!manager.areNotificationsEnabled()) return
        val label = time.format(DateTimeFormatter.ofPattern("H:mm"))
        val template = if (poweredOff) R.string.missed_text_powered_off else R.string.missed_text_ignored
        val body = context.getString(template, label)
        val notification = NotificationCompat.Builder(context, CHANNEL_MISSED)
            .setSmallIcon(R.drawable.ic_alarm_clock)
            .setContentTitle(text(R.string.missed_title))
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .build()
        manager.notify(MISSED_NOTIFICATION_BASE + alarmId.toInt(), notification)
    }

    private fun phaseText(phase: RingPhase?): Int = when (phase) {
        null, RingPhase.Ringing -> R.string.ring_text_ringing
        RingPhase.InMission -> R.string.ring_text_mission
        RingPhase.Resting -> R.string.ring_text_resting
        RingPhase.AwaitingWakeCheck -> R.string.ring_text_waiting
        RingPhase.WakeCheckAsking -> R.string.ring_text_wake_check
    }

    private fun text(id: Int) = context.getString(id)

    companion object {
        const val CHANNEL_RINGING = "ringing"
        const val CHANNEL_MISSED = "missed"
        const val RING_NOTIFICATION_ID = 1
        private const val MISSED_NOTIFICATION_BASE = 1000
    }
}
