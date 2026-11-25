package com.lumian.morningup.feature.mission.play

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.model.mission.DropWordHistory
import com.lumian.morningup.core.model.mission.DropWordUse
import com.lumian.morningup.core.model.mission.RecentDropWords
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Drop Words 최근 단어를 기기에 남겨요(TSK-205). 잠금을 풀기 전에 울려도 읽고 쓰도록 기기 보호 저장소예요.
 * 한 줄에 `날짜<TAB>단계<TAB>단어` 하나씩, 사흘이 지난 줄은 쓸 때 지워요. 단어만 남고 사용자 입력은 남기지 않아요.
 */
@Singleton
class PrefsDropWordHistory @Inject constructor(
    @ApplicationContext private val context: Context,
    private val now: NowProvider,
) : DropWordHistory {
    private val prefs: SharedPreferences by lazy {
        context.createDeviceProtectedStorageContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private fun today(): LocalDate = now.now().toLocalDate()

    @Synchronized
    override fun recent(level: Int): List<String> = RecentDropWords.recent(read(), level, today())

    @Synchronized
    override fun record(level: Int, word: String) {
        val uses = RecentDropWords.add(read(), DropWordUse(today(), level, word))
        prefs.edit { putString(KEY_USES, uses.joinToString(LINE) { "${it.date}$FIELD${it.level}$FIELD${it.word}" }) }
    }

    private fun read(): List<DropWordUse> = prefs.getString(KEY_USES, null)
        ?.split(LINE)
        ?.mapNotNull { line ->
            val parts = line.split(FIELD)
            val date = parts.getOrNull(0)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val level = parts.getOrNull(1)?.toIntOrNull()
            val word = parts.getOrNull(2)
            if (date != null && level != null && !word.isNullOrEmpty()) DropWordUse(date, level, word) else null
        }
        .orEmpty()

    private companion object {
        const val PREFS = "drop_word_history"
        const val KEY_USES = "uses"
        const val LINE = "\n"
        const val FIELD = "\t"
    }
}
