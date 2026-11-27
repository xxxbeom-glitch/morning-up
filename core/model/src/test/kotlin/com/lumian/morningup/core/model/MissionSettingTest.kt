package com.lumian.morningup.core.model

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.mission.QuizCategory
import kotlinx.serialization.json.Json
import org.junit.Test

/** mission-levels.md 2번 표의 범위와 처음 값. */
class MissionSettingTest {
    @Test
    fun defaults() {
        assertThat(MissionSetting.default(MissionType.QuickMath)).isEqualTo(MissionSetting(MissionType.QuickMath, 1, 1))
        assertThat(MissionSetting.default(MissionType.PopQuiz)).isEqualTo(MissionSetting(MissionType.PopQuiz, 1, 1))
        assertThat(MissionSetting.default(MissionType.DropWords))
            .isEqualTo(MissionSetting(MissionType.DropWords, 1, 1, speed = 1))
        assertThat(
            MissionSetting.default(MissionType.ShakeShake),
        ).isEqualTo(MissionSetting(MissionType.ShakeShake, 1, 1))
    }

    @Test
    fun ranges() {
        val math = MissionSetting.rangeOf(MissionType.QuickMath)
        assertThat(math.levels).isEqualTo(1..7)
        assertThat(math.counts).isEqualTo(1..10)
        assertThat(MissionSetting.rangeOf(MissionType.PopQuiz).counts).isEqualTo(1..10)
        assertThat(MissionSetting.rangeOf(MissionType.DropWords).counts).isEqualTo(1..30)
        assertThat(MissionSetting.rangeOf(MissionType.DropWords).countStep).isEqualTo(1)
        assertThat(MissionSetting.rangeOf(MissionType.ShakeShake).counts).isEqualTo(1..200)
        assertThat(MissionSetting.rangeOf(MissionType.ShakeShake).levels).isEqualTo(1..1)
    }

    @Test
    fun normalized_forTest_allowsBelowMinimum() {
        val test = MissionSetting(MissionType.DropWords, count = 3, forTest = true)
        assertThat(test.normalized().count).isEqualTo(3)
        assertThat(MissionSetting(MissionType.DropWords, count = 0).normalized().count).isEqualTo(1)
    }

    @Test
    fun normalized_clampsOutOfRange() {
        assertThat(MissionSetting(MissionType.QuickMath, level = 9, count = 0).normalized())
            .isEqualTo(MissionSetting(MissionType.QuickMath, 7, 1))
        assertThat(MissionSetting(MissionType.ShakeShake, level = 3, count = 500, speed = 4).normalized())
            .isEqualTo(MissionSetting(MissionType.ShakeShake, 1, 200, speed = 1))
        assertThat(MissionSetting(MissionType.DropWords, speed = 9).normalized().speed).isEqualTo(4)
    }

    @Test
    fun alarm_missionTypes_keepOrder() {
        val alarm = Alarm(
            time = java.time.LocalTime.NOON,
            missions = listOf(MissionSetting(MissionType.ShakeShake), MissionSetting(MissionType.QuickMath)),
        )
        assertThat(alarm.missionTypes).containsExactly(MissionType.ShakeShake, MissionType.QuickMath).inOrder()
    }

    @Test
    fun quizCategories_allIsNotSaved_andOldValueReadsAsAll() {
        val json = Json { ignoreUnknownKeys = true }
        val saved = json.encodeToString(MissionSetting.serializer(), MissionSetting(MissionType.PopQuiz))
        assertThat(saved).doesNotContain("quizCategories")
        val old = json.decodeFromString(MissionSetting.serializer(), """{"type":"PopQuiz","level":2,"count":3}""")
        assertThat(old.allQuizCategories).isTrue()
        val two = MissionSetting(MissionType.PopQuiz, quizCategories = setOf(QuizCategory.Science, QuizCategory.Issues))
        val encoded = json.encodeToString(MissionSetting.serializer(), two)
        val back = json.decodeFromString(MissionSetting.serializer(), encoded)
        assertThat(back).isEqualTo(two)
    }

    @Test
    fun quizCategories_emptyOrOtherMission_normalizesToAll() {
        val empty = MissionSetting(MissionType.PopQuiz, quizCategories = emptySet())
        assertThat(empty.normalized().allQuizCategories).isTrue()
        val math = MissionSetting(MissionType.QuickMath, quizCategories = setOf(QuizCategory.People))
        assertThat(math.normalized().allQuizCategories).isTrue()
    }

    @Test
    fun toggleQuizCategory_followsPlanRules() {
        val all = MissionSetting.ALL_QUIZ_CATEGORIES
        // 다 골라진 상태에서 하나를 누르면 그 하나만 빠져요.
        assertThat(MissionSetting.toggleQuizCategory(all, QuizCategory.Economy)).isEqualTo(all - QuizCategory.Economy)
        assertThat(MissionSetting.toggleQuizCategory(setOf(QuizCategory.Science), QuizCategory.Issues))
            .containsExactly(QuizCategory.Science, QuizCategory.Issues)
        // 마지막 하나는 빠지지 않아요.
        assertThat(MissionSetting.toggleQuizCategory(setOf(QuizCategory.Science), QuizCategory.Science)).isNull()
        // '전체' 칩은 모두 골라요.
        assertThat(MissionSetting.toggleQuizCategory(setOf(QuizCategory.Science), null)).isEqualTo(all)
    }
}
