package com.lumian.morningup.core.model.engine

/**
 * 강제 종료 흔적(TSK-177 답 6, B-03). 강제 종료하면 안드로이드가 예약을 모두 지워서, 앱을 다시 열 때까지 알람이 안 울려요.
 * 켜진 알람이 있을 때만 알려요.
 * - 안드로이드 11 이상: 지난번에 연 뒤 '사용자 요청으로 닫힘' 기록이 있으면([userStopSinceLastOpen]) 강제 종료로 봐요.
 * - 그 아래: 닫힘 기록을 못 읽어서(null), 켜진 알람의 예약이 모두 사라졌는지([scheduleWiped])로 대신 봐요.
 */
object ForceStopPolicy {
    fun detected(hasEnabledAlarm: Boolean, userStopSinceLastOpen: Boolean?, scheduleWiped: Boolean): Boolean =
        hasEnabledAlarm && (userStopSinceLastOpen ?: scheduleWiped)
}
