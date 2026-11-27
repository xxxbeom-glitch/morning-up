package com.lumian.morningup.core.ui

import com.lumian.morningup.core.model.mission.QuizCategory
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
    private const val SECONDS_PER_MINUTE = 60
    private const val MILLIS_PER_SECOND = 1_000L

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

    /** '11월 16일 월요일'(알람 울림 화면). */
    fun dateLong(resources: Resources, date: LocalDate): String =
        resources.getString(
            R.string.ui_date_long,
            date.monthValue,
            date.dayOfMonth,
            dayShort(resources, date.dayOfWeek),
        )

    /** '30초', '1분', '1분 30초'. */
    fun duration(resources: Resources, seconds: Int): String {
        val minutes = seconds / SECONDS_PER_MINUTE
        val rest = seconds % SECONDS_PER_MINUTE
        val parts = buildList {
            if (minutes > 0) add(resources.getString(R.string.ui_duration_minutes, minutes))
            if (rest > 0 || isEmpty()) add(resources.getString(R.string.ui_duration_seconds, rest))
        }
        return parts.joinToString(" ")
    }

    /** 남은 시간 '0:42', '1:05'. 남은 밀리초는 올림해서 0이 되는 순간에만 '0:00'이에요. */
    fun countdown(resources: Resources, millis: Long): String {
        val total = ((millis.coerceAtLeast(0) + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
        return resources.getString(R.string.ui_countdown, total / SECONDS_PER_MINUTE, total % SECONDS_PER_MINUTE)
    }

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

    /** Pop Quiz 분야 이름 리소스. */
    fun quizCategoryRes(category: QuizCategory): Int = when (category) {
        QuizCategory.Economy -> R.string.ui_quiz_category_economy
        QuizCategory.Society -> R.string.ui_quiz_category_society
        QuizCategory.KoreanHistory -> R.string.ui_quiz_category_korean_history
        QuizCategory.WorldHistory -> R.string.ui_quiz_category_world_history
        QuizCategory.Science -> R.string.ui_quiz_category_science
        QuizCategory.People -> R.string.ui_quiz_category_people
        QuizCategory.Issues -> R.string.ui_quiz_category_issues
    }

    /** 고른 분야 요약: '과학 용어', '과학 용어 외 1개'. 순서는 분야 목록 순서예요. */
    fun quizCategories(resources: Resources, categories: Set<QuizCategory>): String {
        val sorted = QuizCategory.entries.filter { it in categories }
        val first = resources.getString(quizCategoryRes(sorted.first()))
        return if (sorted.size == 1) first else resources.getString(R.string.ui_mission_more, first, sorted.size - 1)
    }
}

/** Compose에서 쓰는 리소스. 화면 방향이나 언어가 바뀌면 다시 읽어요. */
@Composable
@ReadOnlyComposable
fun alarmResources(): Resources {
    LocalConfiguration.current
    return LocalContext.current.resources
}
