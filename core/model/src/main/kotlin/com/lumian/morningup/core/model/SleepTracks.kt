package com.lumian.morningup.core.model

/**
 * 잠들기 콘텐츠 하나(Z1 타일, Z2 재생). [palette]는 썸네일 B+ 색 3개(ARGB)이고 Z2 배경도 이 색을 이어 써요.
 * 소리 파일은 아직 없어요(8단계는 소리 없이 동작). 나중에 파일만 넣으면 [id]로 찾게 해요.
 */
data class SleepTrack(val id: String, val name: String, val minutes: Int, val palette: List<Long>)

/** Z1 묶음. 탭 줄 이름과 묶음 제목이 같아요. */
data class SleepGroup(val name: String, val tracks: List<SleepTrack>)

/**
 * Z1, Z2 목업 목록. 묶음 이름은 상황별(결정 104)이고, 묶음별 트랙 구성은 아직 정하지 않아 예전 장르 트랙을 그대로 둬요.
 * 목록을 바꾸려면 이 파일만 고쳐요.
 */
@Suppress("MagicNumber") // 길이와 색 자체가 데이터예요.
object SleepTracks {
    val groups: List<SleepGroup> = listOf(
        SleepGroup(
            "잠에 들기 어려울 때",
            listOf(
                SleepTrack("night_rain", "창문을 두드리는 밤비", 60, listOf(0xFF3E6E9A, 0xFF2C7A86, 0xFF9DB4E0)),
                SleepTrack("waves", "멀리서 밀려오는 파도", 45, listOf(0xFF4FB3B0, 0xFF2B5FA8, 0xFF7FD0C8)),
                SleepTrack("forest_birds", "새벽 숲의 새소리", 30, listOf(0xFF5E8F4A, 0xFFC9B85A, 0xFF3F7A6A)),
                SleepTrack("campfire", "타닥이는 모닥불", 50, listOf(0xFFC2622E, 0xFFD9708A, 0xFF5A2A2A)),
            ),
        ),
        SleepGroup(
            "마음이 답답할 때",
            listOf(
                SleepTrack("deep_night", "깊은 밤, 고요한 숨", 52, listOf(0xFF5B3FD1, 0xFFC05AA0, 0xFF2A2F7A)),
                SleepTrack("cloud_walk", "구름 위를 천천히 걷기", 38, listOf(0xFFE6A8D0, 0xFF8FB8E8, 0xFFC8B8F0)),
                SleepTrack("slow_breath", "느리게 쉬는 숨", 20, listOf(0xFF8AA8E0, 0xFFA894D8, 0xFF7FC8C0)),
                SleepTrack("starry_field", "별빛 아래 들판", 45, listOf(0xFF22306A, 0xFFE8C860, 0xFF4A3A8A)),
            ),
        ),
        SleepGroup(
            "생각이 복잡할 때",
            listOf(
                SleepTrack("quiet_room", "고요한 방", 30, listOf(0xFF7A6A9A, 0xFFD8C8B8, 0xFF4A4A6A)),
                SleepTrack("moon_walk", "달빛 산책", 40, listOf(0xFF34507A, 0xFFE0E4F0, 0xFF6A7AB0)),
                SleepTrack("dawn_waltz", "새벽 왈츠", 35, listOf(0xFFB88AC8, 0xFF5A6AA8, 0xFFF0C8B0)),
                SleepTrack("rain_sonata", "빗방울 소나타", 42, listOf(0xFF5A8AB8, 0xFF9AB8D8, 0xFF3A4A7A)),
            ),
        ),
        SleepGroup(
            "아침 5분 명상",
            listOf(
                SleepTrack("two_am", "새벽 2시", 60, listOf(0xFF5C3A6A, 0xFFE07A9A, 0xFF2A2A5A)),
                SleepTrack("alley_cafe", "골목 카페", 45, listOf(0xFFB0784A, 0xFFE8C08A, 0xFF5A3A3A)),
                SleepTrack("night_bus", "밤 버스", 40, listOf(0xFF2E4F6A, 0xFFE0A84A, 0xFF4A3A7A)),
                SleepTrack("slow_afternoon", "느린 오후", 30, listOf(0xFF8AA86A, 0xFFE8D89A, 0xFF5A7A6A)),
            ),
        ),
    )

    val all: List<SleepTrack> get() = groups.flatMap { it.tracks }

    fun find(id: String?): SleepTrack? = all.firstOrNull { it.id == id }

    fun groupOf(id: String?): SleepGroup? = groups.firstOrNull { g -> g.tracks.any { it.id == id } }

    /** Z2-1 종료 타이머 고르기(분). null은 '끄기'. */
    val TIMER_MINUTES: List<Int?> = listOf(null, 15, 30, 45, 60, 90)
}
