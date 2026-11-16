package com.lumian.morningup

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * 앱 시작점. 재부팅 뒤 잠금을 풀기 전에도 실행될 수 있어서
 * 여기서는 일반 저장소(잠금 뒤에만 열림)를 건드리지 않아요.
 */
@HiltAndroidApp
class MorningUpApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
