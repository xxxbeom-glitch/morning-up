package com.lumian.morningup.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lumian.morningup.core.alarm.AlarmScope
import com.lumian.morningup.core.alarm.BatteryReminder
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ring.FlowLog
import com.lumian.morningup.core.alarm.ring.MissionEscapeAnswers
import com.lumian.morningup.core.alarm.ring.MissionEscapeNotifier
import com.lumian.morningup.core.data.AlarmRepository
import com.lumian.morningup.core.model.LabSettings
import com.lumian.morningup.core.model.TrickSpeed
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

    @Inject lateinit var escapeAnswers: MissionEscapeAnswers

    @Inject lateinit var escapeNotifier: MissionEscapeNotifier

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
            ACTION_LAB -> setLab(intent)
            ACTION_ESCAPE -> sendEscape(intent)
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

    /**
     * '확실히 잠깨기' 실험실 값을 바꿔요(결정 146, TSK-172). 빠진 값은 그대로 둬요.
     * 예: `--ez trick true --es speed fast --ez multi true --ei count 3 --ei minute_seconds 6`
     * `--ez escape_hidden true`는 미션 비상구 알림 숨기기예요(결정 151, TSK-182).
     * `minute_seconds`는 여러 번 묻기의 1분을 몇 초로 셀지(시험 배율, 1~60)예요. 60이면 실제 시간이에요.
     */
    private fun setLab(intent: Intent) {
        wakeSettings.update { old ->
            val lab = old.lab
            val minuteSeconds = if (intent.hasExtra(EXTRA_MINUTE_SECONDS)) {
                intent.getIntExtra(EXTRA_MINUTE_SECONDS, SECONDS_PER_MINUTE).coerceIn(1, SECONDS_PER_MINUTE)
            } else {
                (lab.debugMinuteMillis / MILLIS_PER_SECOND).toInt()
            }
            old.copy(
                lab = lab.copy(
                    trickEnabled = intent.boolOr(EXTRA_TRICK, lab.trickEnabled),
                    trickSpeed = intent.getStringExtra(EXTRA_SPEED)?.let { TrickSpeed.of(it) } ?: lab.trickSpeed,
                    multiAskEnabled = intent.boolOr(EXTRA_MULTI, lab.multiAskEnabled),
                    multiAskCount = intent.getIntExtra(EXTRA_COUNT, lab.multiAskCount)
                        .takeIf { it in LabSettings.ASK_COUNTS } ?: lab.multiAskCount,
                    escapeHidden = intent.boolOr(EXTRA_ESCAPE_HIDDEN, lab.escapeHidden),
                    debugMinuteMillis = minuteSeconds * MILLIS_PER_SECOND,
                ),
            )
        }
        val result = "lab ${wakeSettings.settings.value.lab}"
        FlowLog.event("debug_lab", detail = result)
        resultData = result
    }

    /**
     * 미션 비상구 바로 답장 대신 글을 보내요(결정 149 8절 2번, 바로 답장은 adb로 보낼 수 없어요). 알림 답장과 같은 길이에요.
     * `--es answer ok`는 지금 예문 그대로, `wrong`은 끝에 마침표 하나를 더한 글, 그 밖의 글은 그대로 보내요.
     * 예: `--es answer ok`
     */
    private fun sendEscape(intent: Intent) {
        val answer = intent.getStringExtra(EXTRA_ANSWER) ?: ANSWER_OK
        val pending = goAsync()
        scope.launch {
            try {
                val sentence = escapeNotifier.currentSentence
                val text = when (answer) {
                    ANSWER_OK -> sentence
                    ANSWER_WRONG -> sentence?.let { "$it." }
                    else -> answer
                }
                val result = "escape ${escapeAnswers.answer(text)}"
                FlowLog.event("debug_escape", detail = result)
                pending.resultData = result
            } finally {
                pending.finish()
            }
        }
    }

    private fun Intent.boolOr(key: String, fallback: Boolean): Boolean =
        if (hasExtra(key)) getBooleanExtra(key, fallback) else fallback

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
        const val ACTION_LAB = "com.lumian.morningup.debug.LAB"
        const val EXTRA_TRICK = "trick"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_MULTI = "multi"
        const val EXTRA_COUNT = "count"
        const val EXTRA_MINUTE_SECONDS = "minute_seconds"
        const val EXTRA_ESCAPE_HIDDEN = "escape_hidden"
        const val ACTION_ESCAPE = "com.lumian.morningup.debug.ESCAPE"
        const val EXTRA_ANSWER = "answer"
        private const val ANSWER_OK = "ok"
        private const val ANSWER_WRONG = "wrong"
        private const val SECONDS_PER_MINUTE = 60
        private const val MILLIS_PER_SECOND = 1_000L
        private const val MAX_SECONDS = 600
        private const val MAX_MINUTES = 60
    }
}
