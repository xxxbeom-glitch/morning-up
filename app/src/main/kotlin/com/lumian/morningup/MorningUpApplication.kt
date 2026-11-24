package com.lumian.morningup

import android.app.Application
import android.app.NotificationManager
import com.lumian.morningup.core.alarm.AlarmScope
import com.lumian.morningup.core.model.WakeCheckSettingsStore
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 앱 시작점. 재부팅 뒤 잠금을 풀기 전에도 실행될 수 있어서
 * 여기서는 일반 저장소(잠금 뒤에만 열림)를 건드리지 않아요.
 */
@HiltAndroidApp
class MorningUpApplication : Application() {
    @Inject lateinit var wakeSettings: WakeCheckSettingsStore

    @Inject
    @AlarmScope
    lateinit var scope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // 예전 알람마다 다시 잠들기 방지 값을 전체 설정으로 한 번 옮겨요(결정 145). 기기 보호 저장소라 잠금 전에도 돼요.
        scope.launch { wakeSettings.current() }
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
