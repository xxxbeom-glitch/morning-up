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
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.alarm.R
import com.lumian.morningup.core.model.engine.RingPhase
import com.lumian.morningup.core.model.engine.RingSession
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 실험실 여러 번 묻기(결정 146)에서 W3를 기다리는 알림 내용. [left]는 남은 확인 수, [canFinishEarly]는 이번
 * 기다림에 아직 미리 끝내지 않아 지금 하나 끝낼 수 있는지예요.
 */
data class RecheckNotice(val left: Int, val canFinishEarly: Boolean)

/** 세션에서 알림 내용. 여러 번 묻기가 아니면 null이에요. */
fun RingSession.recheckNotice(): RecheckNotice? = checksLeft?.let { RecheckNotice(it, canFinishEarly) }

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
        // 울리는 중 상태 알림(앞 서비스, ID 1). 울림 화면이 이미 맨 앞에 떠 있으니 배너 없이 알림창에만 둬요.
        val status = NotificationChannel(
            CHANNEL_STATUS,
            text(R.string.channel_status),
            NotificationManager.IMPORTANCE_LOW,
        )
            .apply {
                description = text(R.string.channel_status_description)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        // 쉬는 중, W2 기다림 알림은 배너 없이 알림창에만 조용히 남겨요.
        val waiting = NotificationChannel(
            CHANNEL_WAITING,
            text(R.string.channel_waiting),
            NotificationManager.IMPORTANCE_LOW,
        )
            .apply { description = text(R.string.channel_waiting_description) }
        val normal = NotificationManager.IMPORTANCE_DEFAULT
        val missed = NotificationChannel(CHANNEL_MISSED, text(R.string.channel_missed), normal)
            .apply { description = text(R.string.channel_missed_description) }
        val alarmAudio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val fallback = NotificationChannel(CHANNEL_FALLBACK, text(R.string.channel_fallback), high).apply {
            description = text(R.string.channel_fallback_description)
            // 디버그 빌드는 음소거라 대신 울림 알림도 소리 없이 만들어요(이미 만든 채널은 기기에 남은 값을 써요).
            if (RingMute.MUTED) {
                setSound(null, null)
            } else {
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), alarmAudio)
            }
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(true)
        }
        // 미션 비상구(결정 149): 소리, 진동 없는 조용한 알림이지만 바로 답장 버튼이 보이게 기본 중요도예요.
        val escape = NotificationChannel(CHANNEL_ESCAPE, text(R.string.channel_escape), normal).apply {
            description = text(R.string.channel_escape_description)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannels(listOf(ringing, status, waiting, missed, fallback, escape))
    }

    /**
     * 앞 서비스 알림(ID 1). 울리는 동안 알림창에 조용히 남아요(배너, 전체 화면 없음).
     * 화면을 깨우는 일은 [openScreenNow]가 하고, 그게 막히면 [alertRinging]이 배너와 전체 화면으로 대신해요.
     */
    fun ringing(phase: RingPhase?, phaseEndsAt: Long? = null, notice: RecheckNotice? = null): Notification {
        ensureChannels()
        if (isWaiting(phase)) return waiting(phase, phaseEndsAt, notice)
        return ringBuilder(CHANNEL_STATUS, phase)
            .setOnlyAlertOnce(true)
            .build()
    }

    /**
     * 울림 화면을 바로 띄우지 못했거나 잠금 화면일 때만 올리는 깨우기 알림(ID 4).
     * 폰을 쓰는 중이면 위에서 배너로 내려오고, 잠금 화면이면 전체 화면 Intent로 울림 화면이 떠요.
     */
    @SuppressLint("MissingPermission")
    fun alertRinging(phase: RingPhase?) {
        ensureChannels()
        if (!manager.areNotificationsEnabled()) return
        val open = screenPendingIntent()
        // setSilent는 쓰지 않아요. 알림이 조용한 묶음에 들어가 전체 화면 Intent가 막혀요. 채널이 이미 조용해요.
        val notification = ringBuilder(CHANNEL_RINGING, phase)
            .setFullScreenIntent(open, true)
            .build()
        // 이미 떠 있는 알림을 고치기만 하면 꺼진 화면을 다시 깨우지 않아요. W2에 답하지 않아 바로 다시 울릴 때
        // 화면이 꺼진 채 소리만 났어요(11-17 시험 D). 지우고 새로 올려야 전체 화면 Intent가 다시 동작해요.
        manager.cancel(ALERT_NOTIFICATION_ID)
        manager.notify(ALERT_NOTIFICATION_ID, notification)
        diagnostics.record("ring_alert_posted", detail = phase?.name.orEmpty())
    }

    private fun ringBuilder(channel: String, phase: RingPhase?): NotificationCompat.Builder {
        val title = when (phase) {
            RingPhase.WakeCheckAsking -> R.string.wake_check_title
            RingPhase.RecheckAsking -> R.string.recheck_title
            else -> R.string.ring_title
        }
        return NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_alarm_clock)
            .setContentTitle(text(title))
            .setContentText(text(phaseText(phase)))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(screenPendingIntent())
    }

    private fun screenPendingIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        screenIntent.create(context),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /**
     * 쉬는 중이나 W2 기다림 알림. 울리는 게 아니라서 제목을 따로 두고, 배너 없는 조용한 통로를 써요.
     * 쉬는 중이면 다시 울릴 시각을 적고, 누르면 울림 화면을 열어 지금 끌 수 있게 해요(결정 70).
     * W2 기다림은 할 일이 없는 울림 화면 대신 앱 홈을 열어요.
     */
    private fun waiting(phase: RingPhase?, phaseEndsAt: Long?, notice: RecheckNotice?): Notification {
        if (phase == RingPhase.AwaitingRecheck) {
            return if (notice != null) multiRecheckWaiting(notice) else recheckWaiting(phaseEndsAt)
        }
        val resting = phase == RingPhase.Resting
        val target = if (resting) {
            screenIntent.create(context).putExtra(EXTRA_FROM_REST, true)
        } else {
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        }
        val open = target?.let {
            PendingIntent.getActivity(context, 1, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val title = if (resting) R.string.resting_title else R.string.waiting_title
        val body = if (resting && phaseEndsAt != null) {
            val at = Instant.ofEpochMilli(phaseEndsAt).atZone(ZoneId.systemDefault()).toLocalTime()
            context.getString(R.string.ring_text_resting_at, at.format(DateTimeFormatter.ofPattern("H:mm")))
        } else {
            text(phaseText(phase))
        }
        return NotificationCompat.Builder(context, CHANNEL_WAITING)
            .setSmallIcon(R.drawable.ic_alarm_clock)
            .setContentTitle(text(title))
            .setContentText(body)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setContentIntent(open)
            .build()
    }

    /**
     * W2 '깼어요' 뒤 W3를 기다리는 동안의 조용한 알림(결정 145 9번). 제목은 '오전 6:43에 한 번 더 물어볼게요',
     * 줄어드는 시계를 달고, 동작 버튼은 없어요. 누르면 W3를 바로 열어 지금 끝낼 수 있어요.
     */
    private fun recheckWaiting(askAt: Long?): Notification {
        val open = PendingIntent.getActivity(
            context,
            2,
            screenIntent.create(context).putExtra(EXTRA_OPEN_RECHECK, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = askAt?.let { context.getString(R.string.recheck_waiting_title, clock12(it)) }
            ?: text(R.string.ring_text_recheck_waiting)
        val builder = NotificationCompat.Builder(context, CHANNEL_WAITING)
            .setSmallIcon(R.drawable.ic_alarm_clock)
            .setContentTitle(title)
            .setContentText(text(R.string.recheck_waiting_text))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(open)
        if (askAt != null) {
            builder.setWhen(askAt).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true)
        }
        return builder.build()
    }

    /**
     * 여러 번 묻기의 W3 기다림 알림(결정 146 3절). 묻는 때가 무작위라 시각과 줄어드는 시계를 달지 않아요.
     * 제목 '곧 한 번 더 물어볼게요', 본문 '남은 확인 2번. 눌러서 지금 하나 끝낼 수 있어요'.
     * 이번 기다림에 이미 하나 미리 끝냈으면 본문은 남은 수만 적고, 누르면 앱 홈을 열어요(한 번에 하나만).
     */
    private fun multiRecheckWaiting(notice: RecheckNotice): Notification {
        val target = if (notice.canFinishEarly) {
            screenIntent.create(context).putExtra(EXTRA_OPEN_RECHECK, true)
        } else {
            context.packageManager.getLaunchIntentForPackage(context.packageName)
        }
        val open = target?.let {
            PendingIntent.getActivity(context, 2, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val body = if (notice.canFinishEarly) R.string.recheck_multi_waiting_text else R.string.recheck_multi_left
        return NotificationCompat.Builder(context, CHANNEL_WAITING)
            .setSmallIcon(R.drawable.ic_alarm_clock)
            .setContentTitle(text(R.string.recheck_multi_waiting_title))
            .setContentText(context.getString(body, notice.left))
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setContentIntent(open)
            .build()
    }

    /** 12시간제 시각. 예: 오전 6:43. */
    private fun clock12(millis: Long): String {
        val at = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
        val template = if (at.hour < NOON) R.string.clock_am else R.string.clock_pm
        return context.getString(template, at.format(DateTimeFormatter.ofPattern("h:mm")))
    }

    /**
     * 울리는 단계면 앞 서비스 알림(ID 1)을 고치고, 쉬는 중이나 W2 기다림이면 다른 ID(2)로 알림만 남겨요.
     * 다시 울릴 때 ID 1이 새 알림으로 올라와야 잠금 화면 위로 울림 화면이 떠요.
     */
    @SuppressLint("MissingPermission")
    fun showRinging(phase: RingPhase?, phaseEndsAt: Long? = null, notice: RecheckNotice? = null) {
        if (!manager.areNotificationsEnabled()) {
            diagnostics.record("notifications_disabled")
            return
        }
        // 깨우기 알림은 울림(D0)이나 W2 동안만 남기고, 미션을 시작하거나 쉬면 지워요.
        if (phase != RingPhase.Ringing && phase?.isAsking != true) manager.cancel(ALERT_NOTIFICATION_ID)
        if (isWaiting(phase)) {
            manager.notify(WAITING_NOTIFICATION_ID, ringing(phase, phaseEndsAt, notice))
        } else {
            manager.cancel(WAITING_NOTIFICATION_ID)
            manager.notify(RING_NOTIFICATION_ID, ringing(phase))
        }
    }

    fun cancelRinging() {
        manager.cancel(RING_NOTIFICATION_ID)
        manager.cancel(ESCAPE_NOTIFICATION_ID)
        manager.cancel(WAITING_NOTIFICATION_ID)
        manager.cancel(ALERT_NOTIFICATION_ID)
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

    /**
     * 알림만으로는 울림 화면이 안 뜨는 경우가 있어요(폰을 쓰는 중이면 위에서 내려오는 알림만, 일부 기기는 잠금 화면에서도).
     * 화면을 켜고, '다른 앱 위에 표시'가 허용돼 있으면 울림 화면을 바로 맨 앞에 띄워요. 막히면 알림이 그대로 대신해요.
     */
    @Suppress("DEPRECATION")
    fun openScreenNow(): Boolean {
        val power = context.getSystemService(PowerManager::class.java)
        val screenOff = power?.isInteractive == false
        power?.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "morningup:ring_screen",
        )?.acquire(SCREEN_WAKE_MILLIS)
        if (!Settings.canDrawOverlays(context)) {
            diagnostics.record("screen_open_skipped", detail = "no_overlay")
            return false
        }
        // 위 깨우기 잠금은 폰이 '화면 켜기' 권한이 없다고 거절해요(targetSdk 34부터). 그래서 화면은 울림 화면의
        // turnScreenOn이 켜는데, 이건 화면이 새로 열릴 때만 동작해요. W2에 답하지 않아 다시 울릴 때는 화면이 이미
        // 열려 있어서 꺼진 채 소리만 났어요(11-17 시험 D). 화면이 꺼져 있으면 울림 화면을 닫고 새로 열어요(결정 73).
        val intent = screenIntent.create(context)
        if (screenOff) intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        return try {
            context.startActivity(intent)
            diagnostics.record("screen_opened", detail = if (screenOff) "fresh" else "")
            true
        } catch (e: RuntimeException) {
            diagnostics.record("screen_open_failed", detail = e.javaClass.simpleName)
            false
        }
    }

    /**
     * 쉬기 시작할 때 화면 아래 짧은 안내(결정 70). 화면이 켜져 있을 때만 보여요.
     * 예: '3분 뒤 2번째 알람이 울려요'. 끌 때까지 울리는 알람은 번째를 빼요.
     */
    fun restToast(minutes: Int, nextNumber: Int?) {
        val message = if (nextNumber != null) {
            context.getString(R.string.rest_toast_numbered, minutes, nextNumber)
        } else {
            context.getString(R.string.rest_toast, minutes)
        }
        Handler(Looper.getMainLooper()).post { Toast.makeText(context, message, Toast.LENGTH_LONG).show() }
    }

    /** 안드로이드 14부터 사용자가 전체 화면 알림 권한을 끌 수 있어요. 꺼져 있으면 잠금 화면 위로 울림 화면이 안 떠요. */
    fun canShowFullScreen(): Boolean = manager.canUseFullScreenIntent()

    private fun isWaiting(phase: RingPhase?) =
        phase == RingPhase.Resting || phase == RingPhase.AwaitingWakeCheck || phase == RingPhase.AwaitingRecheck

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
        RingPhase.AwaitingRecheck -> R.string.ring_text_recheck_waiting
        RingPhase.RecheckAsking -> R.string.ring_text_recheck
    }

    private fun text(id: Int) = context.getString(id)

    companion object {
        const val CHANNEL_RINGING = "ringing"
        const val CHANNEL_STATUS = "ring_status"
        const val CHANNEL_WAITING = "waiting"
        const val CHANNEL_MISSED = "missed"
        const val CHANNEL_FALLBACK = "ringing_fallback"
        const val CHANNEL_ESCAPE = "mission_escape"

        /** 쉬는 중 알림을 눌러 울림 화면을 열었다는 표시. 쉬는 동안에도 울림 화면을 보여 줘요. */
        const val EXTRA_FROM_REST = "from_rest"

        /** W3 기다림 알림이나 홈 띠를 눌러 W3를 미리 열었다는 표시(결정 145). */
        const val EXTRA_OPEN_RECHECK = "open_recheck"
        const val RING_NOTIFICATION_ID = 1
        const val WAITING_NOTIFICATION_ID = 2
        private const val FALLBACK_NOTIFICATION_ID = 3
        private const val ALERT_NOTIFICATION_ID = 4

        /** 미션 비상구 알림과, 써서 끈 뒤 3초 보이는 '알람을 껐어요'(결정 149). */
        const val ESCAPE_NOTIFICATION_ID = 5
        const val ESCAPE_DONE_NOTIFICATION_ID = 6
        private const val MISSED_NOTIFICATION_BASE = 1000
        private const val SCREEN_WAKE_MILLIS = 10_000L
        private const val NOON = 12
    }
}
