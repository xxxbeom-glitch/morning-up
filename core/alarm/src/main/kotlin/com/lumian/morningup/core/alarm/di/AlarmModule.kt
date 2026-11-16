package com.lumian.morningup.core.alarm.di

import android.app.AlarmManager
import android.content.Context
import com.lumian.morningup.core.alarm.AlarmScope
import com.lumian.morningup.core.alarm.AndroidAlarmScheduler
import com.lumian.morningup.core.alarm.AndroidCatchUpHandler
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ring.AndroidRingEffectRunner
import com.lumian.morningup.core.alarm.ring.RingEffectRunner
import com.lumian.morningup.core.model.engine.AlarmRescheduler
import com.lumian.morningup.core.model.engine.AlarmScheduler
import com.lumian.morningup.core.model.engine.AlarmStore
import com.lumian.morningup.core.model.engine.CatchUpHandler
import com.lumian.morningup.core.model.engine.HolidayCalendar
import com.lumian.morningup.core.model.engine.KoreanHolidays
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object AlarmModule {
    @Provides
    fun alarmManager(@ApplicationContext context: Context): AlarmManager =
        context.getSystemService(AlarmManager::class.java)

    @Provides
    fun holidays(): HolidayCalendar = KoreanHolidays

    @Provides
    fun now(): NowProvider = NowProvider.System

    @Provides
    @Singleton
    @AlarmScope
    fun scope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun rescheduler(
        store: AlarmStore,
        scheduler: AlarmScheduler,
        catchUp: CatchUpHandler,
        holidays: HolidayCalendar,
    ): AlarmRescheduler = AlarmRescheduler(store, scheduler, catchUp, holidays)
}

@Module
@InstallIn(SingletonComponent::class)
interface AlarmBindings {
    @Binds
    fun scheduler(impl: AndroidAlarmScheduler): AlarmScheduler

    @Binds
    fun catchUp(impl: AndroidCatchUpHandler): CatchUpHandler

    @Binds
    fun runner(impl: AndroidRingEffectRunner): RingEffectRunner
}
