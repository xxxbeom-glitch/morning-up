package com.lumian.morningup.core.data

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.data.db.Migration1To2
import com.lumian.morningup.core.data.db.MorningDatabase
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
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
        ).apply { MorningDatabase.MIGRATIONS.forEach { addMigrations(it) } }.allowMainThreadQueries().build()
        val alarms = AlarmRepository(db.alarmDao()).all().associateBy { it.id }
        db.close()

        assertThat(alarms.keys).containsExactly(1L, 2L, 3L)
        assertThat(alarms.getValue(1).missions).containsExactly(
            MissionSetting(MissionType.QuickMath, 1, 3),
            MissionSetting(MissionType.DropWords, 1, 10, 1),
        ).inOrder()
        assertThat(alarms.getValue(2).missions).isEmpty()
        assertThat(alarms.getValue(3).missions).containsExactly(MissionSetting.default(MissionType.ShakeShake))
        // 다른 칸은 손대지 않아요.
        assertThat(alarms.getValue(1).time.hour).isEqualTo(6)
        assertThat(alarms.getValue(1).wakeCheck.enabled).isTrue()
        assertThat(alarms.getValue(1).nextTriggerAt).isEqualTo(1_800_000_000_000)
    }

    private fun insertV1(id: Long, missions: String) =
        "INSERT INTO alarms (id, hour, minute, repeatDaysMask, skipHolidays, enabled, missions, " +
            "ringAgainIntervalMinutes, ringAgainCount, wakeCheckEnabled, wakeCheckAskAfterMinutes, " +
            "wakeCheckAnswerSeconds, powerOffGuard, soundId, volumePercent, vibrate, gentleWake, background, " +
            "skipOnceDate, nextTriggerAt) VALUES ($id, 6, 30, 31, 0, 1, '$missions', 5, 3, 1, 1, 60, 0, " +
            "'default', 100, 1, 0, NULL, NULL, 1800000000000)"
}
