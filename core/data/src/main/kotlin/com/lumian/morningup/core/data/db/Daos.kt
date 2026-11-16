package com.lumian.morningup.core.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    fun observeAll(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms ORDER BY hour, minute, id")
    suspend fun getAll(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun get(id: Long): AlarmEntity?

    @Upsert
    suspend fun upsert(alarm: AlarmEntity): Long

    @Query("DELETE FROM alarms WHERE id = :id")
    suspend fun delete(id: Long)
}

/** 기기 안 진단 기록 한 줄(예약, 울림, 꺼짐, 놓침 같은 알람 사건). */
@Entity(tableName = "diagnostic_log")
data class DiagnosticLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    val event: String,
    val alarmId: Long?,
    val detail: String,
)

@Dao
interface DiagnosticLogDao {
    @Insert
    suspend fun insert(entry: DiagnosticLogEntity)

    @Query("DELETE FROM diagnostic_log WHERE id NOT IN (SELECT id FROM diagnostic_log ORDER BY id DESC LIMIT :keep)")
    suspend fun trimTo(keep: Int)

    @Transaction
    suspend fun append(entry: DiagnosticLogEntity, keep: Int) {
        insert(entry)
        trimTo(keep)
    }

    @Query("SELECT * FROM diagnostic_log ORDER BY id DESC")
    suspend fun all(): List<DiagnosticLogEntity>
}
