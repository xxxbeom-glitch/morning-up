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
import com.lumian.morningup.core.designsystem.theme.ThemeMode
import com.lumian.morningup.core.designsystem.theme.ThemeModeStore
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
            ACTION_THEME -> setTheme(context, intent)
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
        // `--ez silent true`면 알람음 없이 걸어요(TSK-194, Z플립5 시험은 소리 없이).
        val silent = intent.getBooleanExtra(EXTRA_SILENT, false)
        // `--ei volume N`(0~100)이면 알람 벨소리 볼륨을 그 값으로. silent보다 먼저예요(TSK-250).
        val volume = if (intent.hasExtra(EXTRA_VOLUME)) intent.getIntExtra(EXTRA_VOLUME, 100).coerceIn(0, 100) else null
        val pending = goAsync()
        scope.launch {
            try {
                val current = now.now()
                val time = current.toLocalTime().plusMinutes(minutes.toLong()).truncatedTo(ChronoUnit.MINUTES)
                val alarm = repository.insert(AlarmTestCases.alarmFor(case, time, silent, volume))
                rescheduler.reschedule(alarm, current)
                val result = "case ${case.name} alarm ${alarm.id} at $time" + when {
                    volume != null -> " volume=$volume%"
                    silent -> " silent"
                    else -> ""
                }
                FlowLog.event("debug_case_scheduled", alarmId = alarm.id, detail = result)
                pending.resultData = result
            } finally {
                pending.finish()
            }
        }
    }

    /**
     * 다시 잠들기 방지 값을 모든 알람에 넣어요(결정 180부터 알람마다). 켜고 끄기는 알람 것 그대로예요.
     * 고를 수 없는 값은 울릴 때 처음 값으로 바뀌어요. 빠진 값은 그대로 둬요.
     * 예: `--ei ask_minutes 5 --ei answer_seconds 30 --ez recheck true --ei recheck_wait 1 --ei recheck_count 2`
     * 시간을 줄여 시험하려면 lab의 `minute_seconds`를 같이 써요.
     */
    private fun setWakeCheck(intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                val alarms = repository.all()
                alarms.forEach { alarm ->
                    val old = alarm.wakeCheck
                    repository.save(
                        alarm.copy(
                            wakeCheck = old.copy(
                                askAfterMinutes = intent.intOr(EXTRA_ASK_MINUTES, old.askAfterMinutes),
                                answerSeconds = intent.intOr(EXTRA_ANSWER_SECONDS, old.answerSeconds),
                                recheckEnabled = intent.boolOr(EXTRA_RECHECK, old.recheckEnabled),
                                recheckWaitMinutes = intent.intOr(EXTRA_RECHECK_WAIT, old.recheckWaitMinutes),
                                recheckCount = intent.intOr(EXTRA_RECHECK_COUNT, old.recheckCount),
                            ),
                        ),
                    )
                }
                val result = "wake check ${alarms.size} alarms " + repository.all().map { it.id to it.wakeCheck }
                FlowLog.event("debug_wake_check", detail = result)
                pending.resultData = result
            } finally {
                pending.finish()
            }
        }
    }

    /**
     * '확실히 잠깨기' 실험실 값을 바꿔요(결정 146, TSK-172). 빠진 값은 그대로 둬요.
     * 예: `--ez trick true --es speed 6 --ei minute_seconds 6`
     * `--ez escape_hidden true`는 미션 비상구 알림 숨기기예요(결정 151, TSK-182).
     * `minute_seconds`는 W3 기다리는 시간과 대답할 시간의 1분을 몇 초로 셀지(시험 배율, 1~60)예요. 60이면 실제 시간이에요.
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

    /**
     * 화면 모드를 바꿔요(TSK-270). 예전 설정의 '화면 모드(임시)' 줄 대신이에요. 열려 있는 화면에도 바로 적용돼요.
     * 예: `--es mode dark` (auto, light, dark)
     */
    private fun setTheme(context: Context, intent: Intent) {
        val raw = intent.getStringExtra(EXTRA_MODE).orEmpty()
        val mode = ThemeMode.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
        val result = if (mode == null) {
            "theme unknown mode: $raw (auto, light, dark)"
        } else {
            ThemeModeStore.set(context, mode)
            "theme $mode"
        }
        FlowLog.event("debug_theme", detail = result)
        resultData = result
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
        const val EXTRA_SILENT = "silent"
        const val EXTRA_VOLUME = "volume"
        const val ACTION_WAKE_CHECK = "com.lumian.morningup.debug.WAKE_CHECK"
        const val EXTRA_ASK_MINUTES = "ask_minutes"
        const val EXTRA_ANSWER_SECONDS = "answer_seconds"
        const val EXTRA_RECHECK = "recheck"
        const val EXTRA_RECHECK_WAIT = "recheck_wait"
        const val EXTRA_RECHECK_COUNT = "recheck_count"
        const val ACTION_LAB = "com.lumian.morningup.debug.LAB"
        const val EXTRA_TRICK = "trick"
        const val EXTRA_SPEED = "speed"
        const val EXTRA_MINUTE_SECONDS = "minute_seconds"
        const val EXTRA_ESCAPE_HIDDEN = "escape_hidden"
        const val ACTION_ESCAPE = "com.lumian.morningup.debug.ESCAPE"
        const val EXTRA_ANSWER = "answer"
        const val ACTION_THEME = "com.lumian.morningup.debug.THEME"
        const val EXTRA_MODE = "mode"
        private const val ANSWER_OK = "ok"
        private const val ANSWER_WRONG = "wrong"
        private const val SECONDS_PER_MINUTE = 60
        private const val MILLIS_PER_SECOND = 1_000L
        private const val MAX_SECONDS = 600
        private const val MAX_MINUTES = 60
    }
}
