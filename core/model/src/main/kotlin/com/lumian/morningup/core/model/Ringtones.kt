package com.lumian.morningup.core.model

/**
 * S5 알람음 목록 정의. 목록을 바꾸려면 이 파일과 `:core:alarm`의 `res/raw` 소리 파일만 고치면 돼요.
 * [Ringtone.id]는 알람에 저장되는 값이에요. 맨 위는 '랜덤'([Alarm.RANDOM_SOUND_ID], 새 알람의 처음 값)이고,
 * 그다음이 앱 기본음 [Alarm.DEFAULT_SOUND_ID]예요.
 * 소리 파일 이름은 `:core:alarm`의 `RingtoneFiles`가 id로 찾아요.
 * [name]은 화면에 보이는 한글 이름이에요(11-18 사용자 지시). 원래 프랑스어, 이탈리아어 제목은 파일 이름과 id에 남아요.
 */
data class Ringtone(val id: String, val name: String)

/** S5의 묶음. 탭 줄 이름과 목록 묶음 제목이 같아요. 비어 있는 묶음은 보여 주지 않아요. */
data class RingtoneGroup(val name: String, val ringtones: List<Ringtone>)

object Ringtones {
    val groups: List<RingtoneGroup> = listOf(
        RingtoneGroup(
            name = "기본",
            ringtones = listOf(
                Ringtone(Alarm.RANDOM_SOUND_ID, "랜덤"),
                Ringtone(Alarm.DEFAULT_SOUND_ID, "기본 알람음"),
            ),
        ),
        RingtoneGroup(
            name = "잔잔한",
            ringtones = listOf(
                Ringtone("aube_paisible", "고요한 새벽빛"),
                Ringtone("douceur", "보드라운 아침"),
                Ringtone("primavera", "봄날의 창가"),
                Ringtone("petit_bois", "아침 숲 산책"),
                Ringtone("gouttes_de_bois", "나뭇잎 이슬"),
            ),
        ),
        RingtoneGroup(
            name = "경쾌한",
            ringtones = listOf(
                Ringtone("bonjour_soleil", "햇살 인사"),
                Ringtone("buongiorno", "굿모닝 카페"),
                Ringtone("joie_de_vivre", "콧노래"),
                Ringtone("allegro_vivace", "가벼운 발걸음"),
                Ringtone("presto", "바쁜 아침"),
                Ringtone("petits_rebonds", "말랑 통통"),
                Ringtone("route_de_campagne", "시골길 드라이브"),
                Ringtone("soleil_mineur", "구름 낀 햇살"),
            ),
        ),
        RingtoneGroup(
            name = "시끄러운",
            ringtones = listOf(
                Ringtone("fortissimo", "정신 번쩍"),
                Ringtone("nuit_blanche", "벌떡 기상"),
                Ringtone("tambour_battant", "북소리 행진"),
                Ringtone("neon_rouge", "빨간 경보"),
            ),
        ),
        RingtoneGroup(
            name = "웅장한",
            ringtones = listOf(
                Ringtone("maestoso", "왕의 아침"),
                Ringtone("marche_triomphale", "개선 행진"),
                Ringtone("grandeur", "장엄한 서막"),
            ),
        ),
        RingtoneGroup(
            name = "신비로운",
            ringtones = listOf(
                Ringtone("reverie", "꿈결"),
                Ringtone("danse_des_ombres", "그림자 왈츠"),
            ),
        ),
        RingtoneGroup(
            name = "크리스마스",
            ringtones = listOf(
                Ringtone("joyeux_noel", "산타의 종소리"),
                Ringtone("nuit_de_noel", "눈 내리는 밤"),
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
