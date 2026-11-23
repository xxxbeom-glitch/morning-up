package com.lumian.morningup.core.alarm.ring

import com.lumian.morningup.core.model.engine.RingEffect
import com.lumian.morningup.core.model.engine.RingEvent
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import timber.log.Timber

/**
 * 알람 흐름 로그(TSK-164). 울림, 미션, '깼어요', 끄기 같은 사건을 logcat 태그 [TAG] 하나에 한 줄씩 남겨요.
 * `adb logcat -s MorningFlow`로 이것만 뽑아요(docs/qa/README.md). 로그는 디버그 빌드에만 심은 Timber로 나가서
 * 출시 빌드에는 남지 않아요.
 */
object FlowLog {
    const val TAG = "MorningFlow"
    private val clock = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    fun event(name: String, alarmId: Long? = null, detail: String? = null) {
        val line = buildString {
            append("at=").append(LocalTime.now().format(clock))
            append(" alarm=").append(alarmId ?: "-")
            append(" event=").append(name)
            if (!detail.isNullOrBlank()) append(' ').append(detail)
        }
        Timber.tag(TAG).i(line)
    }

    /** 상태표에 들어간 사건 이름. 화면 버튼 사건은 Intent에 쓰는 고정 이름을 그대로 써요. */
    fun nameOf(event: RingEvent): String = when (event) {
        is RingEvent.AlarmFired -> "fired"
        is RingEvent.TimerFired -> "timer_${event.kind.name.lowercase()}"
        else -> RingEvent.keyOf(event) ?: event.javaClass.simpleName
    }

    /** 효과 목록을 짧게. 예: `effects=StartSound,ShowRingScreen,SetTimer`. */
    fun effectsOf(effects: List<RingEffect>): String =
        "effects=" + effects.joinToString(",") { it.javaClass.simpleName }
}
