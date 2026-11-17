package com.lumian.morningup.core.model.engine

import java.time.Duration
import java.time.ZonedDateTime

/**
 * '몇 일 몇 시간 몇 분 후'. 홈 상단 문구, 편집 안내 글, 저장 토스트가 같이 써요.
 * 분은 올려서 세요. 울리기 직전에도 '0분 후'가 나오지 않고, 23:10:01에 06:30 알람은 '7시간 20분 후'예요.
 */
data class TimeUntil(val days: Int, val hours: Int, val minutes: Int) {
    companion object {
        private const val SECONDS_PER_MINUTE = 60L
        private const val MINUTES_PER_HOUR = 60L
        private const val HOURS_PER_DAY = 24L

        fun between(now: ZonedDateTime, at: ZonedDateTime): TimeUntil {
            val nanos = Duration.between(now, at).toNanos()
            if (nanos <= 0) return TimeUntil(0, 0, 0)
            val minuteNanos = Duration.ofSeconds(SECONDS_PER_MINUTE).toNanos()
            val total = (nanos + minuteNanos - 1) / minuteNanos
            val hours = total / MINUTES_PER_HOUR
            return TimeUntil(
                days = (hours / HOURS_PER_DAY).toInt(),
                hours = (hours % HOURS_PER_DAY).toInt(),
                minutes = (total % MINUTES_PER_HOUR).toInt(),
            )
        }
    }
}
