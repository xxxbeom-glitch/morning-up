package com.lumian.morningup.core.model

/**
 * S5 알람음 목록 정의. 목록을 바꾸려면 이 파일과 `:core:alarm`의 `res/raw` 소리 파일만 고치면 돼요.
 * [Ringtone.id]는 알람에 저장되는 값이에요. 맨 위는 '랜덤'([Alarm.RANDOM_SOUND_ID], 새 알람의 처음 값)이고,
 * 그다음이 앱 기본음 [Alarm.DEFAULT_SOUND_ID]예요.
 * 소리 파일 이름은 `:core:alarm`의 `RingtoneFiles`가 id로 찾아요.
 * 화면 이름은 `:core:alarm`의 `res/values/sound_names.xml`에 있어요
 * (11-18 사용자 지시, TSK-273). `SoundNames`가 id로 찾아요. 원래 프랑스어, 이탈리아어 제목은 파일 이름과 id에 남아요.
 */
data class Ringtone(val id: String)

/** S5의 묶음. [key]로 화면 이름(탭 줄, 묶음 제목)을 찾아요. 비어 있는 묶음은 보여 주지 않아요. */
data class RingtoneGroup(val key: String, val ringtones: List<Ringtone>)

object Ringtones {
    val groups: List<RingtoneGroup> = listOf(
        RingtoneGroup(
            key = "basic",
            ringtones = listOf(
                Ringtone(Alarm.RANDOM_SOUND_ID),
                Ringtone(Alarm.DEFAULT_SOUND_ID),
            ),
        ),
        RingtoneGroup(
            key = "calm",
            ringtones = listOf(
                Ringtone("aube_paisible"),
                Ringtone("douceur"),
                Ringtone("primavera"),
                Ringtone("petit_bois"),
                Ringtone("gouttes_de_bois"),
            ),
        ),
        RingtoneGroup(
            key = "upbeat",
            ringtones = listOf(
                Ringtone("bonjour_soleil"),
                Ringtone("buongiorno"),
                Ringtone("joie_de_vivre"),
                Ringtone("allegro_vivace"),
                Ringtone("presto"),
                Ringtone("petits_rebonds"),
                Ringtone("route_de_campagne"),
                Ringtone("soleil_mineur"),
            ),
        ),
        RingtoneGroup(
            key = "loud",
            ringtones = listOf(
                Ringtone("fortissimo"),
                Ringtone("nuit_blanche"),
                Ringtone("tambour_battant"),
                Ringtone("neon_rouge"),
            ),
        ),
        RingtoneGroup(
            key = "grand",
            ringtones = listOf(
                Ringtone("maestoso"),
                Ringtone("marche_triomphale"),
                Ringtone("grandeur"),
            ),
        ),
        RingtoneGroup(
            key = "mystic",
            ringtones = listOf(
                Ringtone("reverie"),
                Ringtone("danse_des_ombres"),
            ),
        ),
        RingtoneGroup(
            key = "christmas",
            ringtones = listOf(
                Ringtone("joyeux_noel"),
                Ringtone("nuit_de_noel"),
            ),
        ),
    )

    val all: List<Ringtone> get() = groups.flatMap { it.ringtones }

    /** 이름을 바꾸기 전(11-17) id. 같은 곡이라, 이미 저장된 알람은 새 id로 이어서 울려요. */
    private val renamed: Map<String, String> = mapOf(
        "morning" to "petits_rebonds",
        "morning_loop" to "douceur",
        "love_serenade" to "aube_paisible",
        "spring_season" to "primavera",
        "good_morning" to "gouttes_de_bois",
    )

    /** 옛 id를 지금 id로 바꿔요. 옛 id가 아니면 그대로예요. */
    fun currentId(id: String): String = renamed[id] ?: id

    /** 저장된 id에 맞는 소리. 목록에 없는 id(지운 소리 등)는 기본음으로 봐요. */
    fun find(id: String): Ringtone = currentId(id).let { cur -> all.firstOrNull { it.id == cur } }
        ?: all.first { it.id == Alarm.DEFAULT_SOUND_ID }

    /** '랜덤'이 고를 수 있는 진짜 벨소리 id(랜덤과 기본음 제외). */
    val randomPool: List<String>
        get() = all.map { it.id }.filter { it != Alarm.RANDOM_SOUND_ID && it != Alarm.DEFAULT_SOUND_ID }
}
