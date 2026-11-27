package com.lumian.morningup.core.alarm.guard

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.guard.PowerMenuCloser.Action
import com.lumian.morningup.core.alarm.guard.PowerMenuCloser.Companion.CHECK_WAIT_MILLIS
import com.lumian.morningup.core.alarm.guard.PowerMenuCloser.Companion.PRESS_GAP_MILLIS
import org.junit.Test

/** ISS-060: 버튼을 누르고 있는 동안 무시돼도, 울림 화면이 초점을 되찾을 때까지 다시 닫아요. */
class PowerMenuCloserTest {
    private val pressRound = List(PowerMenuCloser.PRESSES) { listOf(Action.Home, Action.Back) }.flatten()

    /** 한 바퀴(누름 6번 + 바퀴 끝)를 돌려 동작 목록과 끝난 시각을 돌려줘요. */
    private fun PowerMenuCloser.round(start: Long, focused: Boolean = false): Pair<List<Action>, Long> {
        var now = start
        val actions = mutableListOf<Action>()
        repeat(pressRound.size + 1) {
            val step = next(now, focused)
            actions += step.action
            now += step.waitMillis
        }
        return actions to now
    }

    @Test
    fun ringing_homeBackThreeTimes_thenReopen() {
        val closer = PowerMenuCloser()
        assertThat(closer.onMenuSeen(0, reopenScreen = true)).isTrue()
        val (actions, end) = closer.round(0)
        assertThat(actions).containsExactlyElementsIn(pressRound + Action.Reopen).inOrder()
        assertThat(end).isEqualTo(pressRound.size * PRESS_GAP_MILLIS + CHECK_WAIT_MILLIS)
        assertThat(closer.active).isTrue()
    }

    @Test
    fun noFocusYet_anotherRound_thenDoneWhenFocused() {
        val closer = PowerMenuCloser()
        closer.onMenuSeen(0, reopenScreen = true)
        val (_, end) = closer.round(0)
        // 버튼을 아직 누르고 있어 메뉴가 그대로라 초점이 없어요. 한 바퀴 더 돌아요.
        val (again, end2) = closer.round(end, focused = false)
        assertThat(again).containsExactlyElementsIn(pressRound + Action.Reopen).inOrder()
        assertThat(closer.rounds).isEqualTo(1)
        // 홈 화면 창이 메뉴 뒤에서 열려도 초점만 봐요. 초점이 돌아오면 끝.
        assertThat(closer.next(end2, ringFocused = true).action).isEqualTo(Action.Done)
        assertThat(closer.active).isFalse()
    }

    @Test
    fun resting_waitsInsteadOfReopen_andEndsAfterRestingTime() {
        val closer = PowerMenuCloser()
        closer.onMenuSeen(0, reopenScreen = false)
        val (actions, end) = closer.round(0)
        assertThat(actions.last()).isEqualTo(Action.Wait)
        assertThat(actions).doesNotContain(Action.Reopen)
        var now = end
        var last = closer.next(now, ringFocused = false)
        while (closer.active) {
            assertThat(last.action).isNotEqualTo(Action.Reopen)
            now += last.waitMillis
            last = closer.next(now, ringFocused = false)
        }
        assertThat(last.action).isEqualTo(Action.Done)
        assertThat(now).isAtLeast(PowerMenuCloser.RESTING_MILLIS)
    }

    @Test
    fun repeatedMenuSignal_doesNotRestart_butExtendsDeadline() {
        val closer = PowerMenuCloser()
        closer.onMenuSeen(0, reopenScreen = true)
        closer.next(0, ringFocused = false)
        assertThat(closer.onMenuSeen(4_000, reopenScreen = true)).isFalse()
        var now = 0L
        var last = closer.next(now, ringFocused = false)
        while (closer.active) {
            now += last.waitMillis
            last = closer.next(now, ringFocused = false)
        }
        assertThat(last.action).isEqualTo(Action.GiveUp)
        assertThat(now).isAtLeast(4_000 + PowerMenuCloser.MAX_MILLIS)
    }

    @Test
    fun neverFocused_givesUpAfterMax() {
        val closer = PowerMenuCloser()
        closer.onMenuSeen(0, reopenScreen = true)
        var now = 0L
        var last = closer.next(now, ringFocused = false)
        while (closer.active) {
            now += last.waitMillis
            last = closer.next(now, ringFocused = false)
        }
        assertThat(last.action).isEqualTo(Action.GiveUp)
        assertThat(now).isAtLeast(PowerMenuCloser.MAX_MILLIS)
        assertThat(now).isLessThan(PowerMenuCloser.MAX_MILLIS + 1_000)
    }

    @Test
    fun notActive_returnsDone() {
        assertThat(PowerMenuCloser().next(0, ringFocused = false).action).isEqualTo(Action.Done)
    }
}
