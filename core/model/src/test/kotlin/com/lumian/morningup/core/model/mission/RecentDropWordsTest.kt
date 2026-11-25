package com.lumian.morningup.core.model.mission

import com.google.common.truth.Truth.assertThat
import java.time.LocalDate
import kotlin.random.Random
import org.junit.Test

/** TSK-205: 최근 사흘 동안 낸 단어를 다음 날 같은 단계에서 빼고, 모자라면 오래된 것부터 다시 써요. */
class RecentDropWordsTest {
    private val day1 = LocalDate.of(2026, 11, 25)

    /** 메모리에만 두는 기록. 날짜는 [today]를 따라요. */
    private class MemoryHistory(var today: LocalDate) : DropWordHistory {
        var uses = emptyList<DropWordUse>()

        override fun recent(level: Int) = RecentDropWords.recent(uses, level, today)

        override fun record(level: Int, word: String) {
            uses = RecentDropWords.add(uses, DropWordUse(today, level, word))
        }
    }

    /** 하루 한 판: [count]개 단어를 차례로 내려요(맞히든 놓치든 내린 단어는 기록돼요). */
    private fun playDay(history: MemoryHistory, bank: List<String>, count: Int, seed: Int): List<String> {
        val drawn = mutableListOf<String>()
        val game = DropWordsGame(
            level = 1,
            speed = 1,
            target = count,
            words = bank,
            random = Random(seed),
            avoid = history.recent(1),
            onDrawn = {
                drawn += it
                history.record(1, it)
            },
        )
        var state = game.start(0L)
        var now = 0L
        // 속도 1은 한 번에 한 단어라, 바닥에 닿을 때마다 다음 단어가 내려와요.
        while (drawn.size < count) {
            now += DropWordsSpeed.fallMillis(1)
            state = game.tick(state, now)
        }
        return drawn.take(count)
    }

    private val bank60 = (1..60).map { "가" + ('가' + it) }

    @Test
    fun threeDaysInARow_doNotOverlap() {
        val history = MemoryHistory(day1)
        val days = (0 until 3).map { offset ->
            history.today = day1.plusDays(offset.toLong())
            playDay(history, bank60, count = 15, seed = offset)
        }
        assertThat(days[0].intersect(days[1].toSet())).isEmpty()
        assertThat(days[1].intersect(days[2].toSet())).isEmpty()
        assertThat(days[0].intersect(days[2].toSet())).isEmpty()
    }

    @Test
    fun fourthDay_mayReuseFirstDay_butNotLastTwo() {
        val history = MemoryHistory(day1)
        val days = (0 until 4).map { offset ->
            history.today = day1.plusDays(offset.toLong())
            playDay(history, bank60, count = 20, seed = offset)
        }
        // 넷째 날은 첫날이 사흘 밖으로 나가서 첫날 단어로 채워요. 어제, 그제 단어는 여전히 빠져요.
        assertThat(days[3].intersect(days[1].toSet())).isEmpty()
        assertThat(days[3].intersect(days[2].toSet())).isEmpty()
        assertThat(days[3].toSet()).isEqualTo(days[0].toSet())
    }

    @Test
    fun notEnoughFresh_reusesOldestFirst() {
        val bank = listOf("사과", "나무", "바다", "하늘", "구름")
        val history = MemoryHistory(day1)
        // 어제 '하늘', '바다', 오늘 아침 '사과' 순서로 냈어요.
        history.uses = listOf(
            DropWordUse(day1.minusDays(1), 1, "하늘"),
            DropWordUse(day1.minusDays(1), 1, "바다"),
            DropWordUse(day1, 1, "사과"),
        )
        val drawn = playDay(history, bank, count = 5, seed = 1)
        // 안 낸 두 단어가 먼저, 그다음 오래된 것부터예요.
        assertThat(drawn.take(2)).containsExactly("나무", "구름")
        assertThat(drawn.drop(2)).containsExactly("하늘", "바다", "사과").inOrder()
    }

    @Test
    fun recent_isPerLevel_andWindowIsThreeDays() {
        val uses = listOf(
            DropWordUse(day1.minusDays(3), 1, "오래"),
            DropWordUse(day1.minusDays(2), 1, "그제"),
            DropWordUse(day1.minusDays(2), 2, "다른"),
            DropWordUse(day1, 1, "오늘"),
        )
        assertThat(RecentDropWords.recent(uses, 1, day1)).containsExactly("그제", "오늘").inOrder()
        // 기록을 더하면 사흘 지난 줄은 지워져요.
        val added = RecentDropWords.add(uses, DropWordUse(day1, 1, "새것"))
        assertThat(added.map { it.word }).containsExactly("그제", "다른", "오늘", "새것").inOrder()
    }
}
