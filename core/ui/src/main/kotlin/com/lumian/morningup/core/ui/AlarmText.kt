package com.lumian.morningup.core.ui

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.RepeatPattern
import com.lumian.morningup.core.model.engine.TimeUntil
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * 알람을 화면 글자로 바꿔요. 화면 모듈은 이 함수들만 쓰고 형식을 따로 만들지 않아요.
 * 지금은 12시간제(오전, 오후)만 써요. 24시간제는 설정 단계에서 정해요.
 */
object AlarmText {
    private const val HOURS_HALF_DAY = 12

    /** '오전 6:30'. */
    fun time(resources: Resources, time: LocalTime): String {
        val period = resources.getString(if (time.hour < HOURS_HALF_DAY) R.string.ui_am else R.string.ui_pm)
        return resources.getString(R.string.ui_time_12h, period, hour12(time.hour), time.minute)
    }

    /** 12시간제 시(1~12). */
    fun hour12(hour24: Int): Int = (hour24 % HOURS_HALF_DAY).let { if (it == 0) HOURS_HALF_DAY else it }

    fun period(resources: Resources, pm: Boolean): String =
        resources.getString(if (pm) R.string.ui_pm else R.string.ui_am)

    /** '월'. */
    fun dayShort(resources: Resources, day: DayOfWeek): String =
        resources.getStringArray(R.array.ui_day_short)[day.value - 1]

    /** '11월 16일 (월)'. */
    fun date(resources: Resources, date: LocalDate): String =
        resources.getString(
            R.string.ui_date_short,
            date.monthValue,
            date.dayOfMonth,
            dayShort(resources, date.dayOfWeek),
        )

    /** '7시간 20분', '1일 7시간 50분', '20분'. 0인 단위는 빼요. 모두 0이면 '0분'. */
    fun until(resources: Resources, until: TimeUntil): String {
        val parts = buildList {
            if (until.days > 0) add(resources.getString(R.string.ui_until_days, until.days))
            if (until.hours > 0) add(resources.getString(R.string.ui_until_hours, until.hours))
            if (until.minutes > 0 || isEmpty()) add(resources.getString(R.string.ui_until_minutes, until.minutes))
        }
        return parts.joinToString(" ")
    }

    /** '평일', '매일', '주말', '한 번', '월 수 금'. */
    fun repeat(resources: Resources, days: Set<DayOfWeek>): String = when (val pattern = RepeatPattern.of(days)) {
        RepeatPattern.Once -> resources.getString(R.string.ui_repeat_once)
        RepeatPattern.Everyday -> resources.getString(R.string.ui_repeat_everyday)
        RepeatPattern.Weekdays -> resources.getString(R.string.ui_repeat_weekdays)
        RepeatPattern.Weekend -> resources.getString(R.string.ui_repeat_weekend)
        is RepeatPattern.Custom -> pattern.days.joinToString(" ") { dayShort(resources, it) }
    }

    fun missionName(resources: Resources, mission: MissionType): String = resources.getString(
        when (mission) {
            MissionType.QuickMath -> R.string.ui_mission_quick_math
            MissionType.PopQuiz -> R.string.ui_mission_pop_quiz
            MissionType.DropWords -> R.string.ui_mission_drop_words
            MissionType.ShakeShake -> R.string.ui_mission_shake_shake
        },
    )

    /** '없음', 'Quick Math', 'Drop Words 외 1개'. */
    fun missions(resources: Resources, missions: List<MissionType>): String = when (missions.size) {
        0 -> resources.getString(R.string.ui_mission_none)
        1 -> missionName(resources, missions.first())
        else -> resources.getString(
            R.string.ui_mission_more,
            missionName(resources, missions.first()),
            missions.size - 1,
        )
    }
}

/** Compose에서 쓰는 리소스. 화면 방향이나 언어가 바뀌면 다시 읽어요. */
@Composable
@ReadOnlyComposable
fun alarmResources(): Resources {
    LocalConfiguration.current
    return LocalContext.current.resources
}
