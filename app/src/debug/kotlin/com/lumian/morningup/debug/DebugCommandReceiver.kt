package com.lumian.morningup.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lumian.morningup.core.alarm.AlarmScope
import com.lumian.morningup.core.alarm.BatteryReminder
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ring.FlowLog
import com.lumian.morningup.core.data.AlarmRepository
import com.lumian.morningup.core.model.WakeCheckSettingsStore
import com.lumian.morningup.core.model.engine.AlarmRescheduler
import dagger.hilt.android.AndroidEntryPoint
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 디버그 빌드 전용 adb 입구(TSK-164). 앱 안 '테스트' 페이지 대신 Claude가 adb로 시험 알람과 배터리 알림을 걸어요.
 * 이 파일과 선언은 `app/src/debug`에만 있어서 출시 빌드에는 들어가지 않아요. 보낼 수 있는 쪽은 adb(shell)뿐이에요
 * (선언에 `android.permission.DUMP` 필요). 명령은 docs/qa/README.md에 있어요.
 */
@AndroidEntryPoint
class DebugCommandReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: AlarmRepository

    @Inject lateinit var rescheduler: AlarmRescheduler

    @Inject lateinit var now: NowProvider

    @Inject lateinit var batteryReminder: BatteryReminder

    @Inject lateinit var wakeSettings: WakeCheckSettingsStore

    @Inject
    @AlarmScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ALARM_CASE -> scheduleCase(intent)
            ACTION_BATTERY_REMINDER -> {
                val sent = batteryReminder.postNowForTest()
                val result = if (sent) "battery reminder posted" else "battery reminder blocked: notifications off"
                FlowLog.event("debug_battery_reminder", detail = result)
                resultData = result
            }
            ACTION_WAKE_CHECK -> setWakeCheck(intent)
            else -> resultData = "unknown action: ${intent.action}"
        }
    }

    private fun scheduleCase(intent: Intent) {
        val case = intent.getStringExtra(EXTRA_CASE)?.uppercase()
            ?.let { name -> AlarmTestCase.entries.firstOrNull { it.name == name } }
        if (case == null) {
            resultData = "case must be one of ${AlarmTestCase.entries.joinToString(",")}"
            return
        }
        val minutes = intent.getIntExtra(EXTRA_IN_MINUTES, 1).coerceIn(1, MAX_MINUTES)
        val pending = goAsync()
        scope.launch {
            try {
                val current = now.now()
                val time = current.toLocalTime().plusMinutes(minutes.toLong()).truncatedTo(ChronoUnit.MINUTES)
                val alarm = repository.insert(AlarmTestCases.alarmFor(case, time))
                rescheduler.reschedule(alarm, current)
                val result = "case ${case.name} alarm ${alarm.id} at $time"
                FlowLog.event("debug_case_scheduled", alarmId = alarm.id, detail = result)
                pending.resultData = result
            } finally {
                pending.finish()
            }
        }
    }

    /**
     * 다시 잠들기 방지 전체 설정을 바꿔요(결정 145). 시험을 빨리 하려고 화면에서 고를 수 없는 값(1분 등)도 받아요.
     * 빠진 값은 그대로 둬요. 예: `--ei ask_minutes 1 --ei answer_seconds 30 --ez recheck true --ei recheck_minutes 1`
     */
    private fun setWakeCheck(intent: Intent) {
        wakeSettings.update { old ->
            old.copy(
                askAfterMinutes = intent.intOr(EXTRA_ASK_MINUTES, old.askAfterMinutes),
                answerSeconds = intent.intOr(EXTRA_ANSWER_SECONDS, old.answerSeconds),
                recheckEnabled = if (intent.hasExtra(EXTRA_RECHECK)) {
                    intent.getBooleanExtra(EXTRA_RECHECK, old.recheckEnabled)
                } else {
                    old.recheckEnabled
                },
                recheckMinutes = intent.intOr(EXTRA_RECHECK_MINUTES, old.recheckMinutes),
            )
        }
        val result = "wake check ${wakeSettings.settings.value}"
        FlowLog.event("debug_wake_check", detail = result)
        resultData = result
    }

    private fun Intent.intOr(key: String, fallback: Int): Int =
        if (hasExtra(key)) getIntExtra(key, fallback).coerceIn(1, MAX_SECONDS) else fallback

    companion object {
        const val ACTION_ALARM_CASE = "com.lumian.morningup.debug.ALARM_CASE"
        const val ACTION_BATTERY_REMINDER = "com.lumian.morningup.debug.BATTERY_REMINDER"
        const val EXTRA_CASE = "case"
        const val EXTRA_IN_MINUTES = "in_minutes"
        const val ACTION_WAKE_CHECK = "com.lumian.morningup.debug.WAKE_CHECK"
        const val EXTRA_ASK_MINUTES = "ask_minutes"
        const val EXTRA_ANSWER_SECONDS = "answer_seconds"
        const val EXTRA_RECHECK = "recheck"
        const val EXTRA_RECHECK_MINUTES = "recheck_minutes"
        private const val MAX_SECONDS = 600
        private const val MAX_MINUTES = 60
    }
}
