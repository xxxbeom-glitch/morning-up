package com.lumian.morningup.core.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.time.DayOfWeek

/**
 * 설정 > 알람 '한 주 시작 요일'(TSK-276, TSK-287). 처음 값 일요일. 알람 편집 요일 줄과 홈 알람 카드 요일 칸이
 * 이 순서를 따라요. 반복 요일 저장값은 그대로고 보여 주는 순서만 바뀌어요.
 */
object WeekStart {
    private const val PREFS = "display"
    private const val KEY_MONDAY = "week_start_monday"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isMonday(context: Context): Boolean = prefs(context).getBoolean(KEY_MONDAY, false)

    fun setMonday(context: Context, monday: Boolean) = prefs(context).edit().putBoolean(KEY_MONDAY, monday).apply()

    /** 한 주 일곱 요일을 보여 줄 순서대로. */
    fun days(monday: Boolean): List<DayOfWeek> = if (monday) {
        DayOfWeek.entries.toList()
    } else {
        listOf(DayOfWeek.SUNDAY) + DayOfWeek.entries.filter { it != DayOfWeek.SUNDAY }
    }

    /** 바뀌면 바로 다시 그리는 '월요일 시작인지' 값. */
    @Composable
    fun rememberMonday(): Boolean {
        val context = LocalContext.current
        var monday by remember { mutableStateOf(isMonday(context)) }
        DisposableEffect(context) {
            val p = prefs(context)
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_MONDAY) monday = isMonday(context)
            }
            p.registerOnSharedPreferenceChangeListener(listener)
            monday = isMonday(context)
            onDispose { p.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        return monday
    }

    /** 보여 줄 요일 순서. */
    @Composable
    fun rememberDays(): List<DayOfWeek> = days(rememberMonday())
}
