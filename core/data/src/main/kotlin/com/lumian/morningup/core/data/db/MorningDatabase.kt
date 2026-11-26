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
    version = 5,
    exportSchema = true,
)
abstract class MorningDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    abstract fun diagnosticLogDao(): DiagnosticLogDao

    companion object {
        const val NAME = "morningup.db"

        /** 앱이 여는 모든 구조 옮기기. 빌더에 이것만 넘겨요. */
        val MIGRATIONS: List<Migration> = listOf(Migration1To2, Migration2To3, Migration3To4, Migration4To5)
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

/**
 * v3 → v4: S5에서 '사용 안 함'(진동만)을 빼서(11-19 사용자 결정), 그렇게 저장된 알람을 '랜덤'으로 바꿔요.
 * 표 구조는 그대로이고 글자만 고쳐요.
 */
@Suppress("MagicNumber") // 판 번호예요.
object Migration3To4 : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("UPDATE alarms SET soundId = 'random' WHERE soundId = 'silent'")
    }
}

/**
 * v4 → v5: W3 '한 번 더 확인', '확인까지', '묻는 횟수' 칸을 더해요(결정 180, 알람마다). 처음 값(켜짐, 3분, 1번)으로 채우고,
 * 옛 전체 설정 값은 설정 저장소가 한 번 모든 알람에 옮겨요(WakeCheckSettingsPrefs).
 */
@Suppress("MagicNumber") // 판 번호예요.
object Migration4To5 : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN recheckEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE alarms ADD COLUMN recheckWaitMinutes INTEGER NOT NULL DEFAULT 3")
        db.execSQL("ALTER TABLE alarms ADD COLUMN recheckCount INTEGER NOT NULL DEFAULT 1")
    }
}
