package com.lumian.morningup.feature.mission

import com.lumian.morningup.core.model.mission.DropWordHistory
import com.lumian.morningup.feature.mission.play.MissionContentSource
import com.lumian.morningup.feature.mission.play.PrefsDropWordHistory
import com.lumian.morningup.feature.mission.play.RawMissionContentSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import kotlin.random.Random

/** 미션 문제, 키패드 섞기에 쓰는 난수. 테스트는 seed 있는 [Random]을 직접 넣어요. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MissionRandom

@Module
@InstallIn(SingletonComponent::class)
object MissionModule {
    @Provides
    @MissionRandom
    fun random(): Random = Random.Default
}

@Module
@InstallIn(SingletonComponent::class)
interface MissionBindings {
    @Binds
    fun content(impl: RawMissionContentSource): MissionContentSource

    @Binds
    fun dropWordHistory(impl: PrefsDropWordHistory): DropWordHistory
}
