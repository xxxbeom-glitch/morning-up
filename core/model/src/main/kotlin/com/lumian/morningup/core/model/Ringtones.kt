package com.lumian.morningup.core.model

/**
 * S5 알람음 목록 정의. 목록을 바꾸려면 이 파일과 `:core:alarm`의 `res/raw` 소리 파일만 고치면 돼요.
 * [Ringtone.id]는 알람에 저장되는 값이에요. 기본음은 [Alarm.DEFAULT_SOUND_ID]예요.
 * 소리 파일 이름은 `:core:alarm`의 `RingtoneFiles`가 id로 찾아요.
 */
data class Ringtone(val id: String, val name: String)

/** S5의 묶음. 탭 줄 이름과 목록 묶음 제목이 같아요. 비어 있는 묶음은 보여 주지 않아요. */
data class RingtoneGroup(val name: String, val ringtones: List<Ringtone>)

object Ringtones {
    val groups: List<RingtoneGroup> = listOf(
        RingtoneGroup(
            name = "기본",
            ringtones = listOf(
                Ringtone(Alarm.DEFAULT_SOUND_ID, "기본 알람음"),
                Ringtone("morning", "Morning"),
                Ringtone("morning_loop", "Morning Loop"),
            ),
        ),
        RingtoneGroup(
            name = "잔잔한",
            ringtones = listOf(
                Ringtone("love_serenade", "Love Serenade"),
                Ringtone("spring_season", "Spring Season"),
            ),
        ),
        RingtoneGroup(
            name = "경쾌한",
            ringtones = listOf(
                Ringtone("good_morning", "Good Morning"),
            ),
        ),
    )

    val all: List<Ringtone> get() = groups.flatMap { it.ringtones }

    /** 저장된 id에 맞는 소리. 목록에 없는 id(지운 소리 등)는 기본음으로 봐요. */
    fun find(id: String): Ringtone = all.firstOrNull { it.id == id } ?: all.first()
}
