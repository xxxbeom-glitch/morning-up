package com.lumian.morningup.core.designsystem.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

val LocalMorningColors = staticCompositionLocalOf { lightMorningColors() }
val LocalMorningTypography = staticCompositionLocalOf { MorningTypography() }

/** 기기 '애니메이션 줄이기'(애니메이터 길이 배율 0)가 켜져 있으면 true. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/**
 * 앱 전체 테마. 다크 모드는 기기 설정을 따라요(F18).
 * 화면 코드는 `MorningTheme.colors`, `MorningTheme.typography`, `MorningDimens`만 써요.
 */
@Composable
fun MorningTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reduceMotion: Boolean = rememberSystemReduceMotion(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) darkMorningColors() else lightMorningColors()
    val typography = remember { MorningTypography() }
    CompositionLocalProvider(
        LocalMorningColors provides colors,
        LocalMorningTypography provides typography,
        LocalReduceMotion provides reduceMotion,
    ) {
        CappedFontScale(content)
    }
}

object MorningTheme {
    val colors: MorningColors
        @Composable @ReadOnlyComposable
        get() = LocalMorningColors.current

    val typography: MorningTypography
        @Composable @ReadOnlyComposable
        get() = LocalMorningTypography.current

    val reduceMotion: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalReduceMotion.current
}

@Composable
private fun rememberSystemReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
