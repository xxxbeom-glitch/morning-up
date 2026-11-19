package com.lumian.morningup.feature.edit

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** 시, 분 숫자 입력 규칙(11-19 사용자 지시 5). */
class TimeEntryTest {

    @Test
    fun hour_firstDigit2to9_doneAtOnce() {
        assertThat(TimeEntry.type(TimeField.Hour, "", "7")).isEqualTo(TimeEntry.Step("7", true))
        assertThat(TimeEntry.type(TimeField.Hour, "", "2")).isEqualTo(TimeEntry.Step("2", true))
    }

    @Test
    fun hour_firstDigit0or1_waitsForSecond() {
        assertThat(TimeEntry.type(TimeField.Hour, "", "1")).isEqualTo(TimeEntry.Step("1", false))
        assertThat(TimeEntry.type(TimeField.Hour, "1", "12")).isEqualTo(TimeEntry.Step("12", true))
        assertThat(TimeEntry.type(TimeField.Hour, "0", "09")).isEqualTo(TimeEntry.Step("09", true))
    }

    @Test
    fun hour_over12_secondDigitRejected() {
        assertThat(TimeEntry.type(TimeField.Hour, "1", "13")).isEqualTo(TimeEntry.Step("1", false))
    }

    @Test
    fun minute_rules() {
        assertThat(TimeEntry.type(TimeField.Minute, "", "7")).isEqualTo(TimeEntry.Step("07", true))
        assertThat(TimeEntry.type(TimeField.Minute, "", "3")).isEqualTo(TimeEntry.Step("3", false))
        assertThat(TimeEntry.type(TimeField.Minute, "3", "35")).isEqualTo(TimeEntry.Step("35", true))
        assertThat(TimeEntry.type(TimeField.Minute, "", "x")).isEqualTo(TimeEntry.Step("", false))
    }

    @Test
    fun finish_values() {
        assertThat(TimeEntry.hour12("", 7)).isEqualTo(7)
        assertThat(TimeEntry.hour12("00", 7)).isEqualTo(12)
        assertThat(TimeEntry.hour12("1", 7)).isEqualTo(1)
        assertThat(TimeEntry.minute("", 30)).isEqualTo(30)
        assertThat(TimeEntry.minute("5", 30)).isEqualTo(5)
    }

    @Test
    fun state_hourThenMinute_thenCloses() {
        val commits = mutableListOf<Pair<TimeField, String>>()
        val state = TimeInputState().apply { onCommit = { f, t -> commits += f to t } }
        state.start(TimeField.Hour)
        state.done(TimeField.Hour, "8", next = true)
        assertThat(state.field).isEqualTo(TimeField.Minute)
        state.text = "4"
        state.finish()
        assertThat(state.field).isNull()
        assertThat(commits).containsExactly(TimeField.Hour to "8", TimeField.Minute to "4").inOrder()
    }
}
