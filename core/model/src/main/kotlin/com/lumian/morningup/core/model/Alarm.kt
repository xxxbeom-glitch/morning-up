package com.lumian.morningup.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** 미션 종류. 4~5단계에서 화면이 붙어요. */
enum class MissionType { QuickMath, PopQuiz, DropWords, ShakeShake }

/**
 * 아무도 안 끌 때 다시 울리기. 처음 울림 뒤 [count]번 더 울려요. [count]가 null이면 끌 때까지.
 * 간격은 3, 5, 10, 15분 중 하나예요.
 */
data class RingAgain(val intervalMinutes: Int = 5, val count: Int? = 3) {
    init {
        require(intervalMinutes in ALLOWED_INTERVALS) { "간격은 $ALLOWED_INTERVALS 중 하나예요: $intervalMinutes" }
        require(count == null || count >= 0) { "횟수는 0 이상이에요: $count" }
    }

    companion object {
        val ALLOWED_INTERVALS = setOf(3, 5, 10, 15)
    }
}

/**
 * 다시 잠들기 방지(W2). 해제하고 [askAfterMinutes]분 뒤 묻고, [answerSeconds]초 안에 '깼어요'를 눌러야 해요.
 * 처음 값은 기획서 F9(1분 후, 답할 시간 1분)예요.
 */
data class WakeCheck(val enabled: Boolean = false, val askAfterMinutes: Int = 1, val answerSeconds: Int = 60) {
    init {
        require(askAfterMinutes in 1..MAX_ASK_MINUTES)
        require(answerSeconds in MIN_ANSWER_SECONDS..MAX_ANSWER_SECONDS)
    }

    companion object {
        const val MAX_ASK_MINUTES = 10
        const val MIN_ANSWER_SECONDS = 30
        const val MAX_ANSWER_SECONDS = 120
    }
}

/**
 * 알람 하나. [repeatDays]가 비어 있으면 한 번 알람이에요.
 * [skipHolidays], [skipOnceDate]는 반복 알람에만 적용해요.
 */
data class Alarm(
    val id: Long = 0,
    val time: LocalTime,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val skipHolidays: Boolean = false,
    val enabled: Boolean = true,
    /** 고른 순서대로의 미션과 설정. 비어 있으면 미션 없는 알람(4-1)이에요. */
    val missions: List<MissionSetting> = emptyList(),
    val ringAgain: RingAgain = RingAgain(),
    val wakeCheck: WakeCheck = WakeCheck(),
    val powerOffGuard: Boolean = false,
    val soundId: String = DEFAULT_SOUND_ID,
    val volumePercent: Int = 100,
    val vibrate: Boolean = true,
    val vibrationPattern: VibrationPattern = VibrationPattern.Basic,
    val gentleWake: Boolean = false,
    /** S7 부드럽게 깨우기: 이 초 동안 작은 소리에서 정한 볼륨까지 올려요. */
    val gentleSeconds: Int = DEFAULT_GENTLE_SECONDS,
    val background: String? = null,
    /** '이번만 쉬기'로 건너뛸 날짜. 그날 한 번만 쉬어요. */
    val skipOnceDate: LocalDate? = null,
    /** 마지막으로 예약한 울릴 시각(epoch 밀리초). 꺼져 있던 동안 놓친 알람을 찾는 데 써요. */
    val nextTriggerAt: Long? = null,
) {
    val isRepeating: Boolean get() = repeatDays.isNotEmpty()

    val missionTypes: List<MissionType> get() = missions.map { it.type }

    companion object {
        const val DEFAULT_SOUND_ID = "default"

        /** 편집 화면 '알람음' 토글을 끈 상태(진동만). 울림 쪽은 알람음 화면(7단계)과 같이 따라요. */
        const val SILENT_SOUND_ID = "silent"

        /** 편집 화면 '알람 화면 배경' 토글을 끈 상태. null은 기본 '시간대 하늘'이에요. */
        const val BACKGROUND_PLAIN = "plain"

        const val DEFAULT_GENTLE_SECONDS = 30

        /** S7에서 고를 수 있는 길이(15초, 30초, 1분, 2분). */
        val GENTLE_SECONDS = listOf(15, 30, 60, 120)
    }
}
