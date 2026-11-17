package com.lumian.morningup.core.model

import java.time.LocalTime

/**
 * 울림 화면의 아침과 밤(design-system '알람 울림 (D)': 아침 하늘과 밤 p8 다크 모드).
 * 시각으로 나눠요: 4:00~17:59는 아침, 18:00~3:59는 밤. 새벽 4~5시 알람은 일어나는 시각이라 아침 하늘로 봐요.
 */
enum class DayPart {
    Morning,
    Night,
    ;

    companion object {
        private val MORNING_START: LocalTime = LocalTime.of(4, 0)
        private val NIGHT_START: LocalTime = LocalTime.of(18, 0)

        fun of(time: LocalTime): DayPart =
            if (time >= MORNING_START && time < NIGHT_START) Morning else Night
    }
}
