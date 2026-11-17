package com.lumian.morningup.core.testing

import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.AlarmBook
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** 메모리에만 두는 가짜 알람 창구. 화면 테스트에서 무엇이 불렸는지도 기록해요. */
class FakeAlarmBook(initial: List<Alarm> = emptyList()) : AlarmBook {
    private val state = MutableStateFlow(initial.sortedWith(ORDER))
    override val alarms: StateFlow<List<Alarm>> = state

    /** 저장, 켜고 끄기, 삭제, 되돌리기 호출 기록. 예: "save:1", "enable:1:false". */
    val calls = mutableListOf<String>()

    private var nextId = (initial.maxOfOrNull { it.id } ?: 0L) + 1

    override suspend fun get(id: Long): Alarm? = state.value.firstOrNull { it.id == id }

    override suspend fun save(alarm: Alarm): Alarm {
        val stored = if (alarm.id == 0L) alarm.copy(id = nextId++) else alarm
        put(stored)
        calls += "save:${stored.id}"
        return stored
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        get(id)?.let { put(it.copy(enabled = enabled)) }
        calls += "enable:$id:$enabled"
    }

    override suspend fun delete(id: Long): Alarm? {
        val alarm = get(id)
        state.update { list -> list.filterNot { it.id == id } }
        calls += "delete:$id"
        return alarm
    }

    override suspend fun restore(alarm: Alarm) {
        put(alarm)
        calls += "restore:${alarm.id}"
    }

    private fun put(alarm: Alarm) = state.update { list ->
        (list.filterNot { it.id == alarm.id } + alarm).sortedWith(ORDER)
    }

    private companion object {
        val ORDER = compareBy<Alarm>({ it.time }, { it.id })
    }
}
