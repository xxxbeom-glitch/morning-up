package com.lumian.morningup.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.lumian.morningup.core.model.DayPart
import java.time.ZonedDateTime
import kotlinx.coroutines.delay

/**
 * 앱 테마는 폰 다크 모드 설정이 아니라 지금 시각을 따라요(11-18 사용자 지시, 11-16 '아침은 라이트, 밤은 다크').
 * 아침(4시~18시 전)은 라이트, 밤(18시~4시 전)은 다크예요. 울림 화면과 같은 [DayPart] 기준이에요.
 * 앱을 켜 둔 채 경계 시각을 넘으면 30초 안에 바뀌어요.
 */
@Composable
fun rememberNightByClock(now: () -> ZonedDateTime): Boolean {
    fun isNight() = DayPart.of(now().toLocalTime()) == DayPart.Night
    var night by remember { mutableStateOf(isNight()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(CHECK_MILLIS)
            night = isNight()
        }
    }
    return night
}

/** 상태 표시줄, 내비게이션 줄 아이콘 색을 화면 테마에 맞춰요(다크면 흰 아이콘). */
@Composable
fun SystemBarIcons(dark: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val CHECK_MILLIS = 30_000L
