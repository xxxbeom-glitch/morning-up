package com.lumian.morningup.feature.ring

import android.content.Context
import com.lumian.morningup.core.alarm.ring.LabLog
import com.lumian.morningup.core.alarm.ring.TrickAccessibility
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/**
 * 버튼 속이기(결정 146 2절)를 지금 걸 수 있는지 보고, 실험실 기록을 남기는 곳.
 * 울림 화면 뷰모델이 써요. 시험에서는 가짜로 바꿔 끼워요.
 */
interface TrickRecorder {
    /** 버튼 속이기를 자동으로 꺼야 하는 까닭(2-4). 걸 수 있으면 null이에요. */
    fun offReason(): String?

    /** [LabLog.record]와 같아요. */
    fun record(event: String, alarmId: Long?, detail: String = "", kept: Boolean = false)
}

class AndroidTrickRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val labLog: LabLog,
) : TrickRecorder {
    override fun offReason(): String? = TrickAccessibility.offReason(context)

    override fun record(event: String, alarmId: Long?, detail: String, kept: Boolean) =
        labLog.record(event, alarmId, detail, kept)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RingModule {
    @Binds
    abstract fun trickRecorder(impl: AndroidTrickRecorder): TrickRecorder
}
