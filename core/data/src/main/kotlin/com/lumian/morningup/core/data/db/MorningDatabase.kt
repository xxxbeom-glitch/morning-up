package com.lumian.morningup.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lumian.morningup.core.model.WakeCheckSettings

/**
 * 앱 데이터베이스 `morningup.db`. 기기 보호 저장소에 있어서 재부팅 뒤 잠금을 풀기 전에도 읽혀요(ADR 0002).
 * 구조를 바꾸면 version을 올리고 Migration과 테스트를 같이 더해요. 구조 파일은 `core/data/schemas`.
 */
@Database(
    entities = [AlarmEntity::class, DiagnosticLogEntity::class],
    version = 7,
    exportSchema = true,
)
abstract class MorningDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    abstract fun diagnosticLogDao(): DiagnosticLogDao

    companion object {
        const val NAME = "morningup.db"

        /**
         * 앱이 여는 모든 구조 옮기기. 빌더에 이것만 넘겨요. [legacyWakeCheck]는 v4 → v5에서 알람에 넣을 옛 전체
         * 다시 잠들기 방지 값이에요(앱은 설정 저장소에서 읽어요, 결정 180).
         */
        fun migrations(legacyWakeCheck: () -> LegacyWakeCheck = { LegacyWakeCheck() }): List<Migration> =
            listOf(
                Migration1To2,
                Migration2To3,
                Migration3To4,
                Migration4To5(legacyWakeCheck),
                Migration5To6,
                Migration6To7,
            )
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
 * v4 → v5에서 모든 알람에 넣을 옛 전체 다시 잠들기 방지 값(결정 180). [askAfterMinutes]와 [answerSeconds]가 null이면
 * 결정 145 전 판이라 알람마다 칸에 남은 값을 그대로 둬요. 처음 값은 새로 깐 앱과 같아요.
 */
data class LegacyWakeCheck(
    val askAfterMinutes: Int? = null,
    val answerSeconds: Int? = null,
    val recheckEnabled: Boolean = true,
    val recheckWaitMinutes: Int = WakeCheckSettings.DEFAULT_RECHECK_WAIT_MINUTES,
    val recheckCount: Int = WakeCheckSettings.DEFAULT_RECHECK_COUNT,
)

/**
 * v4 → v5: W3 '한 번 더 확인', '확인까지', '묻는 횟수' 칸을 더하고(결정 180, 알람마다), 옛 전체 값을 같은 자리에서 모든
 * 알람에 넣어요. DB가 열리기 전에 끝나서, 앱이 알람을 먼저 읽고 다시 써도 처음 값으로 덮이지 않아요(ISS-055).
 */
@Suppress("MagicNumber") // 판 번호예요.
class Migration4To5(private val legacy: () -> LegacyWakeCheck) : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE alarms ADD COLUMN recheckEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE alarms ADD COLUMN recheckWaitMinutes INTEGER NOT NULL DEFAULT 3")
        db.execSQL("ALTER TABLE alarms ADD COLUMN recheckCount INTEGER NOT NULL DEFAULT 1")
        val values = legacy()
        db.execSQL(
            "UPDATE alarms SET recheckEnabled = ?, recheckWaitMinutes = ?, recheckCount = ?",
            arrayOf<Any>(if (values.recheckEnabled) 1 else 0, values.recheckWaitMinutes, values.recheckCount),
        )
        val ask = values.askAfterMinutes
        val answer = values.answerSeconds
        if (ask != null && answer != null) {
            db.execSQL(
                "UPDATE alarms SET wakeCheckAskAfterMinutes = ?, wakeCheckAnswerSeconds = ?",
                arrayOf<Any>(ask, answer),
            )
        }
    }
}

/**
 * v5 → v6: '다시 잠들기 방지' 시트를 두 줄로 줄여서(결정 183) W2 대답할 시간은 1분, W3까지 기다리는 시간은 3분으로
 * 고정해요. 옛 값(대답할 시간 30초, 3분 / 확인까지 1분, 5분)을 모든 알람에서 고정값으로 바꿔요. 표 구조는 그대로예요.
 * '한 번 더 확인' 끔은 그대로 끔이고 시트에서 '안 함'으로 보여요. 켬이면 횟수도 그대로예요.
 */
@Suppress("MagicNumber") // 판 번호예요.
object Migration5To6 : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "UPDATE alarms SET wakeCheckAnswerSeconds = ?, recheckWaitMinutes = ?",
            arrayOf<Any>(WakeCheckSettings.FIXED_ANSWER_SECONDS, WakeCheckSettings.FIXED_RECHECK_WAIT_MINUTES),
        )
    }
}

/**
 * v6 → v7: '한 번 더 확인' 줄을 '횟수'로 바꾸고 '안 함' 칸을 없앴어요(결정 186). 켜고 끄기는 실험실 스위치가 맡아요.
 * 옛 '안 함'(끔)은 횟수 1(켬)로 옮기고, 켬이던 알람은 횟수를 그대로 둬요. 표 구조는 그대로예요.
 */
@Suppress("MagicNumber") // 판 번호예요.
object Migration6To7 : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "UPDATE alarms SET recheckEnabled = 1, recheckCount = ? WHERE recheckEnabled = 0 OR recheckCount < ?",
            arrayOf<Any>(WakeCheckSettings.MIN_RECHECK_COUNT, WakeCheckSettings.MIN_RECHECK_COUNT),
        )
    }
}
