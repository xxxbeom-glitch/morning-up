package com.lumian.morningup.feature.settings

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 온보딩을 끝냈는지 기억해요. 첫 실행 때만 온보딩이 나오고, 끝나면(마지막 장을 넘기면) 다시 안 나와요. */
@Singleton
class OnboardingStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val isDone: Boolean get() = prefs.getBoolean(KEY_DONE, false)

    fun markDone() = prefs.edit { putBoolean(KEY_DONE, true) }

    /**
     * 디버그 '테스트' 페이지 스위치 '앱 켤 때마다 온보딩'(11-19 사용자 지시, 확인용 임시). 처음 값은 켜짐.
     * 출시 빌드에서는 보지 않아요(MainActivity가 BuildConfig.DEBUG로 막아요).
     */
    var replayEveryLaunch: Boolean
        get() = prefs.getBoolean(KEY_REPLAY, true)
        set(value) = prefs.edit { putBoolean(KEY_REPLAY, value) }

    private companion object {
        const val PREFS = "onboarding"
        const val KEY_DONE = "done"
        const val KEY_REPLAY = "replay_every_launch"
    }
}
