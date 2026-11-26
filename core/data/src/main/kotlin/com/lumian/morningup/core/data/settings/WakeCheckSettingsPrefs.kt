package com.lumian.morningup.core.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.lumian.morningup.core.data.db.AlarmDao
import com.lumian.morningup.core.data.db.AlarmEntity
import com.lumian.morningup.core.model.LabSettings
import com.lumian.morningup.core.model.TrickSpeed
import com.lumian.morningup.core.model.WakeCheck
import com.lumian.morningup.core.model.WakeCheckSettings
import com.lumian.morningup.core.model.WakeCheckSettingsStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 앱 전체 설정(실험실, 옛 다시 잠들기 방지 전체 값). 재부팅 뒤 잠금을 풀기 전에도 울림이 읽어야 해서 기기 보호 저장소에 둬요.
 * [migrate]가 옮기기 두 가지를 한 번씩 해요. 결정 145 때 예전 알람마다 값을 전체로 모으고, 결정 180에 전체 값을 다시
 * 모든 알람으로 나눠요. 그 뒤로 W2, W3 값은 알람 것만 써요.
 */
@Singleton
class WakeCheckSettingsPrefs internal constructor(
    private val prefs: SharedPreferences,
    private val dao: AlarmDao,
) : WakeCheckSettingsStore {
    @Inject constructor(@ApplicationContext context: Context, dao: AlarmDao) : this(
        context.createDeviceProtectedStorageContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE),
        dao,
    )

    private val mutex = Mutex()
    private val state = MutableStateFlow(read())

    override val settings: StateFlow<WakeCheckSettings> = state.asStateFlow()

    override suspend fun current(): WakeCheckSettings {
        migrate()
        return state.value
    }

    override fun update(transform: (WakeCheckSettings) -> WakeCheckSettings) {
        synchronized(this) {
            val next = transform(state.value)
            write(next)
            state.value = next
        }
    }

    /**
     * 예전에는 알람마다 '다시 물어보기'와 '대답할 시간'이 있었어요. 다시 잠들기 방지가 켜진 알람 중 가장 많이 쓴
     * (간격, 시간) 쌍을 전체 설정으로 옮겨요. 같으면 먼저 만든 알람(id가 작은 것)의 값이에요. 지금 고를 수 없는 값은
     * 처음 값으로 바꿔요. 한 번만 하고, 알람이 없거나 다 꺼져 있으면 처음 값 그대로예요.
     */
    suspend fun migrate() = mutex.withLock {
        migrateFromAlarms()
        migrateToAlarms()
    }

    private suspend fun migrateFromAlarms() {
        if (prefs.getBoolean(KEY_MIGRATED, false)) return
        val picked = pickLegacy(dao.getAll())
        synchronized(this) {
            val next = if (picked != null && !prefs.contains(KEY_ASK)) {
                state.value.copy(askAfterMinutes = picked.first, answerSeconds = picked.second)
            } else {
                state.value
            }
            write(next)
            prefs.edit { putBoolean(KEY_MIGRATED, true) }
            state.value = next
        }
    }

    /**
     * 결정 180: 전체 설정의 W2, W3 값을 지금 있는 모든 알람에 옮겨요. 한 번만 하고, 그 뒤 새 알람은 처음 값으로 시작해요.
     * 옮긴 값은 고를 수 있는 값으로 맞춰요.
     */
    private suspend fun migrateToAlarms() {
        if (prefs.getBoolean(KEY_PER_ALARM, false)) return
        val values = WakeCheck.valuesOf(state.value, enabled = false)
        dao.applyWakeCheckToAll(
            ask = values.askAfterMinutes,
            answer = values.answerSeconds,
            recheck = values.recheckEnabled,
            wait = values.recheckWaitMinutes,
            count = values.recheckCount,
        )
        prefs.edit { putBoolean(KEY_PER_ALARM, true) }
    }

    private fun read() = WakeCheckSettings(
        askAfterMinutes = prefs.getInt(KEY_ASK, WakeCheckSettings.DEFAULT_ASK_MINUTES),
        answerSeconds = prefs.getInt(KEY_ANSWER, WakeCheckSettings.DEFAULT_ANSWER_SECONDS),
        recheckEnabled = prefs.getBoolean(KEY_RECHECK, true),
        recheckWaitMinutes = prefs.getInt(KEY_RECHECK_WAIT, legacyRecheckWait())
            .takeIf { it in WakeCheckSettings.RECHECK_WAIT_MINUTES } ?: WakeCheckSettings.DEFAULT_RECHECK_WAIT_MINUTES,
        recheckCount = prefs.getInt(KEY_RECHECK_COUNT, WakeCheckSettings.DEFAULT_RECHECK_COUNT)
            .takeIf { it in WakeCheckSettings.RECHECK_COUNTS } ?: WakeCheckSettings.DEFAULT_RECHECK_COUNT,
        lab = LabSettings(
            trickEnabled = prefs.getBoolean(KEY_TRICK, false),
            trickSpeed = TrickSpeed.of(prefs.getString(KEY_TRICK_SPEED, null)),
            escapeHidden = prefs.getBoolean(KEY_ESCAPE_HIDDEN, false),
            debugMinuteMillis = prefs.getLong(KEY_DEBUG_MINUTE, LabSettings.MINUTE_MILLIS),
        ),
    )

    /**
     * 결정 176 전에는 W3까지 기다리는 시간이 W2 '다시 물어보기'와 같았어요. 새 값이 없으면 그 값을 옮기고,
     * 고를 수 없는 값(10분)은 처음 값 3분이에요.
     */
    private fun legacyRecheckWait(): Int = prefs.getInt(KEY_ASK, WakeCheckSettings.DEFAULT_RECHECK_WAIT_MINUTES)

    private fun write(settings: WakeCheckSettings) = prefs.edit {
        putInt(KEY_ASK, settings.askAfterMinutes)
        putInt(KEY_ANSWER, settings.answerSeconds)
        putBoolean(KEY_RECHECK, settings.recheckEnabled)
        putInt(KEY_RECHECK_WAIT, settings.recheckWaitMinutes)
        putInt(KEY_RECHECK_COUNT, settings.recheckCount)
        putBoolean(KEY_TRICK, settings.lab.trickEnabled)
        putString(KEY_TRICK_SPEED, settings.lab.trickSpeed.key)
        // 없앤 값(W3 확인할 시간, 실험실 여러 번 묻기)은 옮기지 않고 지워요(결정 176, 177).
        OBSOLETE_KEYS.forEach { remove(it) }
        putBoolean(KEY_ESCAPE_HIDDEN, settings.lab.escapeHidden)
        putLong(KEY_DEBUG_MINUTE, settings.lab.debugMinuteMillis)
    }

    companion object {
        private const val PREFS = "wake_check_settings"
        private const val KEY_ASK = "ask_after_minutes"
        private const val KEY_ANSWER = "answer_seconds"
        private const val KEY_RECHECK = "recheck_enabled"
        private const val KEY_RECHECK_WAIT = "recheck_wait_minutes"
        private const val KEY_RECHECK_COUNT = "recheck_count"
        private const val KEY_MIGRATED = "migrated_from_alarms"

        /** 결정 180: 전체 W2, W3 값을 모든 알람에 옮겼는지. */
        private const val KEY_PER_ALARM = "migrated_to_alarms"

        // 실험실 '확실히 잠깨기'(결정 146)
        private const val KEY_TRICK = "lab_trick"
        private const val KEY_TRICK_SPEED = "lab_trick_speed"

        /** 예전 저장 이름. 읽지 않고 쓸 때 지워요. */
        private val OBSOLETE_KEYS = listOf("recheck_minutes", "lab_multi_ask", "lab_multi_ask_count")

        /** 미션 비상구 알림 숨기기(결정 151). */
        private const val KEY_ESCAPE_HIDDEN = "lab_escape_hidden"

        /** 시험용 1분 길이. 디버그 adb 명령만 바꿔요. */
        private const val KEY_DEBUG_MINUTE = "lab_debug_minute_millis"

        /** 옮길 (간격, 시간). 다시 잠들기 방지가 켜진 알람이 없으면 null. */
        internal fun pickLegacy(alarms: List<AlarmEntity>): Pair<Int, Int>? {
            val pairs = alarms.filter { it.wakeCheckEnabled }
                .sortedBy { it.id }
                .map { it.wakeCheckAskAfterMinutes to it.wakeCheckAnswerSeconds }
            if (pairs.isEmpty()) return null
            val counts = pairs.groupingBy { it }.eachCount()
            val top = counts.values.max()
            val (ask, answer) = pairs.first { counts.getValue(it) == top }
            return Pair(
                ask.takeIf { it in WakeCheckSettings.ASK_MINUTES } ?: WakeCheckSettings.DEFAULT_ASK_MINUTES,
                answer.takeIf { it in WakeCheckSettings.ANSWER_SECONDS } ?: WakeCheckSettings.DEFAULT_ANSWER_SECONDS,
            )
        }
    }
}
