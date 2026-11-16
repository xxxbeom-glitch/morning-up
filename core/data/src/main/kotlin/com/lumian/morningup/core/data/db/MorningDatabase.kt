package com.lumian.morningup.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * 앱 데이터베이스 `morningup.db`. 기기 보호 저장소에 있어서 재부팅 뒤 잠금을 풀기 전에도 읽혀요(ADR 0002).
 * 구조를 바꾸면 version을 올리고 Migration과 테스트를 같이 더해요. 구조 파일은 `core/data/schemas`.
 */
@Database(
    entities = [AlarmEntity::class, DiagnosticLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class MorningDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    abstract fun diagnosticLogDao(): DiagnosticLogDao

    companion object {
        const val NAME = "morningup.db"
    }
}
