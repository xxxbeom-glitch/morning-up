package com.lumian.morningup.core.alarm.ring

import com.lumian.morningup.core.alarm.BuildConfig

/**
 * 디버그 빌드 알람 소리 크기(11-17 음소거, 뒤에 10%, 11-18 사용자 지시로 100%. 지금은 출시 빌드와 같은 크기예요).
 * 폰의 볼륨이나 알람 설정은 건드리지 않아요. 크기를 정할 수 없는 대신 울림 알림 채널은 [MUTED]면 소리 없이 둬요.
 * 출시 빌드는 [MUTED]가 false라 영향이 없어요.
 */
internal object RingMute {
    val MUTED: Boolean = BuildConfig.DEBUG

    /** 디버그에서 앱이 내는 소리 비율. */
    private const val DEBUG_FACTOR = 1f

    fun volume(wanted: Float): Float = if (MUTED) wanted * DEBUG_FACTOR else wanted
}
