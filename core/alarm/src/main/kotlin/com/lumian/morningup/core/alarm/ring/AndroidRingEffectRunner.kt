package com.lumian.morningup.core.alarm.ring

import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.alarm.RingTimers
import com.lumian.morningup.core.model.engine.AlarmRescheduler
import com.lumian.morningup.core.model.engine.AlarmStore
import com.lumian.morningup.core.model.engine.RingEffect
import com.lumian.morningup.core.model.engine.RingOutcome
import com.lumian.morningup.core.model.engine.RingSession
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 상태표 효과를 안드로이드에서 실행해요.
 * 화면 전환(Show 효과)은 [RingController.screen]이 맡고, 여기서는 잠금 화면 위로 띄울 알림만 다시 올려요.
 */
@Singleton
class AndroidRingEffectRunner @Inject constructor(
    private val sound: AlarmSoundPlayer,
    private val notifications: RingNotifications,
    private val timers: RingTimers,
    private val rescheduler: AlarmRescheduler,
    private val alarms: AlarmStore,
    private val diagnostics: AlarmDiagnostics,
) : RingEffectRunner {
    override suspend fun run(session: RingSession?, effects: List<RingEffect>) {
        effects.forEach { effect -> runOne(session, effect) }
    }

    private suspend fun runOne(session: RingSession?, effect: RingEffect) {
        when (effect) {
            is RingEffect.PlaySound -> playSound(session, effect.volumeFraction)
            RingEffect.StopSound -> sound.stop()
            RingEffect.ShowRingScreen,
            is RingEffect.ShowMission,
            RingEffect.ShowWakeCheck,
            -> notifications.showRinging(session?.phase)
            RingEffect.ShowWakeUp -> Unit
            is RingEffect.ScheduleTimer -> timers.schedule(effect.at, effect.kind)
            RingEffect.CancelTimer -> timers.cancel()
            is RingEffect.PostMissedNotification -> {
                alarms.get(effect.alarmId)?.let { notifications.missed(it.id, it.time, poweredOff = false) }
            }
            is RingEffect.RescheduleAlarm -> rescheduler.afterFired(effect.alarmId, diagnostics.now())
            is RingEffect.MarkPassed -> diagnostics.record("passed", effect.alarmId)
            is RingEffect.SessionEnded -> {
                notifications.cancelRinging()
                val event = if (effect.outcome == RingOutcome.Dismissed) "dismissed" else "missed"
                diagnostics.record(event, effect.alarmId)
            }
        }
    }

    /** 앱 볼륨은 알람마다 정한 볼륨이에요. 진동은 제 볼륨으로 울릴 때만 켜요(미션 중 20%에는 끄기). */
    private suspend fun playSound(session: RingSession?, fraction: Float) {
        val alarm = session?.let { alarms.get(it.plan.alarmId) }
        val volume = (alarm?.volumePercent ?: FULL_PERCENT) / FULL_PERCENT.toFloat() * fraction
        val vibrate = (alarm?.vibrate ?: true) && fraction >= 1f
        sound.play(volume, vibrate)
    }

    private companion object {
        const val FULL_PERCENT = 100
    }
}
