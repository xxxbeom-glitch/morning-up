package com.lumian.morningup.core.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * 고른 미션 하나의 설정(mission-levels.md 2번 표).
 * - [level]: 난이도 1~7단계. Shake Shake는 단계가 없어 늘 1이에요.
 * - [count]: Pop Quiz, Quick Math는 연속으로 맞혀야 하는 문제 수, Drop Words는 맞힐 단어 수, Shake Shake는 흔드는 횟수.
 * - [speed]: Drop Words만 쓰는 속도 1~4. 나머지는 늘 1이에요.
 * 범위를 벗어난 값은 만들 때 가장 가까운 끝 값으로 맞춰요(저장된 옛 값이 범위 밖이어도 앱이 죽지 않게).
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MissionSetting(
    val type: MissionType,
    val level: Int = rangeOf(type).defaultLevel,
    val count: Int = rangeOf(type).defaultCount,
    val speed: Int = DEFAULT_SPEED,
    /**
     * 개발용 시험 알람만 true예요. 개수를 범위 아래(최소 1)까지 줄일 수 있어요(11-17 사용자 지시: Drop Words 3개).
     * 사용자 화면에서는 늘 false이고, false면 저장할 때 적지 않아요.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val forTest: Boolean = false,
) {
    /** 범위 안으로 맞춘 값. */
    fun normalized(): MissionSetting {
        val range = rangeOf(type)
        val counts = if (forTest) 1..range.counts.last else range.counts
        return copy(
            level = level.coerceIn(range.levels),
            count = count.coerceIn(counts),
            speed = if (type == MissionType.DropWords) speed.coerceIn(SPEEDS) else DEFAULT_SPEED,
        )
    }

    /** 미션마다 고를 수 있는 범위와 처음 값. */
    data class Range(
        val levels: IntRange,
        val counts: IntRange,
        val countStep: Int,
        val defaultLevel: Int,
        val defaultCount: Int,
    )

    companion object {
        /** 고를 수 있는 미션 수(F5). */
        const val MAX_MISSIONS = 3
        const val DEFAULT_SPEED = 1
        val SPEEDS = 1..4
        private val LEVELS = 1..7

        /** mission-levels.md 2번 표의 개수 범위. */
        private val DROP_WORDS_COUNTS = 5..30
        private val POP_QUIZ_COUNTS = 2..10
        private val QUICK_MATH_COUNTS = 1..10
        private val SHAKE_COUNTS = 10..200
        private val SINGLE_LEVEL = 1..1

        fun rangeOf(type: MissionType): Range = when (type) {
            MissionType.DropWords ->
                Range(LEVELS, DROP_WORDS_COUNTS, countStep = 5, defaultLevel = 1, defaultCount = 10)
            MissionType.PopQuiz -> Range(LEVELS, POP_QUIZ_COUNTS, countStep = 1, defaultLevel = 1, defaultCount = 3)
            MissionType.QuickMath -> Range(LEVELS, QUICK_MATH_COUNTS, countStep = 1, defaultLevel = 1, defaultCount = 3)
            MissionType.ShakeShake ->
                Range(SINGLE_LEVEL, SHAKE_COUNTS, countStep = 1, defaultLevel = 1, defaultCount = 10)
        }

        /** 처음 값으로 만든 설정. */
        fun default(type: MissionType) = MissionSetting(type)
    }
}
