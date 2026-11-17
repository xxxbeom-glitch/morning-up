package com.lumian.morningup.core.alarm.di

import android.app.AlarmManager
import android.content.Context
import com.lumian.morningup.core.alarm.AlarmScope
import com.lumian.morningup.core.alarm.AndroidAlarmScheduler
import com.lumian.morningup.core.alarm.AndroidCatchUpHandler
import com.lumian.morningup.core.alarm.MinuteTicker
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.SchedulingAlarmBook
import com.lumian.morningup.core.alarm.SystemMinuteTicker
import com.lumian.morningup.core.alarm.ring.AndroidRingEffectRunner
import com.lumian.morningup.core.alarm.ring.RingEffectRunner
import com.lumian.morningup.core.alarm.ring.RingLink
import com.lumian.morningup.core.alarm.ring.ServiceRingLink
import com.lumian.morningup.core.model.AlarmBook
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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber

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
    fun scope(): CoroutineScope {
        // 처리 중 예외가 나도 앱 전체가 죽지 않게 기록만 남겨요. 효과 실행기는 효과마다 따로 잡아요.
        val handler = CoroutineExceptionHandler { _, e -> Timber.e(e, "alarm scope failure") }
        return CoroutineScope(SupervisorJob() + Dispatchers.Default + handler)
    }

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

    @Binds
    fun alarmBook(impl: SchedulingAlarmBook): AlarmBook

    @Binds
    fun ticker(impl: SystemMinuteTicker): MinuteTicker

    @Binds
    fun ringLink(impl: ServiceRingLink): RingLink
}
