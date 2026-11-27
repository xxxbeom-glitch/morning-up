package com.lumian.morningup.core.model

import com.lumian.morningup.core.model.mission.QuizCategory
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * 고른 미션 하나의 설정(mission-levels.md 2번 표).
 * - [level]: 난이도 1~7단계. Shake Shake는 단계가 없어 늘 1이에요.
 * - [count]: Pop Quiz, Quick Math는 연속으로 맞혀야 하는 문제 수, Drop Words는 맞힐 단어 수, Shake Shake는 흔드는 횟수.
 *   Drop Words도 화면 이름은 '문제 수'이고 1씩 바뀌어요(11-19 사용자 결정).
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
    /**
     * Pop Quiz만 쓰는 분야(TSK-284, docs/plan-11-quiz-category.md). 처음 값은 전체이고 전체면 저장하지 않아요.
     * 옛 저장값은 전체로 읽혀요. 비어 있으면 [normalized]가 전체로 되돌려요.
     */
    @EncodeDefault(EncodeDefault.Mode.NEVER) val quizCategories: Set<QuizCategory> = ALL_QUIZ_CATEGORIES,
) {
    /** 분야를 모두 고른 상태(처음 값). */
    val allQuizCategories: Boolean get() = quizCategories.containsAll(ALL_QUIZ_CATEGORIES)

    /** 범위 안으로 맞춘 값. */
    fun normalized(): MissionSetting {
        val range = rangeOf(type)
        val counts = if (forTest) 1..range.counts.last else range.counts
        return copy(
            level = level.coerceIn(range.levels),
            count = count.coerceIn(counts),
            speed = if (type == MissionType.DropWords) speed.coerceIn(SPEEDS) else DEFAULT_SPEED,
            quizCategories = if (type == MissionType.PopQuiz && quizCategories.isNotEmpty()) {
                quizCategories
            } else {
                ALL_QUIZ_CATEGORIES
            },
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
        /** 고를 수 있는 미션 수. 편집 화면 미션 칸 4개(결정 87). */
        const val MAX_MISSIONS = 4
        const val DEFAULT_SPEED = 1
        val SPEEDS = 1..4
        val ALL_QUIZ_CATEGORIES: Set<QuizCategory> = QuizCategory.entries.toSet()
        private val LEVELS = 1..7

        /** mission-levels.md 2번 표의 개수 범위. 결정 187로 처음 값이 모두 1이라 최소도 모두 1이에요. */
        private val DROP_WORDS_COUNTS = 1..30
        private val POP_QUIZ_COUNTS = 1..10
        private val QUICK_MATH_COUNTS = 1..10
        private val SHAKE_COUNTS = 1..200
        private val SINGLE_LEVEL = 1..1

        fun rangeOf(type: MissionType): Range = when (type) {
            MissionType.DropWords ->
                Range(LEVELS, DROP_WORDS_COUNTS, countStep = 1, defaultLevel = 1, defaultCount = 1)
            MissionType.PopQuiz -> Range(LEVELS, POP_QUIZ_COUNTS, countStep = 1, defaultLevel = 1, defaultCount = 1)
            MissionType.QuickMath -> Range(LEVELS, QUICK_MATH_COUNTS, countStep = 1, defaultLevel = 1, defaultCount = 1)
            MissionType.ShakeShake ->
                Range(SINGLE_LEVEL, SHAKE_COUNTS, countStep = 1, defaultLevel = 1, defaultCount = 1)
        }

        /**
         * 분야 칩을 누른 뒤의 분야(docs/plan-11-quiz-category.md 3절). [category]가 null이면 '전체' 칩이에요.
         * 다 골라진 상태에서 분야 하나를 누르면 그 하나만 빠져요. 마지막 하나를 빼려 하면 null(막음)이에요.
         */
        fun toggleQuizCategory(current: Set<QuizCategory>, category: QuizCategory?): Set<QuizCategory>? = when {
            category == null -> ALL_QUIZ_CATEGORIES
            category !in current -> current + category
            current.size == 1 -> null
            else -> current - category
        }

        /** 처음 값으로 만든 설정. */
        fun default(type: MissionType) = MissionSetting(type)
    }
}
