package com.lumian.morningup.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 앱 데이터베이스 `morningup.db`. 기기 보호 저장소에 있어서 재부팅 뒤 잠금을 풀기 전에도 읽혀요(ADR 0002).
 * 구조를 바꾸면 version을 올리고 Migration과 테스트를 같이 더해요. 구조 파일은 `core/data/schemas`.
 */
@Database(
    entities = [AlarmEntity::class, DiagnosticLogEntity::class],
    version = 3,
    exportSchema = true,
)
abstract class MorningDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    abstract fun diagnosticLogDao(): DiagnosticLogDao

    companion object {
        const val NAME = "morningup.db"

        /** 앱이 여는 모든 구조 옮기기. 빌더에 이것만 넘겨요. */
        val MIGRATIONS: List<Migration> = listOf(Migration1To2, Migration2To3)
    }
}

/**
 * v1 → v2: `alarms.missions` 칸을 쉼표 이름 목록에서 미션 설정 JSON으로 바꿔요(4단계, 미션마다 단계와 개수).
 * 칸 이름과 종류는 그대로라 표 구조는 같고, 줄마다 글자만 고쳐요. 미션은 처음 값(1단계, 3문제 등)으로 채워요.
 */
object Migration1To2 : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val rows = buildList {
            db.query("SELECT id, missions FROM alarms").use { cursor ->
                while (cursor.moveToNext()) add(cursor.getLong(0) to cursor.getString(1).orEmpty())
            }
        }
        rows.forEach { (id, old) ->
            val updated = MissionCodec.encode(MissionCodec.decodeLegacy(old))
            db.execSQL("UPDATE alarms SET missions = ? WHERE id = ?", arrayOf<Any>(updated, id))
        }
    }
}

/** v2 → v3: S6 진동 패턴과 S7 부드럽게 깨우기 길이 칸을 더해요(7단계). 지금 알람은 기본, 30초로 채워요. */
@Suppress("MagicNumber") // 판 번호예요.
object Migration2To3 : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN vibrationPattern TEXT NOT NULL DEFAULT 'basic'")
        db.execSQL("ALTER TABLE alarms ADD COLUMN gentleSeconds INTEGER NOT NULL DEFAULT 30")
    }
}
