package com.lumian.morningup.core.data

import com.lumian.morningup.core.data.db.AlarmDao
import com.lumian.morningup.core.data.db.toEntity
import com.lumian.morningup.core.data.db.toModel
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.engine.AlarmStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 알람 목록 저장소. 화면은 [alarms]를 보고, 엔진은 [AlarmStore]로 써요. */
@Singleton
class AlarmRepository @Inject constructor(private val dao: AlarmDao) : AlarmStore {
    val alarms: Flow<List<Alarm>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun all(): List<Alarm> = dao.getAll().map { it.toModel() }

    override suspend fun get(id: Long): Alarm? = dao.get(id)?.toModel()

    override suspend fun save(alarm: Alarm) {
        dao.upsert(alarm.toEntity())
    }

    /** 새 알람이면 받은 ID를 넣어 돌려줘요. */
    suspend fun insert(alarm: Alarm): Alarm {
        val id = dao.upsert(alarm.toEntity())
        return if (alarm.id == 0L) alarm.copy(id = id) else alarm
    }

    suspend fun delete(id: Long) = dao.delete(id)
}
