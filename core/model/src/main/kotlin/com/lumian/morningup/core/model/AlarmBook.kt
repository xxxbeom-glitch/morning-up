package com.lumian.morningup.core.model

import kotlinx.coroutines.flow.Flow

/**
 * 화면이 알람을 다루는 유일한 창구. 저장, 켜고 끄기, 삭제, 되돌리기를 하면 예약도 같이 맞춰요.
 * 구현은 `:core:alarm`의 SchedulingAlarmBook이고, 화면 테스트는 가짜를 써요.
 */
interface AlarmBook {
    /** 시각 순서로 정렬된 알람 목록. */
    val alarms: Flow<List<Alarm>>

    suspend fun get(id: Long): Alarm?

    /** 새 알람이면 ID를 받아 돌려줘요. 저장 뒤 다음 울릴 시각을 예약해요. */
    suspend fun save(alarm: Alarm): Alarm

    suspend fun setEnabled(id: Long, enabled: Boolean)

    /** 지우고 예약을 취소해요. 되돌리기용으로 지운 알람을 돌려줘요. */
    suspend fun delete(id: Long): Alarm?

    /** 지운 알람을 같은 ID로 되살리고 다시 예약해요. */
    suspend fun restore(alarm: Alarm)
}
