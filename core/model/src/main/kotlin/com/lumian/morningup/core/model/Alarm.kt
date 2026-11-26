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
        // 1분은 결정 82로 새로 넣었고, 15분은 예전에 저장한 알람이 읽히도록 남겨요.
        val ALLOWED_INTERVALS = setOf(1, 3, 5, 10, 15)
    }
}

/**
 * 다시 잠들기 방지(W2, W3)를 이 알람에 쓸지와 그 값(결정 180, 알람마다). 알람 편집 S4 시트에서 정해요.
 * - [askAfterMinutes]: W2 '다시 물어보기'. 미션을 끝낸 뒤 W2까지예요.
 * - [answerSeconds]: W2 '대답할 시간'.
 * - [recheckEnabled]: '한 번 더 확인'(W3 '정말 깼어요?'). 처음 켜짐.
 * - [recheckWaitMinutes]: W3 '확인까지'. W2 '깼어요' 뒤 W3까지, W3와 다음 W3 사이(결정 176).
 * - [recheckCount]: W3 '묻는 횟수'(결정 176).
 * 처음 값은 결정 145의 전체 설정 처음 값과 같아요. 옛 전체 설정 값은 한 번 모든 알람에 옮겨요(WakeCheckSettingsPrefs).
 */
data class WakeCheck(
    val enabled: Boolean = false,
    val askAfterMinutes: Int = WakeCheckSettings.DEFAULT_ASK_MINUTES,
    val answerSeconds: Int = WakeCheckSettings.DEFAULT_ANSWER_SECONDS,
    val recheckEnabled: Boolean = true,
    val recheckWaitMinutes: Int = WakeCheckSettings.DEFAULT_RECHECK_WAIT_MINUTES,
    val recheckCount: Int = WakeCheckSettings.DEFAULT_RECHECK_COUNT,
) {
    /** 고를 수 없는 값(옛 저장값)은 처음 값으로 바꿔요. */
    fun normalized(): WakeCheck = copy(
        askAfterMinutes = askAfterMinutes.takeIf { it in WakeCheckSettings.ASK_MINUTES }
            ?: WakeCheckSettings.DEFAULT_ASK_MINUTES,
        answerSeconds = answerSeconds.takeIf { it in WakeCheckSettings.ANSWER_SECONDS }
            ?: WakeCheckSettings.DEFAULT_ANSWER_SECONDS,
        recheckWaitMinutes = recheckWaitMinutes.takeIf { it in WakeCheckSettings.RECHECK_WAIT_MINUTES }
            ?: WakeCheckSettings.DEFAULT_RECHECK_WAIT_MINUTES,
        recheckCount = recheckCount.takeIf { it in WakeCheckSettings.RECHECK_COUNTS }
            ?: WakeCheckSettings.DEFAULT_RECHECK_COUNT,
    )

    companion object {
        /** 옛 전체 설정 값을 알람에 옮길 때 써요(결정 180). 켜고 끄기([enabled])는 알람 것 그대로예요. */
        fun valuesOf(settings: WakeCheckSettings, enabled: Boolean) = WakeCheck(
            enabled = enabled,
            askAfterMinutes = settings.askAfterMinutes,
            answerSeconds = settings.answerSeconds,
            recheckEnabled = settings.recheckEnabled,
            recheckWaitMinutes = settings.recheckWaitMinutes,
            recheckCount = settings.recheckCount,
        ).normalized()
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
    /** 새 알람은 랜덤으로 시작해요(11-19 사용자 결정). */
    val soundId: String = RANDOM_SOUND_ID,
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
        /** 앱 기본음 '기본 알람음'. 모르는 id도 이 소리로 울려요. */
        const val DEFAULT_SOUND_ID = "default"

        /** '랜덤': 울릴 때마다 벨소리 중 하나를 골라요(11-19 사용자 결정). 새 알람의 처음 값이에요. */
        const val RANDOM_SOUND_ID = "random"

        /**
         * 진동만(알람음 없음). 11-19부터 S5에서 '사용 안 함'을 빼서 사용자가 고를 수 없어요.
         * 저장돼 있던 알람은 DB v4에서 랜덤으로 바꿨고, 설정 '테스트'의 시험 알람 F만 써요.
         */
        const val SILENT_SOUND_ID = "silent"

        /** 편집 화면 '알람 화면 배경' 토글을 끈 상태. null은 기본 '시간대 하늘'이에요. */
        const val BACKGROUND_PLAIN = "plain"

        const val DEFAULT_GENTLE_SECONDS = 30

        /** S6 볼륨 시트 '볼륨 천천히 올리기' 길이(10초, 15초, 20초, 결정 98). 예전에 고른 다른 값도 울림에는 그대로 써요. */
        val GENTLE_SECONDS = listOf(10, 15, 20)
    }
}
