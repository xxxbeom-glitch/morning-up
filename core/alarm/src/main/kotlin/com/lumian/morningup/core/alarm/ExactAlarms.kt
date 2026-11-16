package com.lumian.morningup.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import com.lumian.morningup.core.model.engine.AlarmScheduler
import com.lumian.morningup.core.model.engine.TimerKind
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AlarmManager 예약 한 곳. 정확한 알람 권한이 없거나 막히면 덜 정확한 방법으로 대신 예약하고 기록을 남겨요.
 * 대신 예약은 몇 분 늦을 수 있어서 아침 확인 목록에 넣었어요.
 */
@Singleton
class ExactAlarms @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmManager: AlarmManager,
    private val diagnostics: AlarmDiagnostics,
) {
    fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** 상태 표시줄 알람 아이콘이 뜨고 도즈(절전)도 뚫는 가장 강한 예약. */
    fun setAlarmClock(at: Long, operation: PendingIntent, alarmId: Long?) {
        if (canScheduleExact()) {
            try {
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(at, showIntent()), operation)
                return
            } catch (e: SecurityException) {
                diagnostics.record("exact_denied", alarmId, e.message.orEmpty())
            }
        }
        fallback(at, operation, alarmId)
    }

    /** 울리는 중 타이머(1분 울림, 59초 미션 등)용. 화면에 알람 아이콘을 띄우지 않아요. */
    fun setExact(at: Long, operation: PendingIntent) {
        if (canScheduleExact()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
                return
            } catch (e: SecurityException) {
                diagnostics.record("exact_denied", null, e.message.orEmpty())
            }
        }
        fallback(at, operation, null)
    }

    fun cancel(operation: PendingIntent) = alarmManager.cancel(operation)

    private fun fallback(at: Long, operation: PendingIntent, alarmId: Long?) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
        diagnostics.record("fallback_schedule", alarmId, "at=$at")
    }

    private fun showIntent(): PendingIntent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
}

/** 엔진의 [AlarmScheduler]를 AlarmManager로 구현해요. */
@Singleton
class AndroidAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exact: ExactAlarms,
    private val diagnostics: AlarmDiagnostics,
) : AlarmScheduler {
    override fun schedule(alarmId: Long, triggerAt: Long) {
        exact.setAlarmClock(triggerAt, AlarmIntents.firePendingIntent(context, alarmId), alarmId)
        diagnostics.record("scheduled", alarmId, "at=$triggerAt")
    }

    override fun cancel(alarmId: Long) {
        exact.cancel(AlarmIntents.firePendingIntent(context, alarmId))
        diagnostics.record("cancelled", alarmId)
    }
}

/**
 * 울리는 중 타이머 한 칸. 쉬는 시간 끝과 W2 묻기는 서비스가 멈춘 채 몇 분 기다렸다가 화면을 다시 띄워야 해서
 * 가장 강한 알람 시계 예약을 써요. 나머지(1분 울림, 59초 미션, W2 답할 시간)는 서비스가 떠 있는 동안이에요.
 */
@Singleton
class RingTimers @Inject constructor(
    @ApplicationContext private val context: Context,
    private val exact: ExactAlarms,
) {
    fun schedule(at: Long, kind: TimerKind) {
        val operation = AlarmIntents.timerPendingIntent(context, kind)
        if (kind in WAKE_UP_KINDS) exact.setAlarmClock(at, operation, null) else exact.setExact(at, operation)
    }

    private companion object {
        val WAKE_UP_KINDS = setOf(TimerKind.RestEnded, TimerKind.WakeCheckAsk)
    }

    fun cancel() = exact.cancel(AlarmIntents.timerPendingIntent(context, null))
}
