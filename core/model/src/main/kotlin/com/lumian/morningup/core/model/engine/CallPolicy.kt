package com.lumian.morningup.core.model.engine

/** 실제로 낼 소리 크기(0~1)와 진동. */
data class SoundOutput(val volume: Float, val vibrate: Boolean)

/**
 * 통화 중 알람(2단계에서 미룬 것). 통화 중에 알람이 제 크기로 울리면 귀에 대고 있던 사람이 놀라고 통화도 방해돼요.
 * 그래서 통화 중에는 소리를 [CALL_VOLUME] 이하로 줄이고 진동으로 알려요. 통화가 끝나면 원래 값으로 돌아가요.
 */
object CallPolicy {
    /** 통화 중 최대 소리 크기(알람 볼륨 기준이 아니라 소리 통로 전체 기준 비율). */
    const val CALL_VOLUME = 0.1f

    fun output(requested: SoundOutput, inCall: Boolean): SoundOutput =
        if (!inCall) {
            requested
        } else {
            SoundOutput(volume = minOf(requested.volume, CALL_VOLUME), vibrate = true)
        }
}
