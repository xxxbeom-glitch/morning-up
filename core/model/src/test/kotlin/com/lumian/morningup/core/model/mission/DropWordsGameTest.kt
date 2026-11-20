package com.lumian.morningup.core.model.mission

import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test

/** Drop Words: 속도표, 이어 치기, 정답, 틀림, 바닥에 닿음. */
class DropWordsGameTest {
    private val bank = listOf("사과", "나무", "바다", "하늘", "구름", "기차", "우유", "모자", "시계", "가방")
    private val t0 = 1_000_000L

    private fun game(speed: Int = 1, target: Int = 5, words: List<String> = bank) =
        DropWordsGame(level = 1, speed = speed, target = target, words = words, random = Random(4))

    private fun DropWordsGame.typeWord(
        state: DropWordsState,
        word: String,
        now: Long,
    ): Pair<DropWordsState, DropInput> {
        var s = state
        var last: DropInput = DropInput.Ignored
        word.forEach { ch ->
            val (next, result) = type(s, ch.toString(), now)
            s = next
            last = result
        }
        return s to last
    }

    @Test
    fun speedTable_matchesMissionLevels() {
        assertThat(
            (1..4).map {
                DropWordsSpeed.fallMillis(it)
            },
        ).containsExactly(10_000L, 8_000L, 6_000L, 4_000L).inOrder()
        assertThat((1..4).map { DropWordsSpeed.maxWords(it) }).containsExactly(1, 2, 2, 3).inOrder()
    }

    @Test
    fun start_dropsOneWord_andKeypadHasItsSyllables() {
        val s = game().start(t0)
        assertThat(s.words).hasSize(1)
        assertThat(s.correct).isEqualTo(0)
        assertThat(s.target).isEqualTo(5)
        assertThat(s.keypad).hasSize(DropKeypadLayout.of(s.level).size)
        s.words.single().text.forEach { assertThat(s.keypad).contains(it.toString()) }
    }

    @Test
    fun speed4_keepsUpToThreeWords_spawnedInTurn() {
        val g = game(speed = 4)
        var s = g.start(t0)
        s = g.tick(s, t0 + 100)
        assertThat(s.words).hasSize(1)
        s = g.tick(s, t0 + 1_400)
        assertThat(s.words).hasSize(2)
        s = g.tick(s, t0 + 2_800)
        assertThat(s.words).hasSize(3)
        s = g.tick(s, t0 + 3_900)
        assertThat(s.words).hasSize(3)
        assertThat(s.words.map { it.lane }.toSet()).hasSize(3)
        assertThat(s.words.map { it.text }.toSet()).hasSize(3)
    }

    @Test
    fun typingPrefix_keepsInput_andWholeWord_countsCorrect() {
        val g = game()
        val s0 = g.start(t0)
        val word = s0.words.single().text
        val (s1, r1) = g.type(s0, word.first().toString(), t0 + 10)
        assertThat(r1).isEqualTo(DropInput.Typing)
        assertThat(s1.input).isEqualTo(word.first().toString())
        val (s2, r2) = g.typeWord(s0, word, t0 + 10)
        assertThat(r2).isEqualTo(DropInput.Correct(word))
        assertThat(s2.correct).isEqualTo(1)
        assertThat(s2.input).isEmpty()
        assertThat(s2.words.map { it.text }).doesNotContain(word)
    }

    @Test
    fun correct_rebuildsKeypad_withNewWordSyllables() {
        val g = game()
        val s0 = g.start(t0)
        val (s1, _) = g.typeWord(s0, s0.words.single().text, t0 + 10)
        // 화면이 비면 바로 새 단어가 내려와요.
        assertThat(s1.words).hasSize(1)
        s1.words.single().text.forEach { assertThat(s1.keypad).contains(it.toString()) }
        assertThat(s1.keypad).isNotEqualTo(s0.keypad)
    }

    @Test
    fun wrongSyllable_clearsInput_withoutPenalty() {
        val g = game()
        val s0 = g.start(t0)
        val word = s0.words.single().text
        val (typed, _) = g.type(s0, word.first().toString(), t0)
        val wrong = s0.keypad.first { key -> !word.startsWith(word.first() + key) }
        val (s1, r) = g.type(typed, wrong, t0)
        assertThat(r).isEqualTo(DropInput.Wrong)
        assertThat(s1.input).isEmpty()
        assertThat(s1.correct).isEqualTo(typed.correct)
        assertThat(s1.keypad).isEqualTo(typed.keypad)
    }

    @Test
    fun delete_removesLastSyllable() {
        val g = game()
        val s0 = g.start(t0)
        val (typed, _) = g.type(s0, s0.words.single().text.first().toString(), t0)
        assertThat(g.delete(typed).input).isEmpty()
        assertThat(g.delete(g.delete(typed)).input).isEmpty()
    }

    @Test
    fun wordReachingFloor_disappears_withoutPenalty_andNewWordFalls() {
        val g = game(speed = 1)
        val s0 = g.start(t0)
        val first = s0.words.single()
        val (typed, _) = g.type(s0, first.text.first().toString(), t0 + 100)
        val s1 = g.tick(typed, t0 + 10_000)
        assertThat(s1.words.map { it.id }).doesNotContain(first.id)
        assertThat(s1.correct).isEqualTo(0)
        // 치던 단어가 사라지면 입력도 조용히 비워요.
        assertThat(s1.input).isEmpty()
        assertThat(s1.words).hasSize(1)
        s1.words.single().text.forEach { assertThat(s1.keypad).contains(it.toString()) }
    }

    @Test
    fun keypad_doesNotChangeWhileTyping_evenWhenNewWordArrives() {
        val g = game(speed = 4)
        val s0 = g.start(t0)
        val (typed, _) = g.type(s0, s0.words.single().text.first().toString(), t0 + 10)
        val s1 = g.tick(typed, t0 + 1_400)
        assertThat(s1.words).hasSize(2)
        assertThat(s1.keypad).isEqualTo(typed.keypad)
        // 입력을 비우면 빠진 음절이 들어와요.
        val s2 = g.tick(g.delete(s1), t0 + 1_500)
        s2.words.flatMap { w -> w.text.map { it.toString() } }.forEach { assertThat(s2.keypad).contains(it) }
    }

    @Test
    fun reachingTarget_finishes_andIgnoresMoreInput() {
        val g = game(target = 5)
        var s = g.start(t0)
        var last: DropInput = DropInput.Ignored
        repeat(5) { i ->
            val word = s.words.minBy { it.spawnedAt }.text
            val (next, r) = g.typeWord(s, word, t0 + i * 10L)
            s = next
            last = r
        }
        assertThat(last).isInstanceOf(DropInput.Finished::class.java)
        assertThat(s.finished).isTrue()
        assertThat(s.correct).isEqualTo(5)
        assertThat(g.type(s, s.keypad.first(), t0).second).isEqualTo(DropInput.Ignored)
    }

    @Test
    fun progress_isFractionOfFall() {
        val w = FallingWord(id = 1, text = "사과", lane = 0, spawnedAt = 0L)
        assertThat(w.progress(5_000L, 10_000L)).isWithin(1e-6f).of(0.5f)
        assertThat(w.progress(20_000L, 10_000L)).isEqualTo(1f)
    }

    @Test
    fun valuesOutOfRange_areClamped() {
        val g = DropWordsGame(level = 9, speed = 9, target = 99, words = bank, random = Random(1))
        val s = g.start(t0)
        assertThat(s.target).isEqualTo(30)
        assertThat(s.fallMillis).isEqualTo(4_000L)
    }

    @Test
    fun testAlarmTarget_belowMinimum_isKept() {
        // 시험 알람 C는 3개예요(결정 67). 게임이 다시 5개로 올리지 않아야 해요.
        assertThat(game(target = 3).start(t0).target).isEqualTo(3)
        assertThat(game(target = 0).start(t0).target).isEqualTo(1)
    }

    @Test
    fun reshuffle_neverDropsSameWordTwiceInARow() {
        // 단어 10개로 30개를 맞히면 두 번 다시 섞어요. 섞인 첫 단어가 바로 전 단어와 같아도 연달아 나오지 않아야 해요(11-19 지시 7).
        repeat(200) { seed ->
            val g = DropWordsGame(level = 1, speed = 1, target = 30, words = bank, random = Random(seed))
            var s = g.start(t0)
            val answered = mutableListOf<String>()
            while (!s.finished) {
                val word = s.words.minBy { it.spawnedAt }.text
                s = g.typeWord(s, word, t0).first
                answered += word
            }
            assertThat(answered.zipWithNext().none { (a, b) -> a == b }).isTrue()
        }
    }
}
