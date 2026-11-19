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

    private companion object {
        const val PREFS = "onboarding"
        const val KEY_DONE = "done"
    }
}
