package com.lumian.morningup.core.alarm.ring

import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.alarm.RingTimers
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.VibrationPattern
import com.lumian.morningup.core.model.engine.AlarmRescheduler
import com.lumian.morningup.core.model.engine.AlarmStore
import com.lumian.morningup.core.model.engine.RingEffect
import com.lumian.morningup.core.model.engine.RingOutcome
import com.lumian.morningup.core.model.engine.RingSession
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

/**
 * 상태표 효과를 안드로이드에서 실행해요.
 * 화면 전환(Show 효과)은 [RingController.screen]이 맡고, 여기서는 잠금 화면 위로 띄울 알림만 다시 올려요.
 */
// 효과마다 맡는 장치가 하나씩이라 묶지 않고 그대로 받아요(W2, W3 짧은 알림음, 실험실 기록이 더해져 9개).
@Suppress("LongParameterList")
@Singleton
class AndroidRingEffectRunner @Inject constructor(
    private val sound: AlarmSoundPlayer,
    private val notifications: RingNotifications,
    private val timers: RingTimers,
    private val rescheduler: AlarmRescheduler,
    private val alarms: AlarmStore,
    private val diagnostics: AlarmDiagnostics,
    private val chime: ChimePlayer,
    private val labLog: LabLog,
) : RingEffectRunner {
    override suspend fun run(session: RingSession?, effects: List<RingEffect>) {
        // 효과 하나가 실패해도(소리 장치 오류 등) 뒤의 타이머 예약과 다시 예약은 꼭 해요.
        effects.forEach { effect ->
            try {
                runOne(session, effect)
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                val detail = "${effect.javaClass.simpleName} ${e.javaClass.simpleName}"
                diagnostics.record("effect_failed", session?.plan?.alarmId, detail)
            }
        }
    }

    private suspend fun runOne(session: RingSession?, effect: RingEffect) {
        when (effect) {
            is RingEffect.PlaySound -> playSound(session, effect.volumeFraction)
            RingEffect.StopSound -> {
                if (sound.isPlaying) diagnostics.record("sound_stop", session?.plan?.alarmId)
                sound.stop()
            }
            RingEffect.ShowRingScreen,
            is RingEffect.ShowMission,
            RingEffect.ShowWakeCheck,
            RingEffect.ShowRecheck,
            -> notifications.showRinging(session?.phase)
            RingEffect.ShowWakeUp -> Unit
            RingEffect.PlayChime -> chime.play(session?.plan?.alarmId)
            RingEffect.PausedForCall -> diagnostics.record("check_paused_for_call", session?.plan?.alarmId)
            is RingEffect.RecheckScheduled -> {
                diagnostics.record("recheck_scheduled", session?.plan?.alarmId, "at=${clock(effect.askAt)}")
                if (effect.number != null) {
                    // 여러 번 묻기: 몇 번째/전체와 고른 간격은 디버그 흐름 로그에만 남겨요(결정 146 4절).
                    labLog.record(
                        LabLog.RECHECK_ASK,
                        session?.plan?.alarmId,
                        "n=${effect.number}/${effect.total} gap=${effect.gapMinutes}m at=${clock(effect.askAt)}",
                    )
                }
                notifications.showRinging(session?.phase, effect.askAt, session?.recheckNotice())
            }
            is RingEffect.MultiAskRecorded -> {
                val left = effect.left?.let { " left=$it" }.orEmpty()
                labLog.record(effect.kind.event, effect.alarmId, "n=${effect.number}/${effect.total}$left", effect.kind.kept)
            }
            is RingEffect.WakeResultRecorded -> {
                val detail = "result=${effect.result.name} rounds=${effect.rounds}"
                diagnostics.record("wake_result", effect.alarmId, detail)
                FlowLog.event("wake_result", effect.alarmId, detail)
            }
            is RingEffect.RestStarted -> {
                diagnostics.record(
                    "rest_started",
                    session?.plan?.alarmId,
                    "next=${clock(effect.nextRingAt)} #${effect.nextRingNumber ?: "-"}",
                )
                notifications.restToast(effect.intervalMinutes, effect.nextRingNumber)
            }
            is RingEffect.ScheduleTimer -> {
                diagnostics.record("timer_set", session?.plan?.alarmId, "${effect.kind} at ${clock(effect.at)}")
                timers.schedule(effect.at, effect.kind)
            }
            RingEffect.CancelTimer -> timers.cancel()
            is RingEffect.PostMissedNotification -> {
                alarms.get(effect.alarmId)?.let { notifications.missed(it.id, it.time, poweredOff = false) }
            }
            is RingEffect.RescheduleAlarm -> rescheduler.afterFired(effect.alarmId, diagnostics.now())
            is RingEffect.MarkPassed -> diagnostics.record("passed", effect.alarmId)
            is RingEffect.SessionEnded -> {
                notifications.cancelRinging()
                val event = when (effect.outcome) {
                    RingOutcome.Dismissed -> "dismissed"
                    RingOutcome.Missed -> "missed"
                    RingOutcome.Cancelled -> "checks_cancelled"
                }
                diagnostics.record(event, effect.alarmId)
            }
        }
    }

    /**
     * 앱 볼륨은 알람마다 정한 볼륨이에요. 진동은 제 볼륨으로 울릴 때만 켜요(미션 중에는 소리와 진동 모두 꺼요, 결정 65).
     * 알람음을 끈 알람(진동만, S5)은 소리 0으로 진동만 울려요. 부드럽게 깨우기면 처음 켤 때 서서히 올려요(S7).
     */
    private suspend fun playSound(session: RingSession?, fraction: Float) {
        val alarm = session?.let { alarms.get(it.plan.alarmId) }
        val silent = alarm?.soundId == Alarm.SILENT_SOUND_ID
        val volume = if (silent) 0f else (alarm?.volumePercent ?: FULL_PERCENT) / FULL_PERCENT.toFloat() * fraction
        val vibrate = ((alarm?.vibrate ?: true) || silent) && fraction >= 1f
        val gentle = if (alarm?.gentleWake == true) "${alarm.gentleSeconds}s" else "off"
        val detail = "sound=${alarm?.soundId} volume=${(RingMute.volume(volume) * FULL_PERCENT).toInt()}% " +
            "vibrate=$vibrate gentle=$gentle"
        diagnostics.record("sound_play", session?.plan?.alarmId, detail)
        sound.play(
            volume = volume,
            vibrate = vibrate,
            soundId = alarm?.soundId.takeUnless { silent },
            pattern = alarm?.vibrationPattern ?: VibrationPattern.Basic,
            gentleSeconds = if (alarm?.gentleWake == true) alarm.gentleSeconds else 0,
        )
    }

    private fun clock(millis: Long): String =
        CLOCK.format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

    private companion object {
        val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
        const val FULL_PERCENT = 100
    }
}
