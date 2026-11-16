package com.lumian.morningup.ring

import android.content.Intent
import com.lumian.morningup.core.alarm.ring.RingScreenIntentFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** 알람 모듈이 울림 화면을 열 때 이 Activity를 써요. */
@Module
@InstallIn(SingletonComponent::class)
object RingScreenModule {
    @Provides
    fun ringScreenIntent(): RingScreenIntentFactory = RingScreenIntentFactory { context ->
        Intent(context, RingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}
