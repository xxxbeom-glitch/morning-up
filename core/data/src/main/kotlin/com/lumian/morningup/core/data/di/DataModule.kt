package com.lumian.morningup.core.data.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.lumian.morningup.core.data.AlarmRepository
import com.lumian.morningup.core.data.db.AlarmDao
import com.lumian.morningup.core.data.db.DiagnosticLogDao
import com.lumian.morningup.core.data.db.MorningDatabase
import com.lumian.morningup.core.data.session.RingSessionStore
import com.lumian.morningup.core.data.settings.WakeCheckSettingsPrefs
import com.lumian.morningup.core.model.WakeCheckSettingsStore
import com.lumian.morningup.core.model.engine.AlarmStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    /** 잠금을 풀기 전에도 읽혀야 하는 저장소는 모두 이 기기 보호 저장소에 둬요. */
    private fun Context.deviceProtected(): Context = createDeviceProtectedStorageContext()

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): MorningDatabase =
        Room.databaseBuilder(context.deviceProtected(), MorningDatabase::class.java, MorningDatabase.NAME)
            .apply { MorningDatabase.MIGRATIONS.forEach { addMigrations(it) } }
            .build()

    @Provides
    fun alarmDao(db: MorningDatabase): AlarmDao = db.alarmDao()

    @Provides
    fun diagnosticLogDao(db: MorningDatabase): DiagnosticLogDao = db.diagnosticLogDao()

    @Provides
    @Singleton
    fun ringSessionStore(@ApplicationContext context: Context): RingSessionStore {
        val dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.deviceProtected().preferencesDataStoreFile("ring_session") },
        )
        return RingSessionStore(dataStore)
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface DataBindings {
    @Binds
    fun alarmStore(repository: AlarmRepository): AlarmStore

    @Binds
    fun wakeCheckSettings(impl: WakeCheckSettingsPrefs): WakeCheckSettingsStore
}
