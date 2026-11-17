package com.lumian.morningup.core.alarm.guard

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** P6 접근성 권한 동의를 받았는지. 한 번 동의하면 다음부터는 P7 안내로 바로 가요. */
@Singleton
class PowerGuardConsent @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(prefs.getBoolean(KEY_AGREED, false))

    val agreed: StateFlow<Boolean> = state.asStateFlow()

    fun agree() {
        prefs.edit { putBoolean(KEY_AGREED, true) }
        state.value = true
    }

    private companion object {
        const val PREFS = "power_guard"
        const val KEY_AGREED = "agreed"
    }
}
