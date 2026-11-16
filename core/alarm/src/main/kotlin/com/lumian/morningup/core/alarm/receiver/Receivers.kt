package com.lumian.morningup.core.alarm.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.alarm.AlarmIntents
import com.lumian.morningup.core.alarm.AlarmScope
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ring.RingNotifications
import com.lumian.morningup.core.data.session.RingSessionStore
import com.lumian.morningup.core.model.engine.AlarmRescheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 서비스를 앞으로 띄워요. 정확한 알람이 깨운 직후라 안드로이드 12 이상의 백그라운드 제한에서 빠져요. */
internal fun startRingService(
    context: Context,
    intent: Intent,
    diagnostics: AlarmDiagnostics,
    notifications: RingNotifications,
) {
    try {
        context.startForegroundService(intent)
    } catch (e: IllegalStateException) {
        // ForegroundServiceStartNotAllowedException도 여기로 와요.
        // 정확한 알람 권한이 없어 대신 예약된 알람이면 이렇게 될 수 있어서, 알림 소리로라도 깨워요.
        diagnostics.record("service_start_failed", detail = e.javaClass.simpleName)
        if (intent.action != AlarmIntents.ACTION_RESUME) notifications.fallbackAlert()
    }
}

/** 알람 시각이 됐어요. */
@AndroidEntryPoint
class AlarmFireReceiver : BroadcastReceiver() {
    @Inject lateinit var diagnostics: AlarmDiagnostics

    @Inject lateinit var notifications: RingNotifications

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmIntents.ACTION_FIRE) return
        val id = intent.getLongExtra(AlarmIntents.EXTRA_ALARM_ID, -1)
        val service = AlarmIntents.serviceIntent(context, AlarmIntents.ACTION_FIRE)
            .putExtra(AlarmIntents.EXTRA_ALARM_ID, id)
        startRingService(context, service, diagnostics, notifications)
    }
}

/** 울리는 중 타이머(1분 울림, 쉬는 시간 끝, 59초 미션, W2)가 됐어요. */
@AndroidEntryPoint
class RingTimerReceiver : BroadcastReceiver() {
    @Inject lateinit var diagnostics: AlarmDiagnostics

    @Inject lateinit var notifications: RingNotifications

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmIntents.ACTION_TIMER) return
        val service = AlarmIntents.serviceIntent(context, AlarmIntents.ACTION_TIMER)
            .putExtra(AlarmIntents.EXTRA_TIMER_KIND, intent.getStringExtra(AlarmIntents.EXTRA_TIMER_KIND))
        startRingService(context, service, diagnostics, notifications)
    }
}

/**
 * 예약이 사라지거나 틀어질 수 있는 시스템 신호를 받아 전부 다시 예약해요.
 * 부팅(잠금 풀기 전 포함), 시간 변경, 시간대 변경, 앱 업데이트, 정확한 알람 권한 변경.
 */
@AndroidEntryPoint
class SystemEventReceiver : BroadcastReceiver() {
    @Inject lateinit var rescheduler: AlarmRescheduler

    @Inject lateinit var now: NowProvider

    @Inject lateinit var sessions: RingSessionStore

    @Inject lateinit var diagnostics: AlarmDiagnostics

    @Inject lateinit var notifications: RingNotifications

    @Inject
    @AlarmScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in ACTIONS) return
        diagnostics.record("system_event", detail = action.substringAfterLast('.'))
        val pending = goAsync()
        scope.launch {
            try {
                rescheduler.rescheduleAll(now.now())
                // 하던 울림이 있으면 이어가요(재부팅 직전에 울리던 알람).
                if (sessions.load() != null) {
                    val resume = AlarmIntents.serviceIntent(context, AlarmIntents.ACTION_RESUME)
                    startRingService(context, resume, diagnostics, notifications)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        val ACTIONS = setOf(
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )
    }
}
