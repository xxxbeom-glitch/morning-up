package com.lumian.morningup

import android.app.Application
import android.app.NotificationManager
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
        if (!BuildConfig.SLEEP_LOUNGE) {
            // 수면 라운지를 숨긴 판에서는 예전 설치 때 만든 '잠들기 재생' 알림 종류를 지워요(ISS-039).
            // 알림 종류는 앱을 업데이트해도 남아서, 폰 알림 설정에 쓰지 않는 줄로 보였어요.
            getSystemService(NotificationManager::class.java)?.deleteNotificationChannel(SLEEP_CHANNEL)
        }
    }

    private companion object {
        /** core/alarm `SleepPlaybackService.CHANNEL`과 같은 값이에요. */
        const val SLEEP_CHANNEL = "sleep_playback"
    }
}
