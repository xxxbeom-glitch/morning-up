package com.lumian.morningup.core.designsystem.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 화면 모드(TSK-166, 11-23 사용자 요청 "설정에서 임시로 다크, 라이트 모드로 수동으로 해 볼 수 있는 거").
 * [Auto]는 지금처럼 시각을 따라요(아침 라이트, 밤 다크). 디버그 빌드의 설정에서만 고를 수 있어요.
 */
enum class ThemeMode {
    Auto,
    Light,
    Dark,
    ;

    /** [autoNight]는 시각으로 정한 밤 여부예요. 고른 값이 있으면 그걸 따라요. */
    fun isDark(autoNight: Boolean): Boolean = when (this) {
        Auto -> autoNight
        Light -> false
        Dark -> true
    }
}

/** 울림 화면처럼 따로 뜨는 화면이 고른 화면 모드를 알 수 있게 내려 줘요. 출시 빌드는 늘 [ThemeMode.Auto]예요. */
val LocalThemeMode = staticCompositionLocalOf { ThemeMode.Auto }

/** 고른 화면 모드를 기기에 남겨요. 앱을 다시 열어도 유지돼요. 모든 화면이 같은 값을 봐요. */
object ThemeModeStore {
    private const val PREFS = "debug_theme_mode"
    private const val KEY = "mode"
    private val state = MutableStateFlow(ThemeMode.Auto)
    private var loaded = false

    fun mode(context: Context): StateFlow<ThemeMode> {
        if (!loaded) {
            val saved = prefs(context).getString(KEY, null)
            state.value = ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.Auto
            loaded = true
        }
        return state.asStateFlow()
    }

    fun set(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY, mode.name).apply()
        loaded = true
        state.value = mode
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

/** 고른 화면 모드. [enabled]가 아니면(출시 빌드) 늘 [ThemeMode.Auto]예요. */
@Composable
fun rememberThemeMode(enabled: Boolean): ThemeMode {
    if (!enabled) return ThemeMode.Auto
    val context = LocalContext.current
    val flow = remember(context) { ThemeModeStore.mode(context) }
    val mode by flow.collectAsState()
    return mode
}
