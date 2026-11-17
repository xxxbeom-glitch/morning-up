package com.lumian.morningup.core.alarm.guard

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 전원 종료 방지 앱 설정(결정 101). [enabled]는 설정 탭 '전원 종료 방지' 토글이고, 켜 두면 미션이 있는 알람의 미션 중에 동작해요.
 * [agreed]는 P6 접근성 권한 동의를 받았는지예요. 한 번 동의하면 다음부터는 P7 안내로 바로 가요.
 */
@Singleton
class PowerGuardConsent @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(prefs.getBoolean(KEY_AGREED, false))

    val agreed: StateFlow<Boolean> = state.asStateFlow()

    fun agree() {
        prefs.edit { putBoolean(KEY_AGREED, true) }
        state.value = true
    }

    private val enabledState = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))

    val enabled: StateFlow<Boolean> = enabledState.asStateFlow()

    fun setEnabled(on: Boolean) {
        prefs.edit { putBoolean(KEY_ENABLED, on) }
        enabledState.value = on
    }

    private companion object {
        const val PREFS = "power_guard"
        const val KEY_AGREED = "agreed"
        const val KEY_ENABLED = "enabled"
    }
}
