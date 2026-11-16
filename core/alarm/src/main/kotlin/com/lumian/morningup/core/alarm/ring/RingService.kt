package com.lumian.morningup.core.alarm.ring

import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.alarm.AlarmIntents
import com.lumian.morningup.core.alarm.AlarmScope
import com.lumian.morningup.core.model.engine.RingEvent
import com.lumian.morningup.core.model.engine.RingPhase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 울리는 동안 앞에 떠 있는 서비스. 모든 울림 신호(알람 시각, 타이머, 화면 버튼)가 이곳을 지나요.
 * 시작하자마자 알림을 띄우고(안드로이드 규칙), 쉬는 중과 W2 기다림에는 알림만 남기고 멈춰요.
 * 처리는 [AlarmScope]에서 해서, 서비스가 멈춰도 하던 저장은 끝까지 해요.
 */
@AndroidEntryPoint
class RingService : LifecycleService() {
    @Inject lateinit var controller: RingController

    @Inject lateinit var notifications: RingNotifications

    @Inject lateinit var sound: AlarmSoundPlayer

    @Inject lateinit var diagnostics: AlarmDiagnostics

    @Inject
    @AlarmScope
    lateinit var scope: CoroutineScope

    private var inFlight = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        goForeground()
        inFlight++
        scope.launch {
            try {
                handle(intent)
            } finally {
                withContext(NonCancellable + Dispatchers.Main) {
                    inFlight--
                    if (inFlight == 0) settle(startId)
                }
            }
        }
        return START_STICKY
    }

    private suspend fun handle(intent: Intent?) {
        when (intent?.action) {
            AlarmIntents.ACTION_FIRE -> {
                val id = intent.getLongExtra(AlarmIntents.EXTRA_ALARM_ID, -1)
                diagnostics.record("fired", id)
                if (!controller.fire(id)) diagnostics.record("fired_unknown", id)
            }
            AlarmIntents.ACTION_TIMER ->
                AlarmIntents.timerKindOf(intent)?.let { controller.send(RingEvent.TimerFired(it)) }
            ACTION_EVENT -> eventOf(intent)?.let { controller.send(it) }
            // 시스템이 서비스를 되살렸거나(intent 없음) 앱이 이어가기를 부탁했어요.
            else -> controller.resume()
        }
    }

    /** 처리가 다 끝난 뒤: 울리는 중이면 알림을 고치고, 아니면 멈춰요. */
    private fun settle(startId: Int) {
        val phase = controller.session.value?.phase
        when (phase) {
            null -> {
                sound.stop()
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
            RingPhase.Resting, RingPhase.AwaitingWakeCheck -> {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                notifications.showRinging(phase)
                stopSelf(startId)
            }
            else -> notifications.showRinging(phase)
        }
    }

    private fun goForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            RingNotifications.RING_NOTIFICATION_ID,
            notifications.ringing(controller.session.value?.phase),
            type,
        )
    }

    override fun onDestroy() {
        // 울리지 않는 단계면 소리를 확실히 멈춰요. 울리는 중에 시스템이 서비스를 죽이면 START_STICKY로 다시 이어가요.
        val phase = controller.session.value?.phase
        if (phase == null || phase == RingPhase.Resting || phase == RingPhase.AwaitingWakeCheck) sound.stop()
        super.onDestroy()
    }

    companion object {
        const val ACTION_EVENT = "com.lumian.morningup.alarm.EVENT"
        private const val EXTRA_EVENT = "event"

        /** 울림 화면 버튼을 서비스로 보내요. 화면이 앞에 있으니 서비스를 앞으로 띄울 수 있어요. */
        fun send(context: Context, event: RingEvent) {
            val intent = AlarmIntents.serviceIntent(context, ACTION_EVENT)
                .putExtra(EXTRA_EVENT, event.javaClass.simpleName)
            context.startForegroundService(intent)
        }

        fun eventOf(intent: Intent): RingEvent? {
            val name = intent.getStringExtra(EXTRA_EVENT) ?: return null
            return RingEvent.userEvents.firstOrNull { it.javaClass.simpleName == name }
        }
    }
}
