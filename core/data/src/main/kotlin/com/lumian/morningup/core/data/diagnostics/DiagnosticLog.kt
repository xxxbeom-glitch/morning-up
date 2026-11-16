package com.lumian.morningup.core.data.diagnostics

import com.lumian.morningup.core.data.db.DiagnosticLogDao
import com.lumian.morningup.core.data.db.DiagnosticLogEntity
import javax.inject.Inject
import javax.inject.Singleton

/** 알람 사건 기록(예약, 울림, 꺼짐, 놓침, 대신 예약). 최근 [MAX_ENTRIES]줄만 남겨요. */
@Singleton
class DiagnosticLog @Inject constructor(private val dao: DiagnosticLogDao) {
    suspend fun record(at: Long, event: String, alarmId: Long? = null, detail: String = "") {
        dao.append(DiagnosticLogEntity(at = at, event = event, alarmId = alarmId, detail = detail), MAX_ENTRIES)
    }

    suspend fun entries(): List<DiagnosticLogEntity> = dao.all()

    companion object {
        const val MAX_ENTRIES = 500
    }
}
