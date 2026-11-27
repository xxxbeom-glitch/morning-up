package com.lumian.morningup.core.alarm.guard

/**
 * 전원 메뉴를 닫을 때까지 다시 닫는 순서(ISS-060 세 번째 고침). 안드로이드와 상관없는 순수 로직이라 단위 시험으로 확인해요.
 *
 * 한 바퀴: 0.1초마다 홈 → 뒤로를 3번(알라미와 같은 방식, `research/Q-alarmy-powerguard.md`).
 * 홈 다음이라 뒤로는 홈 화면에 닿아서 미션 화면의 '닫기'를 누르지 않아요.
 * 울리는 중이나 미션 중이면 바퀴 끝에 울림 화면을 다시 앞으로 부르고 0.4초 뒤 울림 화면이 입력 초점을 되찾았는지 봐요.
 * 초점이 있으면 메뉴가 닫힌 거예요. 없으면(버튼을 아직 누르고 있어 무시됨) 한 바퀴 더 해요.
 * 홈 화면 창이 메뉴 뒤에서 열려도 초점은 메뉴에 있으니 잘못 끝내지 않아요(d64ab49에서 실패한 점).
 * 쉬는 중에는 띄울 울림 화면이 없어서 메뉴 신호 뒤 [RESTING_MILLIS] 동안 바퀴를 돌고 끝내요.
 * 마지막 메뉴 신호 뒤 [MAX_MILLIS]가 지나면 멈춰요. 닫는 중에 메뉴 신호가 또 오면 시간만 늘려요.
 */
class PowerMenuCloser {
    enum class Action { Home, Back, Reopen, Wait, Done, GiveUp }

    /** [action]을 하고 [waitMillis] 뒤에 다시 [next]를 불러요. Done, GiveUp이면 끝이에요. */
    data class Step(val action: Action, val waitMillis: Long)

    var active: Boolean = false
        private set
    var rounds: Int = 0
        private set

    private var reopen = false
    private var startedAt = 0L
    private var deadline = 0L
    private var index = 0

    /** 전원 메뉴 창 신호. 처음이면 true(반복을 시작해야 해요), 이미 닫는 중이면 false. */
    fun onMenuSeen(now: Long, reopenScreen: Boolean): Boolean {
        deadline = now + MAX_MILLIS
        reopen = reopenScreen
        if (active) return false
        active = true
        startedAt = now
        rounds = 0
        index = 0
        return true
    }

    /** 다음에 할 일. [ringFocused]는 지금 울림 화면에 입력 초점이 있는지예요. */
    fun next(now: Long, ringFocused: Boolean): Step {
        if (!active) return Step(Action.Done, 0)
        val presses = PRESSES * 2
        if (index < presses) {
            val action = if (index % 2 == 0) Action.Home else Action.Back
            index++
            return Step(action, PRESS_GAP_MILLIS)
        }
        if (index == presses) {
            index++
            // 쉬는 중에는 다시 띄울 화면이 없어 기다리기만 해요.
            return Step(if (reopen) Action.Reopen else Action.Wait, CHECK_WAIT_MILLIS)
        }
        val closed = if (reopen) ringFocused else now - startedAt >= RESTING_MILLIS
        return when {
            closed -> finish(Action.Done)
            now >= deadline -> finish(Action.GiveUp)
            else -> {
                rounds++
                index = 0
                next(now, ringFocused)
            }
        }
    }

    fun stop() {
        active = false
    }

    private fun finish(action: Action): Step {
        active = false
        return Step(action, 0)
    }

    companion object {
        /** 한 바퀴에 홈 → 뒤로를 몇 번. */
        const val PRESSES = 3

        const val PRESS_GAP_MILLIS = 100L

        /** 바퀴 끝에 울림 화면이 초점을 되찾을 때까지 기다리는 시간. */
        const val CHECK_WAIT_MILLIS = 400L

        /** 쉬는 중에 닫기를 되풀이하는 시간. 버튼을 누르고 있는 1.5초를 넉넉히 넘겨요. */
        const val RESTING_MILLIS = 2_500L

        /** 마지막 메뉴 신호 뒤 이만큼 지나면 멈춰요. */
        const val MAX_MILLIS = 5_000L
    }
}
