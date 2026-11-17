package com.lumian.morningup.core.model.mission

import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import kotlin.random.Random

/** mission-levels.md Drop Words 속도표. 속도 1~4: 칸 위에서 바닥까지 10, 8, 6, 4초, 한 번에 떠 있는 단어 1, 2, 2, 3개. */
object DropWordsSpeed {
    private val FALL_MILLIS = listOf(10_000L, 8_000L, 6_000L, 4_000L)
    private val MAX_WORDS = listOf(1, 2, 2, 3)

    fun fallMillis(speed: Int): Long = FALL_MILLIS[speed.coerceIn(MissionSetting.SPEEDS) - 1]

    fun maxWords(speed: Int): Int = MAX_WORDS[speed.coerceIn(MissionSetting.SPEEDS) - 1]

    /** 떨어지는 칸을 세로로 나눈 자리 수(단어가 겹치지 않게). */
    const val LANES = 3
}

/** 떨어지고 있는 단어 하나. [lane]은 가로 자리(0 왼쪽, 1 가운데, 2 오른쪽)예요. */
data class FallingWord(val id: Int, val text: String, val lane: Int, val spawnedAt: Long) {
    /** 0이면 칸 위끝, 1이면 바닥에 닿음. */
    fun progress(now: Long, fallMillis: Long): Float = ((now - spawnedAt).toFloat() / fallMillis).coerceIn(0f, 1f)
}

data class DropWordsState(
    val level: Int,
    val target: Int,
    /** 지금까지 맞힌 단어 수(바닥에 닿은 단어로 줄지 않아요). */
    val correct: Int,
    val words: List<FallingWord>,
    /** 친 음절들(확인 키 없이 단어가 다 맞으면 바로 정답). */
    val input: String,
    /** 19칸 음절. 화면 왼쪽 위부터 차례로, 오른쪽 아래 지우기 칸은 빼요. */
    val keypad: List<String>,
    val fallMillis: Long,
    val finished: Boolean = false,
    val lastSpawnAt: Long,
    val nextId: Int,
)

/** 음절 하나를 친 결과. */
sealed interface DropInput {
    /** 어떤 단어의 앞부분과 맞아서 이어 치는 중. */
    data object Typing : DropInput

    data class Correct(val word: String) : DropInput

    data class Finished(val word: String) : DropInput

    /** 떠 있는 어떤 단어의 앞부분과도 안 맞음. 입력칸이 흔들리고 비워져요(벌 없음). */
    data object Wrong : DropInput

    /** 이미 끝났거나 키패드에 없는 음절. */
    data object Ignored : DropInput
}

/**
 * Drop Words 규칙(mission-levels.md, design-system M1).
 * - 단어는 위에서 내려오고 바닥에 닿으면 벌 없이 사라져요. 빈 자리가 생기면 차례로 새 단어가 내려와요.
 * - 확인 키가 없어요. 친 글자가 떠 있는 어떤 단어의 앞부분과도 안 맞는 순간 비워요. 단어를 다 치면 바로 정답이에요.
 * - 정답마다 키패드를 새로 만들어요(그 순간 떠 있는 단어 음절 + 가짜 음절). 치는 도중에는 키패드를 바꾸지 않고,
 *   입력이 비어 있을 때 새 단어 음절이 빠져 있으면 그 음절만 제자리에 끼워 넣어요.
 * - 맞힌 단어가 [target]개가 되면 끝나요.
 */
class DropWordsGame(
    level: Int,
    speed: Int,
    target: Int,
    words: List<String>,
    private val random: Random,
) {
    private val range = MissionSetting.rangeOf(MissionType.DropWords)
    private val level = level.coerceIn(range.levels)
    private val target = target.coerceIn(range.counts)
    private val fallMillis = DropWordsSpeed.fallMillis(speed)
    private val maxWords = DropWordsSpeed.maxWords(speed)
    private val bank = words.filter(Hangul::isHangulWord).distinct().ifEmpty { error("단어가 없어요") }
    private var deck = ArrayDeque<String>()

    fun start(now: Long): DropWordsState {
        val empty = DropWordsState(
            level = level,
            target = target,
            correct = 0,
            words = emptyList(),
            input = "",
            keypad = emptyList(),
            fallMillis = fallMillis,
            lastSpawnAt = now,
            nextId = 1,
        )
        val first = spawn(empty, now)
        return first.copy(keypad = SyllableKeypad.build(needed(first), random))
    }

    /** 시간이 흘렀어요. 바닥에 닿은 단어를 지우고, 빈 자리에 새 단어를 내리고, 입력이 비어 있으면 빠진 음절을 넣어요. */
    fun tick(state: DropWordsState, now: Long): DropWordsState {
        if (state.finished) return state
        var s = state.copy(words = state.words.filter { it.progress(now, fallMillis) < 1f })
        if (s.input.isNotEmpty() && s.words.none { it.text.startsWith(s.input) }) s = s.copy(input = "")
        val gap = fallMillis / maxWords
        if (s.words.isEmpty() || (s.words.size < maxWords && now - s.lastSpawnAt >= gap)) s = spawn(s, now)
        if (s.input.isEmpty()) s = s.copy(keypad = SyllableKeypad.fill(s.keypad, needed(s), random))
        return s
    }

    fun type(state: DropWordsState, syllable: String, now: Long): Pair<DropWordsState, DropInput> {
        if (state.finished || syllable !in state.keypad) return state to DropInput.Ignored
        val candidate = state.input + syllable
        val hit = state.words.filter { it.text == candidate }.minByOrNull { it.spawnedAt }
        return when {
            hit != null -> answer(state, hit, now)
            state.words.any { it.text.startsWith(candidate) } -> state.copy(input = candidate) to DropInput.Typing
            else -> state.copy(input = "") to DropInput.Wrong
        }
    }

    /** 단어 하나를 맞혔을 때. 다 맞히면 끝, 아니면 새 키패드. */
    private fun answer(state: DropWordsState, hit: FallingWord, now: Long): Pair<DropWordsState, DropInput> {
        val correct = state.correct + 1
        val left = state.copy(correct = correct, input = "", words = state.words - hit)
        if (correct >= state.target) return left.copy(finished = true) to DropInput.Finished(hit.text)
        val next = tick(left, now)
        return next.copy(keypad = SyllableKeypad.build(needed(next), random)) to DropInput.Correct(hit.text)
    }

    fun delete(state: DropWordsState): DropWordsState =
        if (state.finished) state else state.copy(input = state.input.dropLast(1))

    /** 키패드에 꼭 있어야 하는 음절. 먼저 떨어질(먼저 내려온) 단어부터. */
    private fun needed(state: DropWordsState): List<String> =
        state.words.sortedBy { it.spawnedAt }.flatMap { w -> w.text.map(Char::toString) }.distinct()

    private fun spawn(state: DropWordsState, now: Long): DropWordsState {
        val onScreen = state.words.map { it.text }.toSet()
        val text = drawWord(onScreen)
        val usedLanes = state.words.map { it.lane }.toSet()
        val lane = (0 until DropWordsSpeed.LANES).filter {
            it !in usedLanes
        }.ifEmpty { (0 until DropWordsSpeed.LANES).toList() }
            .random(random)
        val word = FallingWord(id = state.nextId, text = text, lane = lane, spawnedAt = now)
        return state.copy(words = state.words + word, lastSpawnAt = now, nextId = state.nextId + 1)
    }

    /** 섞어 둔 단어를 차례로 꺼내요. 다 쓰면 다시 섞고, 화면에 떠 있는 단어는 건너뛰어요. */
    private fun drawWord(onScreen: Set<String>): String {
        repeat(bank.size * 2) {
            if (deck.isEmpty()) deck = ArrayDeque(bank.shuffled(random))
            val next = deck.removeFirst()
            if (next !in onScreen) return next
        }
        return bank.random(random)
    }
}
