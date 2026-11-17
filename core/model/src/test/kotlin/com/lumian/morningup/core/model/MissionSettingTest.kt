package com.lumian.morningup.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** mission-levels.md 2번 표의 범위와 처음 값. */
class MissionSettingTest {
    @Test
    fun defaults() {
        assertThat(MissionSetting.default(MissionType.QuickMath)).isEqualTo(MissionSetting(MissionType.QuickMath, 1, 3))
        assertThat(MissionSetting.default(MissionType.PopQuiz)).isEqualTo(MissionSetting(MissionType.PopQuiz, 1, 3))
        assertThat(MissionSetting.default(MissionType.DropWords))
            .isEqualTo(MissionSetting(MissionType.DropWords, 1, 10, speed = 1))
        assertThat(
            MissionSetting.default(MissionType.ShakeShake),
        ).isEqualTo(MissionSetting(MissionType.ShakeShake, 1, 10))
    }

    @Test
    fun ranges() {
        val math = MissionSetting.rangeOf(MissionType.QuickMath)
        assertThat(math.levels).isEqualTo(1..7)
        assertThat(math.counts).isEqualTo(1..10)
        assertThat(MissionSetting.rangeOf(MissionType.PopQuiz).counts).isEqualTo(2..10)
        assertThat(MissionSetting.rangeOf(MissionType.DropWords).counts).isEqualTo(5..30)
        assertThat(MissionSetting.rangeOf(MissionType.DropWords).countStep).isEqualTo(5)
        assertThat(MissionSetting.rangeOf(MissionType.ShakeShake).counts).isEqualTo(10..200)
        assertThat(MissionSetting.rangeOf(MissionType.ShakeShake).levels).isEqualTo(1..1)
    }

    @Test
    fun normalized_forTest_allowsBelowMinimum() {
        val test = MissionSetting(MissionType.DropWords, count = 3, forTest = true)
        assertThat(test.normalized().count).isEqualTo(3)
        assertThat(MissionSetting(MissionType.DropWords, count = 3).normalized().count).isEqualTo(5)
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
}
