package com.lumian.morningup.core.model.engine

import kotlin.math.ceil

/**
 * 울리기 직전 폰 알람 볼륨이 가장 작으면(0) 최대의 30%로 올리고, 끈 뒤 원래대로 돌려요(결정 143, C-14).
 * W3 알림음에도 같이 써요(결정 145 대책 표).
 */
object VolumeFloor {
    const val FRACTION = 0.3

    /** 올릴 크기. [current]가 [min]보다 크면 그대로라 null이에요. */
    fun raisedTo(current: Int, min: Int, max: Int): Int? {
        if (current > min || max <= min) return null
        return ceil(max * FRACTION).toInt().coerceIn(min + 1, max)
    }

    /** 울리는 동안 사용자가 볼륨을 직접 바꿨으면 그 값을 두고, 우리가 올린 그대로면 원래대로 돌려요. */
    fun shouldRestore(current: Int, raised: Int): Boolean = current == raised
}
