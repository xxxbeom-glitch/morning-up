package com.lumian.morningup.core.model.mission

import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import kotlin.math.sqrt

/**
 * 흔들기 한 번 알아채기(mission-levels.md Shake Shake, F17).
 * 가속도 센서 값(중력 포함, m/s²)의 크기가 중력의 [THRESHOLD_G]배를 넘으면 한 번으로 세요.
 * 팔을 크게 한 번 흔들면 넘고, 누워서 손목만 까딱하면 못 넘는 세기예요. [MIN_GAP_MILLIS] 안에 또 넘으면
 * 같은 흔들기(되돌아오는 반동이나 떨림)로 보고 세지 않아요.
 */
class ShakeCounter(
    private val thresholdG: Float = THRESHOLD_G,
    private val minGapMillis: Long = MIN_GAP_MILLIS,
) {
    private var lastCountAt: Long? = null

    /** 센서 값 하나. 한 번으로 셌으면 true. */
    fun onSample(x: Float, y: Float, z: Float, timeMillis: Long): Boolean {
        val g = sqrt(x * x + y * y + z * z) / GRAVITY
        if (g < thresholdG) return false
        val last = lastCountAt
        if (last != null && timeMillis - last < minGapMillis) return false
        lastCountAt = timeMillis
        return true
    }

    fun reset() {
        lastCountAt = null
    }

    companion object {
        const val GRAVITY = 9.80665f

        /**
         * 세기 기준(중력의 몇 배). 실기기(갤럭시 S24+, Z플립5)로 맞출 값이에요(F17). 숫자 하나만 바꾸면 돼요.
         * 2.5배는 흔히 쓰는 흔들기 알아채기 기준(2.5~2.7배)의 아래쪽이에요.
         */
        const val THRESHOLD_G = 2.5f

        /** 0.25초 안 두 번은 한 번. */
        const val MIN_GAP_MILLIS = 250L
    }
}

/** Shake Shake 남은 횟수. */
data class ShakeShakeState(val target: Int, val remaining: Int) {
    val finished: Boolean get() = remaining == 0
}

object ShakeShake {
    fun start(target: Int): ShakeShakeState {
        val count = target.coerceIn(MissionSetting.rangeOf(MissionType.ShakeShake).counts)
        return ShakeShakeState(target = count, remaining = count)
    }

    fun shake(state: ShakeShakeState): ShakeShakeState = state.copy(remaining = (state.remaining - 1).coerceAtLeast(0))
}
