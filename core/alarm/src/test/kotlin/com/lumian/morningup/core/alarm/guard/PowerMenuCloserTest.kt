package com.lumian.morningup.core.alarm.guard

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.guard.PowerMenuCloser.Action
import org.junit.Test

/** ISS-060: 버튼을 누르고 있는 동안 무시돼도, 메뉴가 사라질 때까지 다시 닫아요. */
class PowerMenuCloserTest {
    private val step = PowerMenuCloser.RETRY_MILLIS

    @Test
    fun backOnce_thenHomeUntilOtherWindow() {
        val closer = PowerMenuCloser()
        assertThat(closer.onMenuSeen(0)).isTrue()
        assertThat(closer.next(0)).isEqualTo(Action.Back)
        // 1.5초 누르는 동안 홈이 무시되어 다른 창 신호가 없어요.
        val during = (1..5).map { closer.next(it * step) }
        assertThat(during).containsExactlyElementsIn(List(5) { Action.Home })
        assertThat(closer.active).isTrue()
        closer.onOtherWindow()
        assertThat(closer.next(6 * step)).isEqualTo(Action.Done)
        assertThat(closer.active).isFalse()
    }

    @Test
    fun repeatedMenuSignal_doesNotRestart_butExtendsTime() {
        val closer = PowerMenuCloser()
        closer.onMenuSeen(0)
        closer.next(0)
        assertThat(closer.onMenuSeen(4_000)).isFalse()
        assertThat(closer.next(4_000 + PowerMenuCloser.MAX_MILLIS - 1)).isEqualTo(Action.Home)
        assertThat(closer.next(4_000 + PowerMenuCloser.MAX_MILLIS)).isEqualTo(Action.GiveUp)
        assertThat(closer.active).isFalse()
    }

    @Test
    fun menuSeenAgainAfterClosing_clearsGone() {
        val closer = PowerMenuCloser()
        closer.onMenuSeen(0)
        closer.next(0)
        closer.onOtherWindow()
        closer.onMenuSeen(step)
        assertThat(closer.next(step)).isEqualTo(Action.Home)
    }

    @Test
    fun otherWindow_whenIdle_isIgnored() {
        val closer = PowerMenuCloser()
        closer.onOtherWindow()
        assertThat(closer.onMenuSeen(0)).isTrue()
        assertThat(closer.next(0)).isEqualTo(Action.Back)
    }

    @Test
    fun givesUp_afterMaxWithoutSignal() {
        val closer = PowerMenuCloser()
        closer.onMenuSeen(0)
        var now = 0L
        var last: Action
        do {
            last = closer.next(now)
            now += step
        } while (closer.active)
        assertThat(last).isEqualTo(Action.GiveUp)
        assertThat(now).isAtLeast(PowerMenuCloser.MAX_MILLIS)
    }
}
