package com.lumian.morningup.core.model.engine

import java.time.Duration
import java.time.ZonedDateTime

/**
 * 울리기 10분 전 방해 금지 점검(TSK-177 답 2, R2). 갤럭시 모드나 루틴은 알람 시각 가까이에 켜질 수 있어서
 * 앱을 열 때만 봐서는 못 잡아요. 다음 알람 [LEAD] 전에 한 번 깨어 방해 금지를 봐요.
 */
object PreRingCheckPolicy {
    val LEAD: Duration = Duration.ofMinutes(10)

    /**
     * 다음 알람을 찾기 시작할 시각. 지금부터 [LEAD]가 안 남은 알람은 미리 볼 수 없어서 건너뛰고 그다음 알람을 봐요.
     * 점검이 막 끝난 때 부르면 방금 본 알람을 다시 고르지 않아요.
     */
    fun searchFrom(now: ZonedDateTime): ZonedDateTime = now.plus(LEAD).plusSeconds(1)

    /** [nextAlarmAt]을 점검할 시각. 알람이 없으면 null. */
    fun checkAt(nextAlarmAt: ZonedDateTime?): ZonedDateTime? = nextAlarmAt?.minus(LEAD)
}
