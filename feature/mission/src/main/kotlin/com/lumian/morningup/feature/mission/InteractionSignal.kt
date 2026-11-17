package com.lumian.morningup.feature.mission

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 미션 조작 신호 묶기. 키를 누를 때마다 서비스에 보내면 너무 잦아서,
 * 처음 한 번은 바로 보내고 [windowMillis] 안에 더 누른 것은 창이 끝날 때 한 번으로 묶어 보내요.
 * 엔진의 59초는 늘 마지막 조작보다 늦게(또는 같게) 끝나서, 화면 타이머보다 먼저 끝나지 않아요.
 */
class InteractionSignal(
    private val scope: CoroutineScope,
    private val windowMillis: Long = WINDOW_MILLIS,
    private val send: () -> Unit,
) {
    private var window: Job? = null
    private var pending = false

    fun poke() {
        if (window?.isActive == true) {
            pending = true
            return
        }
        send()
        window = scope.launch {
            delay(windowMillis)
            while (pending) {
                pending = false
                send()
                delay(windowMillis)
            }
        }
    }

    /** 미션이 끝나거나 다시 시작될 때 남은 신호를 버려요. */
    fun cancel() {
        pending = false
        window?.cancel()
        window = null
    }

    companion object {
        const val WINDOW_MILLIS = 2_000L
    }
}
