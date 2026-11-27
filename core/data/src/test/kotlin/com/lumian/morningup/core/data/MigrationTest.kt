package com.lumian.morningup.core.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.data.db.Migration1To2
import com.lumian.morningup.core.data.db.Migration2To3
import com.lumian.morningup.core.data.db.LegacyWakeCheck
import com.lumian.morningup.core.data.db.Migration3To4
import com.lumian.morningup.core.data.db.Migration4To5
import com.lumian.morningup.core.data.db.Migration5To6
import com.lumian.morningup.core.data.db.Migration6To7
import com.lumian.morningup.core.data.db.MorningDatabase
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.VibrationPattern
import com.lumian.morningup.core.model.WakeCheck
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** v1에 저장한 알람이 v2로 옮긴 뒤에도 미션과 함께 그대로 남는지(사용자 알람이 사라지면 안 돼요). */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), MorningDatabase::class.java)

    @Test
    fun v1ToV2_keepsAlarms_andTurnsMissionNamesIntoSettings() = runTest {
        helper.createDatabase(dbName, 1).apply {
            execSQL(insertV1(id = 1, missions = "QuickMath,DropWords"))
            execSQL(insertV1(id = 2, missions = ""))
            execSQL(insertV1(id = 3, missions = "OldMission,ShakeShake"))
            close()
        }
        helper.runMigrationsAndValidate(dbName, 2, true, Migration1To2).close()

        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MorningDatabase::class.java,
            dbName,
        ).apply { MorningDatabase.migrations().forEach { addMigrations(it) } }.allowMainThreadQueries().build()
        val alarms = AlarmRepository(db.alarmDao()).all().associateBy { it.id }
        db.close()

        assertThat(alarms.keys).containsExactly(1L, 2L, 3L)
        assertThat(alarms.getValue(1).missions).containsExactly(
            MissionSetting.default(MissionType.QuickMath),
            MissionSetting.default(MissionType.DropWords),
        ).inOrder()
        assertThat(alarms.getValue(2).missions).isEmpty()
        assertThat(alarms.getValue(3).missions).containsExactly(MissionSetting.default(MissionType.ShakeShake))
        // 다른 칸은 손대지 않아요.
        assertThat(alarms.getValue(1).time.hour).isEqualTo(6)
        assertThat(alarms.getValue(1).wakeCheck.enabled).isTrue()
        assertThat(alarms.getValue(1).nextTriggerAt).isEqualTo(1_800_000_000_000)
    }

    @Test
    fun v2ToV3_addsVibrationPatternAndGentleSeconds_withDefaults() = runTest {
        helper.createDatabase(dbName, 1).apply {
            execSQL(insertV1(id = 7, missions = "QuickMath"))
            close()
        }
        helper.runMigrationsAndValidate(dbName, 2, true, Migration1To2).close()
        helper.runMigrationsAndValidate(dbName, 3, true, Migration2To3).close()

        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MorningDatabase::class.java,
            dbName,
        ).apply { MorningDatabase.migrations().forEach { addMigrations(it) } }.allowMainThreadQueries().build()
        val alarm = AlarmRepository(db.alarmDao()).all().single()
        db.close()

        assertThat(alarm.id).isEqualTo(7L)
        assertThat(alarm.vibrationPattern).isEqualTo(VibrationPattern.Basic)
        assertThat(alarm.gentleSeconds).isEqualTo(30)
        assertThat(alarm.missionTypes).containsExactly(MissionType.QuickMath)
    }

    @Test
    fun v3ToV4_turnsSilentIntoRandom_andKeepsOtherSounds() = runTest {
        helper.createDatabase(dbName, 1).apply {
            execSQL(insertV1(id = 1, missions = "", sound = "silent"))
            execSQL(insertV1(id = 2, missions = "", sound = "douceur"))
            execSQL(insertV1(id = 3, missions = ""))
            close()
        }
        helper.runMigrationsAndValidate(dbName, 2, true, Migration1To2).close()
        helper.runMigrationsAndValidate(dbName, 3, true, Migration2To3).close()
        helper.runMigrationsAndValidate(dbName, 4, true, Migration3To4).close()

        val db = Room.databaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MorningDatabase::class.java,
            dbName,
        ).apply { MorningDatabase.migrations().forEach { addMigrations(it) } }.allowMainThreadQueries().build()
        val alarms = AlarmRepository(db.alarmDao()).all().associateBy { it.id }
        db.close()

        assertThat(alarms.getValue(1).soundId).isEqualTo(Alarm.RANDOM_SOUND_ID)
        assertThat(alarms.getValue(2).soundId).isEqualTo("douceur")
        assertThat(alarms.getValue(3).soundId).isEqualTo(Alarm.DEFAULT_SOUND_ID)
    }

    /** v4까지 올린 DB(알람 W2 칸은 1분, 60초). */
    private fun createV4(vararg ids: Long) {
        helper.createDatabase(dbName, 1).apply {
            ids.forEach { execSQL(insertV1(id = it, missions = "")) }
            close()
        }
        helper.runMigrationsAndValidate(dbName, 2, true, Migration1To2).close()
        helper.runMigrationsAndValidate(dbName, 3, true, Migration2To3).close()
        helper.runMigrationsAndValidate(dbName, 4, true, Migration3To4).close()
    }

    private fun openApp(legacy: LegacyWakeCheck) = Room.databaseBuilder(
        ApplicationProvider.getApplicationContext(),
        MorningDatabase::class.java,
        dbName,
    ).apply { MorningDatabase.migrations { legacy }.forEach { addMigrations(it) } }.allowMainThreadQueries().build()

    @Test
    fun v4ToV5_putsOldGlobalValuesIntoEveryAlarm_andSurvivesReadThenSave() = runTest {
        // ISS-055: 옛 판(전체 '확인까지' 5분) 위에 새 판을 깔면 모든 알람에 5분이 들어가야 해요.
        createV4(1, 2)
        val legacy = LegacyWakeCheck(5, 30, recheckEnabled = true, recheckWaitMinutes = 5, recheckCount = 2)
        helper.runMigrationsAndValidate(dbName, 5, true, Migration4To5 { legacy }).close()

        val db = openApp(LegacyWakeCheck())
        val repo = AlarmRepository(db.alarmDao())
        // 앱으로 열면 v6까지 옮겨서 대답할 시간은 1분, 확인까지는 3분으로 고정돼요(결정 183).
        val expected = WakeCheck(true, 5, 60, recheckEnabled = true, recheckWaitMinutes = 3, recheckCount = 2)
        assertThat(repo.all().map { it.wakeCheck }).containsExactly(expected, expected)
        // 앱을 열 때 다시 맞추기(rescheduleAll)처럼 읽은 알람을 통째로 다시 써도 값은 그대로예요.
        repo.all().forEach { repo.save(it.copy(nextTriggerAt = 1L)) }
        assertThat(repo.all().map { it.wakeCheck }).containsExactly(expected, expected)
        db.close()
    }

    @Test
    fun v4ToV5_throughAppBuilder_migratesBeforeFirstRead() = runTest {
        // 앱처럼 빌더로 바로 열어도 첫 읽기 전에 옮기기가 끝나요.
        createV4(3)
        val db = openApp(LegacyWakeCheck(10, 180, recheckEnabled = false, recheckWaitMinutes = 1, recheckCount = 3))
        val check = AlarmRepository(db.alarmDao()).all().single().wakeCheck
        db.close()
        assertThat(check)
            // 결정 186: v6 → v7에서 옛 '안 함'은 횟수 1이 돼요.
            .isEqualTo(WakeCheck(true, 10, 60, recheckEnabled = true, recheckWaitMinutes = 3, recheckCount = 1))
    }

    @Test
    fun v4ToV5_beforeDecision145_keepsAlarmAskValues() = runTest {
        // 전체 값으로 모은 적 없는 판이면 W2 칸은 알람 것 그대로 두고 W3 값만 넣어요.
        createV4(4)
        val legacy = LegacyWakeCheck(recheckWaitMinutes = 5)
        val db = helper.runMigrationsAndValidate(dbName, 5, true, Migration4To5 { legacy })
        val row = db.query("SELECT wakeCheckAskAfterMinutes, wakeCheckAnswerSeconds, recheckWaitMinutes FROM alarms")
            .use { c ->
                c.moveToFirst()
                listOf(c.getInt(0), c.getInt(1), c.getInt(2))
            }
        db.close()
        assertThat(row).containsExactly(1, 60, 5).inOrder()
    }

    @Test
    fun v5ToV6_fixesOldAnswerAndWait_keepsRecheckOnOffAndCount() = runTest {
        // 결정 183: 대답할 시간 30초, 3분 → 1분 / 확인까지 1분, 5분 → 3분 / 한 번 더 확인 끔 → '안 함'(끔 그대로), 켬 → 횟수 그대로.
        createV4(1, 2, 3)
        helper.runMigrationsAndValidate(dbName, 5, true, Migration4To5 { LegacyWakeCheck() }).apply {
            execSQL("UPDATE alarms SET wakeCheckAnswerSeconds = 30, recheckWaitMinutes = 1, recheckEnabled = 1, recheckCount = 2 WHERE id = 1")
            execSQL("UPDATE alarms SET wakeCheckAnswerSeconds = 180, recheckWaitMinutes = 5, recheckEnabled = 0, recheckCount = 3 WHERE id = 2")
            execSQL("UPDATE alarms SET wakeCheckAnswerSeconds = 60, recheckWaitMinutes = 3, recheckEnabled = 1, recheckCount = 1 WHERE id = 3")
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 6, true, Migration5To6)
        val rows = db.query(
            "SELECT id, wakeCheckAnswerSeconds, recheckWaitMinutes, recheckEnabled, recheckCount FROM alarms ORDER BY id",
        ).use { c ->
            buildList {
                while (c.moveToNext()) add(List(5) { c.getInt(it) })
            }
        }
        db.close()
        assertThat(rows).containsExactly(
            listOf(1, 60, 3, 1, 2),
            listOf(2, 60, 3, 0, 3),
            listOf(3, 60, 3, 1, 1),
        ).inOrder()
    }

    @Test
    fun v6ToV7_turnsOldNoneIntoOne_keepsOnCounts() = runTest {
        // 결정 186: '안 함'(끔) → 횟수 1(켬), 켬이면 횟수 그대로(최대 없음).
        createV4(1, 2, 3)
        helper.runMigrationsAndValidate(dbName, 5, true, Migration4To5 { LegacyWakeCheck() }).close()
        helper.runMigrationsAndValidate(dbName, 6, true, Migration5To6).apply {
            execSQL("UPDATE alarms SET recheckEnabled = 0, recheckCount = 3 WHERE id = 1")
            execSQL("UPDATE alarms SET recheckEnabled = 1, recheckCount = 2 WHERE id = 2")
            execSQL("UPDATE alarms SET recheckEnabled = 1, recheckCount = 1 WHERE id = 3")
            close()
        }
        val db = helper.runMigrationsAndValidate(dbName, 7, true, Migration6To7)
        val rows = db.query("SELECT id, recheckEnabled, recheckCount FROM alarms ORDER BY id").use { c ->
            buildList {
                while (c.moveToNext()) add(List(3) { c.getInt(it) })
            }
        }
        db.close()
        assertThat(rows).containsExactly(listOf(1, 1, 1), listOf(2, 1, 2), listOf(3, 1, 1)).inOrder()
    }

    private fun insertV1(id: Long, missions: String, sound: String = "default") =
        "INSERT INTO alarms (id, hour, minute, repeatDaysMask, skipHolidays, enabled, missions, " +
            "ringAgainIntervalMinutes, ringAgainCount, wakeCheckEnabled, wakeCheckAskAfterMinutes, " +
            "wakeCheckAnswerSeconds, powerOffGuard, soundId, volumePercent, vibrate, gentleWake, background, " +
            "skipOnceDate, nextTriggerAt) VALUES ($id, 6, 30, 31, 0, 1, '$missions', 5, 3, 1, 1, 60, 0, " +
            "'$sound', 100, 1, 0, NULL, NULL, 1800000000000)"
}
