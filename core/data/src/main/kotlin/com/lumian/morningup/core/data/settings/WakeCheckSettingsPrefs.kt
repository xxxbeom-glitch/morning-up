package com.lumian.morningup.core.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.lumian.morningup.core.data.db.AlarmDao
import com.lumian.morningup.core.data.db.AlarmEntity
import com.lumian.morningup.core.model.LabSettings
import com.lumian.morningup.core.model.TrickLevel
import com.lumian.morningup.core.model.TrickSpeed
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
 * 다시 잠들기 방지 전체 설정(결정 145). 재부팅 뒤 잠금을 풀기 전에도 울림이 읽어야 해서 기기 보호 저장소에 둬요.
 * 처음 한 번은 예전 알람마다 값을 옮겨요([migrate]).
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
        if (prefs.getBoolean(KEY_MIGRATED, false)) return@withLock
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

    private fun read() = WakeCheckSettings(
        askAfterMinutes = prefs.getInt(KEY_ASK, WakeCheckSettings.DEFAULT_ASK_MINUTES),
        answerSeconds = prefs.getInt(KEY_ANSWER, WakeCheckSettings.DEFAULT_ANSWER_SECONDS),
        recheckEnabled = prefs.getBoolean(KEY_RECHECK, true),
        recheckMinutes = prefs.getInt(KEY_RECHECK_MINUTES, WakeCheckSettings.DEFAULT_RECHECK_MINUTES),
        lab = LabSettings(
            trickEnabled = prefs.getBoolean(KEY_TRICK, false),
            trickLevel = TrickLevel.of(prefs.getString(KEY_TRICK_LEVEL, null)),
            trickSpeed = TrickSpeed.of(prefs.getString(KEY_TRICK_SPEED, null)),
            multiAskEnabled = prefs.getBoolean(KEY_MULTI, false),
            multiAskCount = prefs.getInt(KEY_MULTI_COUNT, LabSettings.DEFAULT_ASK_COUNT)
                .takeIf { it in LabSettings.ASK_COUNTS } ?: LabSettings.DEFAULT_ASK_COUNT,
            escapeHidden = prefs.getBoolean(KEY_ESCAPE_HIDDEN, false),
            debugMinuteMillis = prefs.getLong(KEY_DEBUG_MINUTE, LabSettings.MINUTE_MILLIS),
        ),
    )

    private fun write(settings: WakeCheckSettings) = prefs.edit {
        putInt(KEY_ASK, settings.askAfterMinutes)
        putInt(KEY_ANSWER, settings.answerSeconds)
        putBoolean(KEY_RECHECK, settings.recheckEnabled)
        putInt(KEY_RECHECK_MINUTES, settings.recheckMinutes)
        putBoolean(KEY_TRICK, settings.lab.trickEnabled)
        putString(KEY_TRICK_LEVEL, settings.lab.trickLevel.key)
        putString(KEY_TRICK_SPEED, settings.lab.trickSpeed.key)
        putBoolean(KEY_MULTI, settings.lab.multiAskEnabled)
        putInt(KEY_MULTI_COUNT, settings.lab.multiAskCount)
        putBoolean(KEY_ESCAPE_HIDDEN, settings.lab.escapeHidden)
        putLong(KEY_DEBUG_MINUTE, settings.lab.debugMinuteMillis)
    }

    companion object {
        private const val PREFS = "wake_check_settings"
        private const val KEY_ASK = "ask_after_minutes"
        private const val KEY_ANSWER = "answer_seconds"
        private const val KEY_RECHECK = "recheck_enabled"
        private const val KEY_RECHECK_MINUTES = "recheck_minutes"
        private const val KEY_MIGRATED = "migrated_from_alarms"

        // 실험실 '확실히 잠깨기'(결정 146)
        private const val KEY_TRICK = "lab_trick"
        private const val KEY_TRICK_LEVEL = "lab_trick_level"
        private const val KEY_TRICK_SPEED = "lab_trick_speed"
        private const val KEY_MULTI = "lab_multi_ask"
        private const val KEY_MULTI_COUNT = "lab_multi_ask_count"

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
