package com.lumian.morningup.core.alarm.ring

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lumian.morningup.core.alarm.AlarmDiagnostics
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
    private val diagnostics: AlarmDiagnostics,
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
        val alarmAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val fallback = NotificationChannel(CHANNEL_FALLBACK, text(R.string.channel_fallback), high).apply {
            description = text(R.string.channel_fallback_description)
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), alarmAudio)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        manager.createNotificationChannels(listOf(ringing, missed, fallback))
    }

    fun ringing(phase: RingPhase?): Notification {
        ensureChannels()
        val open = PendingIntent.getActivity(
            context,
            0,
            screenIntent.create(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val urgent = !isWaiting(phase)
        return NotificationCompat.Builder(context, CHANNEL_RINGING)
            .setSmallIcon(R.drawable.ic_alarm_clock)
            .setContentTitle(text(R.string.ring_title))
            .setContentText(text(phaseText(phase)))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            // setSilent는 쓰지 않아요. 알림이 조용한 묶음에 들어가 전체 화면 Intent가 막혀요. 채널이 이미 조용해요.
            .setContentIntent(open)
            .apply { if (urgent) setFullScreenIntent(open, true) }
            .build()
    }

    /**
     * 울리는 단계면 앞 서비스 알림(ID 1)을 고치고, 쉬는 중이나 W2 기다림이면 다른 ID(2)로 알림만 남겨요.
     * 다시 울릴 때 ID 1이 새 알림으로 올라와야 잠금 화면 위로 울림 화면이 떠요.
     */
    @SuppressLint("MissingPermission")
    fun showRinging(phase: RingPhase?) {
        if (!manager.areNotificationsEnabled()) {
            diagnostics.record("notifications_disabled")
            return
        }
        if (isWaiting(phase)) {
            manager.notify(WAITING_NOTIFICATION_ID, ringing(phase))
        } else {
            manager.cancel(WAITING_NOTIFICATION_ID)
            manager.notify(RING_NOTIFICATION_ID, ringing(phase))
        }
    }

    fun cancelRinging() {
        manager.cancel(RING_NOTIFICATION_ID)
        manager.cancel(WAITING_NOTIFICATION_ID)
    }

    /**
     * 마지막 수단: 울림 서비스를 띄우지 못했을 때 알림 소리(폰 기본 알람음)와 전체 화면 Intent로 깨워요.
     * 알람 소리 통로를 쓰는 별도 채널이라 무음 모드에서도 들려요.
     */
    @SuppressLint("MissingPermission")
    fun fallbackAlert() {
        ensureChannels()
        val open = PendingIntent.getActivity(
            context,
            0,
            screenIntent.create(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_FALLBACK)
            .setSmallIcon(R.drawable.ic_alarm_clock)
            .setContentTitle(text(R.string.ring_title))
            .setContentText(text(R.string.ring_text_ringing))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setAutoCancel(true)
            .build()
            .apply { flags = flags or Notification.FLAG_INSISTENT }
        if (manager.areNotificationsEnabled()) manager.notify(FALLBACK_NOTIFICATION_ID, notification)
    }

    /** 안드로이드 14부터 사용자가 전체 화면 알림 권한을 끌 수 있어요. 꺼져 있으면 잠금 화면 위로 울림 화면이 안 떠요. */
    fun canShowFullScreen(): Boolean = manager.canUseFullScreenIntent()

    private fun isWaiting(phase: RingPhase?) = phase == RingPhase.Resting || phase == RingPhase.AwaitingWakeCheck

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
        const val CHANNEL_FALLBACK = "ringing_fallback"
        const val RING_NOTIFICATION_ID = 1
        const val WAITING_NOTIFICATION_ID = 2
        private const val FALLBACK_NOTIFICATION_ID = 3
        private const val MISSED_NOTIFICATION_BASE = 1000
    }
}
