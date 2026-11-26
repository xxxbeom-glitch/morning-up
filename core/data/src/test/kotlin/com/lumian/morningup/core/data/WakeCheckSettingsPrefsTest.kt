package com.lumian.morningup.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.data.db.AlarmEntity
import com.lumian.morningup.core.data.db.LegacyWakeCheck
import com.lumian.morningup.core.data.db.MorningDatabase
import com.lumian.morningup.core.data.db.toEntity
import com.lumian.morningup.core.data.settings.WakeCheckSettingsPrefs
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.LabSettings
import com.lumian.morningup.core.model.TrickSpeed
import com.lumian.morningup.core.model.WakeCheck
import com.lumian.morningup.core.model.WakeCheckSettings
import java.time.LocalTime
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** 다시 잠들기 방지 전체 설정 저장과 예전 알람마다 값 옮기기(결정 145). */
@RunWith(RobolectricTestRunner::class)
class WakeCheckSettingsPrefsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, MorningDatabase::class.java).allowMainThreadQueries().build()
    private val prefs = context.getSharedPreferences("wake_check_test", Context.MODE_PRIVATE)

    @After
    fun close() {
        db.close()
        prefs.edit().clear().commit()
    }

    private fun legacy(id: Long, on: Boolean, ask: Int, answer: Int): AlarmEntity =
        Alarm(id = id, time = LocalTime.of(6, 0), wakeCheck = WakeCheck(on)).toEntity()
            .copy(wakeCheckAskAfterMinutes = ask, wakeCheckAnswerSeconds = answer)

    @Test
    fun pickLegacy_takesMostUsedPair_amongOnes_thenEarliest() {
        val picked = WakeCheckSettingsPrefs.pickLegacy(
            listOf(
                legacy(3, on = true, ask = 10, answer = 180),
                legacy(1, on = true, ask = 5, answer = 30),
                legacy(2, on = true, ask = 10, answer = 180),
                legacy(4, on = false, ask = 3, answer = 60),
                legacy(5, on = false, ask = 3, answer = 60),
                legacy(6, on = false, ask = 3, answer = 60),
            ),
        )
        assertThat(picked).isEqualTo(10 to 180)
        // 같은 수면 먼저 만든 알람의 값이에요.
        val tie = WakeCheckSettingsPrefs.pickLegacy(
            listOf(legacy(2, on = true, ask = 10, answer = 30), legacy(1, on = true, ask = 5, answer = 180)),
        )
        assertThat(tie).isEqualTo(5 to 180)
    }

    @Test
    fun pickLegacy_outOfChoices_becomesDefault_andNoneOn_isNull() {
        assertThat(WakeCheckSettingsPrefs.pickLegacy(listOf(legacy(1, on = true, ask = 1, answer = 120))))
            .isEqualTo(WakeCheckSettings.DEFAULT_ASK_MINUTES to WakeCheckSettings.DEFAULT_ANSWER_SECONDS)
        assertThat(WakeCheckSettingsPrefs.pickLegacy(listOf(legacy(1, on = false, ask = 5, answer = 30)))).isNull()
    }

    @Test
    fun legacyWakeCheck_readsOldGlobalValues_forDbV5() {
        // ISS-055: 옛 판에서 '확인까지' 5분, 2번이었으면 그 값을 DB 옮기기에 넘겨요.
        prefs.edit().putBoolean("migrated_from_alarms", true).putInt("ask_after_minutes", 5)
            .putInt("answer_seconds", 30).putInt("recheck_wait_minutes", 5).putInt("recheck_count", 2).commit()
        assertThat(WakeCheckSettingsPrefs.legacyWakeCheck(prefs))
            // 결정 183: 대답할 시간과 확인까지는 고정값(1분, 3분)으로 넘기고, 다시 물어보기와 횟수는 옛 값 그대로예요.
            .isEqualTo(LegacyWakeCheck(5, 60, recheckEnabled = true, recheckWaitMinutes = 3, recheckCount = 2))
    }

    @Test
    fun legacyWakeCheck_freshPrefs_keepsAlarmAsk_andDefaults() {
        assertThat(WakeCheckSettingsPrefs.legacyWakeCheck(prefs)).isEqualTo(LegacyWakeCheck())
    }

    @Test
    fun migrate_runsOnce_andUpdatesKeepValues() = runTest {
        db.alarmDao().upsert(legacy(1, on = true, ask = 5, answer = 30))
        val store = WakeCheckSettingsPrefs(prefs, db.alarmDao())
        assertThat(store.current()).isEqualTo(WakeCheckSettings(askAfterMinutes = 5, answerSeconds = 30))

        store.update { it.copy(askAfterMinutes = 10, recheckEnabled = false) }
        // 다시 만들어도(앱을 다시 켜도) 옮기기는 다시 하지 않고 바꾼 값을 읽어요.
        val again = WakeCheckSettingsPrefs(prefs, db.alarmDao())
        assertThat(again.current())
            .isEqualTo(WakeCheckSettings(askAfterMinutes = 10, answerSeconds = 30, recheckEnabled = false))
    }

    @Test
    fun labSettings_startOff_andKeepAfterReopen() = runTest {
        val store = WakeCheckSettingsPrefs(prefs, db.alarmDao())
        assertThat(store.current().lab).isEqualTo(LabSettings())
        val lab = LabSettings(
            trickEnabled = true,
            trickSpeed = TrickSpeed.Slow,
            escapeHidden = true,
            debugMinuteMillis = 6_000L,
            recheckEnabled = true,
        )
        // 결정 184: 실험실 '한 번 더 확인'은 처음 꺼짐이고, 켜면 다시 열어도 켜져 있어요.
        assertThat(store.current().lab.recheckEnabled).isFalse()
        store.update { it.copy(lab = lab) }
        val again = WakeCheckSettingsPrefs(prefs, db.alarmDao())
        assertThat(again.current().lab).isEqualTo(lab)
    }

    @Test
    fun recheckWaitAndCount_keepAfterReopen() = runTest {
        val store = WakeCheckSettingsPrefs(prefs, db.alarmDao())
        assertThat(store.current().recheckWaitMinutes).isEqualTo(3)
        assertThat(store.current().recheckCount).isEqualTo(1)
        store.update { it.copy(recheckWaitMinutes = 1, recheckCount = 3) }
        val again = WakeCheckSettingsPrefs(prefs, db.alarmDao()).current()
        assertThat(again.recheckWaitMinutes).isEqualTo(1)
        assertThat(again.recheckCount).isEqualTo(3)
    }

    @Test
    fun oldValues_moveWaitFromAskAfter_andDropMultiAsk() = runTest {
        // 결정 176 전 저장: W3까지 = 다시 물어보기 5분, 확인할 시간 10분, 실험실 여러 번 묻기 5번.
        prefs.edit()
            .putInt("ask_after_minutes", 5)
            .putInt("recheck_minutes", 10)
            .putBoolean("lab_multi_ask", true)
            .putInt("lab_multi_ask_count", 5)
            .commit()
        val store = WakeCheckSettingsPrefs(prefs, db.alarmDao())
        val s = store.current()
        assertThat(s.recheckWaitMinutes).isEqualTo(5)
        assertThat(s.recheckCount).isEqualTo(1)
        assertThat(prefs.contains("lab_multi_ask")).isFalse()
        assertThat(prefs.contains("recheck_minutes")).isFalse()
    }

    @Test
    fun oldAskAfter10_becomesWait3() = runTest {
        prefs.edit().putInt("ask_after_minutes", 10).commit()
        val s = WakeCheckSettingsPrefs(prefs, db.alarmDao()).current()
        assertThat(s.askAfterMinutes).isEqualTo(10)
        assertThat(s.recheckWaitMinutes).isEqualTo(3)
    }
}
