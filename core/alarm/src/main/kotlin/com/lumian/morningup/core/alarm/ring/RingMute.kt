package com.lumian.morningup.core.alarm.ring

import com.lumian.morningup.core.alarm.BuildConfig

/**
 * 디버그 빌드에서는 알람 소리를 0으로 내요(11-17 사용자 지시: 개발 중 폰 시험 때 벨소리 음소거).
 * 폰의 볼륨이나 알람 설정은 건드리지 않고 앱이 내는 소리 크기만 0으로 해요. 진동은 그대로 둬서
 * 울리는지는 손으로 알 수 있어요. 출시 빌드는 [MUTED]가 false라 영향이 없어요.
 */
internal object RingMute {
    val MUTED: Boolean = BuildConfig.DEBUG

    fun volume(wanted: Float): Float = if (MUTED) 0f else wanted
}
