package com.lumian.morningup.core.alarm.ring

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.lumian.morningup.core.alarm.R
import com.lumian.morningup.core.model.engine.MissionEscape
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import javax.inject.Inject
import javax.inject.Singleton

/** 지금 알림에 붙일 미션 비상구 내용. [wrong]이면 첫 줄이 '조금 달라요'예요. */
data class EscapeView(val index: Int, val sentence: String, val wrong: Boolean)

/**
 * 미션 비상구 상태(결정 149~151). 지금 예문, 바로 전 예문, '틀림' 표시, 이번 달 쓴 횟수.
 * 재부팅 직후에도 읽도록 기기 보호 저장소에 둬요. 쓴 글 자체는 어디에도 남기지 않아요.
 * 알림을 그리는 [RingNotifications]와 바꾸는 [MissionEscapeNotifier]가 같이 써요.
 */
@Singleton
class MissionEscapeStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs: SharedPreferences by lazy {
        context.createDeviceProtectedStorageContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /**
     * 따라 쓸 예문. 시험하는 동안은 [TEST_SENTENCE] 하나예요(결정 172). 출시 전에 `R.array.escape_sentences`
     * 40~60자 예문 20개(결정 150)로 되돌려요.
     */
    @Suppress("UnusedPrivateProperty")
    private val releaseSentences: Array<String> get() = context.resources.getStringArray(R.array.escape_sentences)

    private val sentences: Array<String> get() = arrayOf(MissionEscape.TEST_SENTENCE)

    /** 지금 알림에 붙일 내용. 비상구가 닫혀 있으면 null이에요. */
    fun current(): EscapeView? {
        val index = prefs.getInt(KEY_CURRENT, NONE)
        val sentence = sentences.getOrNull(index) ?: return null
        return EscapeView(index, sentence, prefs.getBoolean(KEY_WRONG, false))
    }

    /** 바로 전 예문을 빼고 새로 골라 열어요(4절). */
    fun openFresh(): Int {
        val all = sentences
        val previous = prefs.getInt(KEY_LAST, NONE).takeIf { it in all.indices }
        return MissionEscape.pick(all.size, previous).also { picked ->
            prefs.edit {
                putInt(KEY_CURRENT, picked)
                putInt(KEY_LAST, picked)
                putBoolean(KEY_WRONG, false)
            }
        }
    }

    fun markWrong() = prefs.edit { putBoolean(KEY_WRONG, true) }

    fun close() = prefs.edit {
        putInt(KEY_CURRENT, NONE)
        putBoolean(KEY_WRONG, false)
    }

    /** 써서 끈 횟수를 이번 달 몫으로 하나 세고 '이번 달 n번째'를 돌려줘요(7절). 달이 바뀌면 1부터 다시 세요. */
    fun countUse(now: TemporalAccessor): Pair<String, Int> {
        val month = MONTH.format(now)
        val count = if (prefs.getString(KEY_MONTH, null) == month) prefs.getInt(KEY_MONTH_COUNT, 0) + 1 else 1
        prefs.edit {
            putString(KEY_MONTH, month)
            putInt(KEY_MONTH_COUNT, count)
        }
        return month to count
    }

    private companion object {
        const val PREFS = "mission_escape"
        const val KEY_CURRENT = "current_sentence"
        const val KEY_LAST = "last_sentence"
        const val KEY_WRONG = "wrong"
        const val KEY_MONTH = "month"
        const val KEY_MONTH_COUNT = "month_count"
        const val NONE = -1
        val MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
    }
}
