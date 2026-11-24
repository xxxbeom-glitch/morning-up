package com.lumian.morningup.core.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.data.db.MorningDatabase
import com.lumian.morningup.core.data.db.toEntity
import com.lumian.morningup.core.data.db.toModel
import com.lumian.morningup.core.data.diagnostics.DiagnosticLog
import com.lumian.morningup.core.data.session.RingSessionStore
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.RingAgain
import com.lumian.morningup.core.model.WakeCheck
import com.lumian.morningup.core.model.engine.RingPhase
import com.lumian.morningup.core.model.engine.RingPlan
import com.lumian.morningup.core.model.engine.RingSession
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DataLayerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, MorningDatabase::class.java).allowMainThreadQueries().build()

    @After
    fun close() = db.close()

    private val full = Alarm(
        id = 3,
        time = LocalTime.of(6, 45),
        repeatDays = setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY),
        skipHolidays = true,
        enabled = true,
        missions = listOf(MissionSetting(MissionType.QuickMath, 4, 5), MissionSetting(MissionType.DropWords, 2, 15, 3)),
        ringAgain = RingAgain(10, null),
        wakeCheck = WakeCheck(true),
        powerOffGuard = true,
        soundId = "s5_bird",
        volumePercent = 70,
        vibrate = false,
        gentleWake = true,
        background = "sky",
        skipOnceDate = LocalDate.of(2026, 11, 18),
        nextTriggerAt = 1_800_000_000_000,
    )

    @Test
    fun entityMapping_roundTripsEveryField() {
        assertThat(full.toEntity().toModel()).isEqualTo(full)
        val plain = Alarm(time = LocalTime.MIDNIGHT)
        assertThat(plain.toEntity().toModel()).isEqualTo(plain)
    }

    @Test
    fun everyDayMask_roundTrips() {
        val all = full.copy(repeatDays = DayOfWeek.entries.toSet())
        assertThat(all.toEntity().repeatDaysMask).isEqualTo(0b111_1111)
        assertThat(all.toEntity().toModel().repeatDays).isEqualTo(DayOfWeek.entries.toSet())
    }

    @Test
    fun unknownMissionName_isSkipped() {
        val entity = full.toEntity().copy(missions = "QuickMath,OldMission,")
        assertThat(entity.toModel().missions).containsExactly(MissionSetting.default(MissionType.QuickMath))
    }

    @Test
    fun missionJson_unknownTypeOrBrokenText_isSkipped() {
        val json = """[{"type":"OldMission","level":1,"count":3},{"type":"QuickMath","level":2,"count":4}]"""
        assertThat(full.toEntity().copy(missions = json).toModel().missions)
            .containsExactly(MissionSetting(MissionType.QuickMath, 2, 4))
        assertThat(full.toEntity().copy(missions = "[{broken").toModel().missions).isEmpty()
    }

    @Test
    fun repository_insertGetSaveDelete() = runTest {
        val repo = AlarmRepository(db.alarmDao())
        val saved = repo.insert(full.copy(id = 0))
        assertThat(saved.id).isGreaterThan(0L)
        assertThat(repo.get(saved.id)).isEqualTo(saved)
        repo.save(saved.copy(enabled = false))
        assertThat(repo.all().single().enabled).isFalse()
        repo.delete(saved.id)
        assertThat(repo.all()).isEmpty()
    }

    @Test
    fun diagnosticLog_keepsLatestMax() = runTest {
        val log = DiagnosticLog(db.diagnosticLogDao())
        val max = DiagnosticLog.MAX_ENTRIES
        repeat(max + 10) { log.record(at = it.toLong(), event = "ring", alarmId = 1) }
        val entries = log.entries()
        assertThat(entries).hasSize(max)
        assertThat(entries.first().at).isEqualTo(max + 9L)
        assertThat(entries.last().at).isEqualTo(10)
    }

    private fun TestScope.sessionStore(file: File) =
        PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file })

    @Test
    fun ringSession_savedLoadedAndCleared() = runTest {
        val store = RingSessionStore(sessionStore(tmp.newFile("ring_session.preferences_pb").also { it.delete() }))
        assertThat(store.load()).isNull()
        val session = RingSession(
            plan = RingPlan(3, listOf(MissionType.ShakeShake), 5, 3, false, 5, 60),
            phase = RingPhase.Resting,
            ringsLeft = 2,
            missionsDone = 0,
            phaseEndsAt = 99,
        )
        store.save(session)
        assertThat(store.load()).isEqualTo(session)
        store.save(null)
        assertThat(store.load()).isNull()
    }

    @Test
    fun ringSession_unreadableValue_isTreatedAsNone() = runTest {
        val dataStore = sessionStore(tmp.newFile("broken.preferences_pb").also { it.delete() })
        dataStore.edit { it[stringPreferencesKey("session")] = "{not json" }
        assertThat(RingSessionStore(dataStore).load()).isNull()
    }

    @Test
    fun schemas_areExported() {
        listOf(1, 2).forEach { version ->
            val schema = File("schemas/com.lumian.morningup.core.data.db.MorningDatabase/$version.json")
            assertThat(schema.exists()).isTrue()
            assertThat(schema.readText()).contains("\"tableName\": \"alarms\"")
        }
    }
}
