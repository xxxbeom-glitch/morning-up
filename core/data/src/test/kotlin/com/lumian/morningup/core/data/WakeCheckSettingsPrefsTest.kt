package com.lumian.morningup.core.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.data.db.AlarmEntity
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
            multiAskEnabled = true,
            multiAskCount = 5,
            escapeHidden = true,
            debugMinuteMillis = 6_000L,
        )
        store.update { it.copy(lab = lab) }
        val again = WakeCheckSettingsPrefs(prefs, db.alarmDao())
        assertThat(again.current().lab).isEqualTo(lab)
        assertThat(again.current().recheckOn).isTrue()
    }
}
