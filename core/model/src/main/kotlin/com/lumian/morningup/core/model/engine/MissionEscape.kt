package com.lumian.morningup.core.model.engine

import java.text.Normalizer
import kotlin.random.Random

/**
 * 미션 비상구(결정 149~151). 울림 미션을 못 할 때 알림의 바로 답장 칸에 예문을 따라 써서 끄는 길이에요.
 * 버튼 글은 '미션 포기'(가칭)이고, 문서와 코드 이름은 `MissionEscape`예요.
 */
object MissionEscape {
    /** 맞게 보낸 뒤 '알람을 껐어요'를 보여 주는 시간(3절). */
    const val DONE_MILLIS = 3_000L

    /**
     * 보낸 글이 예문과 완전히 같은지(4절, 11-24 사용자 정정 '오타 절대 안됨 공백마저도').
     * 맨 앞과 맨 끝의 빈칸, 줄 바꿈만 지우고, 한글 저장 방식 차이(`NFC`)만 맞춘 뒤 한 글자도 달라서는 안 돼요.
     * 가운데 띄어쓰기, 문장 부호, 키보드가 붙인 마침표는 모두 그대로 비교해요.
     */
    fun matches(input: CharSequence?, sentence: String): Boolean {
        if (input == null) return false
        return normalize(input) == normalize(sentence)
    }

    /** 맞는지 볼 때와 같은 다듬기. 기록에 글자 수 차이만 남길 때도 써요(쓴 글 자체는 남기지 않아요). */
    fun normalize(text: CharSequence): String = Normalizer.normalize(text.toString().trim(), Normalizer.Form.NFC)

    /** 예문 [count]개 중 하나를 무작위로 골라요. 바로 전에 쓴 [previous]는 빼요(4절). */
    fun pick(count: Int, previous: Int?, random: Random = Random.Default): Int {
        require(count > 0) { "예문이 없어요" }
        if (count == 1) return 0
        val skip = previous?.takeIf { it in 0 until count } ?: return random.nextInt(count)
        val picked = random.nextInt(count - 1)
        return if (picked >= skip) picked + 1 else picked
    }

    /**
     * 이번 바퀴에서 알림을 띄울지(2절, 5절). 숨기기를 켰어도 마지막 바퀴의 첫 미션에는 띄워요.
     * 움직임 센서가 없는 폰의 흔들기 예외는 울리기 시작할 때 [RingPlan.escapeHidden]에 이미 반영돼요.
     */
    fun shouldShow(session: RingSession): Boolean =
        session.plan.missions.isNotEmpty() &&
            (!session.plan.escapeHidden || session.rounds >= RingStateMachine.MAX_ROUNDS)
}
