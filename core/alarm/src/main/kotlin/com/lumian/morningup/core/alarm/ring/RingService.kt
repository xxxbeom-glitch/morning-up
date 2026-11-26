package com.lumian.morningup.core.alarm.ring

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.alarm.AlarmIntents
import com.lumian.morningup.core.alarm.AlarmScope
import com.lumian.morningup.core.alarm.NextAlarmNotice
import com.lumian.morningup.core.alarm.SilencedRingLog
import com.lumian.morningup.core.alarm.sleep.SleepPlayer
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

    @Inject lateinit var sleepPlayer: SleepPlayer

    @Inject lateinit var calls: CallMonitor

    @Inject lateinit var chime: ChimePlayer

    @Inject lateinit var silencedRings: SilencedRingLog

    @Inject lateinit var nextAlarmNotice: NextAlarmNotice

    @Inject
    @AlarmScope
    lateinit var scope: CoroutineScope

    private var inFlight = 0

    private val handler = Handler(Looper.getMainLooper())

    /**
     * 울리는 중(D0)이나 W2에 화면을 끄면 다시 켜요(결정 74). 전에는 처음 끌 때만 다시 켜지고 두 번째부터는
     * 꺼진 채였어요(11-17 시험 C, D). 울림 화면의 '열릴 때 화면 켜기'는 한 번 열 때 한 번만 동작해서예요.
     */
    private val relight = Runnable {
        val phase = controller.session.value?.phase
        val off = getSystemService(PowerManager::class.java)?.isInteractive == false
        if (off && (phase == RingPhase.Ringing || phase?.isAsking == true)) {
            diagnostics.record("screen_relit", controller.session.value?.plan?.alarmId, phase.name)
            notifications.openScreenNow()
        }
    }

    /**
     * W3 '정말 깼어요?'가 떠 있는 1분 중 30초 지점에 알림음을 한 번 더 내고 화면을 켜요(결정 182).
     * 뜰 때 한 번은 상태 기계의 PlayChime이 내요. 옛 2분 반복은 없앴어요.
     */
    private val recheckNudge = Runnable {
        val session = controller.session.value
        if (session?.phase == RingPhase.RecheckAsking && session.pausedLeftMillis == null) {
            diagnostics.record("recheck_nudge", session.plan.alarmId)
            chime.play(session.plan.alarmId, recheck = true)
            notifications.openScreenNow()
        }
    }

    /** W3가 끝나기 30초 전에 [recheckNudge]를 걸어요. 서비스가 여러 번 불려도 끝 시각 기준이라 한 번만 울려요. */
    private fun scheduleRecheckNudge() {
        handler.removeCallbacks(recheckNudge)
        val session = controller.session.value ?: return
        if (session.phase != RingPhase.RecheckAsking || session.pausedLeftMillis != null) return
        val delay = session.phaseEndsAt - RECHECK_NUDGE_BEFORE_END_MILLIS - System.currentTimeMillis()
        if (delay > 0) handler.postDelayed(recheckNudge, delay)
    }

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            handler.removeCallbacks(relight)
            handler.postDelayed(relight, RELIGHT_DELAY_MILLIS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        // 앱을 열지 않고 울린 경우에도 울린 뒤 다음 알람으로 알림을 고쳐요(결정 179).
        nextAlarmNotice.start()
        ContextCompat.registerReceiver(
            this,
            screenOffReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        // 통화가 시작되면 W2, W3를 멈추고, 끝나면 남은 시간으로 다시 물어요(결정 145 '통화 중').
        calls.start(CALL_KEY) { inCall ->
            scope.launch {
                controller.send(RingEvent.CallChanged(inCall))
                if (!inCall) openScreenIfNeeded(intent = null, force = true)
            }
        }
        // 화면에서 남은 확인을 멈추는 등 서비스 밖에서 울림이 끝나면 스스로 멈춰요.
        lifecycleScope.launch {
            var previous = controller.session.value
            controller.session.collect { session ->
                val ended = previous != null && session == null
                previous = session
                if (ended && inFlight == 0) {
                    sound.stop()
                    ServiceCompat.stopForeground(this@RingService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // 알람 시각이나 타이머로 깨어났으면 첫 알림부터 전체 화면 Intent를 달아요(새 알림일 때만 화면이 떠요).
        val wakeUp = intent?.action == AlarmIntents.ACTION_FIRE || intent?.action == AlarmIntents.ACTION_TIMER
        // 잠들기 소리를 틀어 둔 채 잠들었어도 알람이 이겨요.
        if (intent?.action == AlarmIntents.ACTION_FIRE) sleepPlayer.stopForAlarm()
        if (!goForeground(if (wakeUp) RingPhase.Ringing else controller.session.value?.phase)) {
            if (wakeUp) notifications.fallbackAlert()
            stopSelf(startId)
            return START_NOT_STICKY
        }
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
        val before = controller.session.value
        dispatch(intent)
        // 시험 기록(11-17 사용자 지시): 무슨 신호로 어느 단계에서 어느 단계로 갔는지 남겨요.
        val after = controller.session.value
        val alarmId = (after ?: before)?.plan?.alarmId
        diagnostics.record(
            "ring_event",
            alarmId,
            "${signalOf(intent)}: ${before?.phase ?: "-"} -> ${after?.phase ?: "end"}",
        )
        openScreenIfNeeded(intent)
    }

    private fun signalOf(intent: Intent?): String = when (intent?.action) {
        AlarmIntents.ACTION_FIRE -> "alarm_time"
        AlarmIntents.ACTION_TIMER -> "timer ${AlarmIntents.timerKindOf(intent)}"
        ACTION_EVENT -> intent.getStringExtra(EXTRA_EVENT) ?: "event"
        else -> "resume"
    }

    private suspend fun dispatch(intent: Intent?) {
        when (intent?.action) {
            AlarmIntents.ACTION_FIRE -> {
                val id = intent.getLongExtra(AlarmIntents.EXTRA_ALARM_ID, -1)
                diagnostics.record("fired", id)
                // 방해 금지가 소리를 막는 채 울렸으면 다음에 앱을 열 때 알려요(TSK-177 R2 '울린 뒤').
                if (controller.fire(id)) silencedRings.onRing(id) else diagnostics.record("fired_unknown", id)
            }
            AlarmIntents.ACTION_TIMER ->
                AlarmIntents.timerKindOf(intent)?.let { controller.send(RingEvent.TimerFired(it)) }
            ACTION_EVENT -> eventOf(intent)?.let { controller.send(it) }
            // 시스템이 서비스를 되살렸거나(intent 없음) 앱이 이어가기를 부탁했어요.
            else -> controller.resume()
        }
    }

    private suspend fun openScreenIfNeeded(intent: Intent?, force: Boolean = false) {
        val wakeUp = force || intent?.action == AlarmIntents.ACTION_FIRE || intent?.action == AlarmIntents.ACTION_TIMER
        // 울림(D0)과 깨어 있나요 확인(W2, W3) 모두 폰을 쓰는 중이어도 맨 앞에 바로 떠야 해요. 통화 중이면 미뤄요.
        val session = controller.session.value
        val phase = session?.phase
        val show = phase == RingPhase.Ringing || (phase?.isAsking == true && session.pausedLeftMillis == null)
        if (wakeUp && show) {
            withContext(Dispatchers.Main) {
                // 화면을 맨 앞에 띄웠으면 알림은 조용히 두고, 막혔거나 잠금 화면이면 배너와 전체 화면 알림으로 깨워요.
                val opened = notifications.openScreenNow()
                val locked = getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == true
                if (!opened || locked) notifications.alertRinging(phase)
            }
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
            RingPhase.Resting, RingPhase.AwaitingWakeCheck, RingPhase.AwaitingRecheck -> {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                val session = controller.session.value
                notifications.showRinging(phase, session?.phaseEndsAt, session?.recheckNotice())
                stopSelf(startId)
            }
            else -> {
                notifications.showRinging(phase)
                scheduleRecheckNudge()
            }
        }
    }

    /** 앞 서비스로 올려요. 안드로이드가 막으면(백그라운드 시작 제한) 기록하고 false. */
    private fun goForeground(phase: RingPhase?): Boolean {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
        } else {
            0
        }
        return try {
            ServiceCompat.startForeground(
                this,
                RingNotifications.RING_NOTIFICATION_ID,
                notifications.ringing(phase),
                type,
            )
            true
        } catch (e: IllegalStateException) {
            diagnostics.record("foreground_denied", detail = e.javaClass.simpleName)
            false
        }
    }

    override fun onDestroy() {
        // 울리지 않는 단계면 소리를 확실히 멈춰요. 울리는 중에 시스템이 서비스를 죽이면 START_STICKY로 다시 이어가요.
        val phase = controller.session.value?.phase
        val quiet = phase == null || phase == RingPhase.Resting || phase == RingPhase.AwaitingWakeCheck ||
            phase == RingPhase.AwaitingRecheck
        if (quiet) sound.stop()
        handler.removeCallbacks(relight)
        handler.removeCallbacks(recheckNudge)
        calls.stop(CALL_KEY)
        unregisterReceiver(screenOffReceiver)
        super.onDestroy()
    }

    companion object {
        const val ACTION_EVENT = "com.lumian.morningup.alarm.EVENT"
        private const val EXTRA_EVENT = "event"

        /** 화면이 꺼지고 잠시 뒤 다시 켜요. 끄자마자 열면 꺼지는 중과 겹쳐요. */
        private const val RELIGHT_DELAY_MILLIS = 600L

        /** W3 끝나기 이만큼 전(1분 중 30초 지점)에 알림음을 한 번 더 내요(결정 182). */
        private const val RECHECK_NUDGE_BEFORE_END_MILLIS = 30_000L
        private const val CALL_KEY = "ring_checks"

        /** 울림 화면 버튼을 서비스로 보내요. 화면이 앞에 있으니 서비스를 앞으로 띄울 수 있어요. */
        fun send(context: Context, event: RingEvent) {
            context.startForegroundService(eventIntent(context, event))
        }

        fun eventIntent(context: Context, event: RingEvent): Intent =
            AlarmIntents.serviceIntent(context, ACTION_EVENT).putExtra(EXTRA_EVENT, RingEvent.keyOf(event))

        fun eventOf(intent: Intent): RingEvent? =
            intent.getStringExtra(EXTRA_EVENT)?.let { RingEvent.userEventKeys[it] }
    }
}
