package com.lumian.morningup.core.alarm.guard

/**
 * 전원 메뉴를 닫을 때까지 다시 닫는 순서(ISS-060). 안드로이드와 상관없는 순수 로직이라 단위 시험으로 확인해요.
 *
 * 측면+볼륨 아래를 누르고 있는 동안(1.5초쯤)은 뒤로, 홈이 무시돼요. 그래서 한 번 보내고 끝내지 않고
 * 처음에 뒤로를 한 번 보내고, 그 뒤로는 [RETRY_MILLIS]마다 홈만 보내요.
 * (뒤로를 되풀이하면 메뉴가 이미 닫힌 뒤 미션 화면의 '닫기'가 눌릴 수 있어요.) 메뉴가 사라졌는지는 화면 내용을 읽지 않고(P6 약속)
 * 시스템 창이 아닌 다른 창(홈 화면 등)으로 바뀌었다는 신호로만 알아요. 홈으로 나가면 시스템이 전원 메뉴를 모두 닫으므로,
 * 그 신호가 오면 닫힌 거예요. [MAX_MILLIS]가 지나도 신호가 없으면 멈춰요.
 * 다시 닫는 중에 메뉴 신호가 또 오면 새로 시작하지 않고, 기다리는 시간만 다시 늘려요.
 */
class PowerMenuCloser {
    enum class Action { Back, Home, Done, GiveUp }

    var active: Boolean = false
        private set
    var tries: Int = 0
        private set

    private var gone = false
    private var deadline = 0L

    /** 전원 메뉴 창 신호. 처음이면 true(반복을 시작해야 해요), 이미 닫는 중이면 false. */
    fun onMenuSeen(now: Long): Boolean {
        gone = false
        deadline = now + MAX_MILLIS
        if (active) return false
        active = true
        tries = 0
        return true
    }

    /** 시스템 창이 아닌 창으로 바뀐 신호(홈 화면 등). 메뉴가 닫혔다고 봐요. */
    fun onOtherWindow() {
        if (active) gone = true
    }

    /** 다음에 할 일. [Action.Done], [Action.GiveUp]이면 반복을 끝내요. */
    fun next(now: Long): Action {
        val action = when {
            !active -> return Action.Done
            gone -> Action.Done
            now >= deadline -> Action.GiveUp
            tries == 0 -> Action.Back
            else -> Action.Home
        }
        if (action == Action.Done || action == Action.GiveUp) active = false else tries++
        return action
    }

    fun stop() {
        active = false
    }

    companion object {
        /** 다시 닫는 간격. 버튼을 떼자마자 닫히게 짧게 둬요. */
        const val RETRY_MILLIS = 300L

        /** 마지막 메뉴 신호 뒤로 이만큼 다시 닫아 봐요. 버튼을 오래 누르고 있어도 넘기게 넉넉히 둬요. */
        const val MAX_MILLIS = 5_000L
    }
}
