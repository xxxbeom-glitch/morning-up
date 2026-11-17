package com.lumian.morningup.core.alarm

import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** 분이 바뀔 때마다 한 번씩 알려요. 홈 상단 '몇 시간 몇 분 후'를 맞추는 데 써요. 테스트는 가짜를 넣어요. */
fun interface MinuteTicker {
    fun ticks(): Flow<Unit>
}

/** 시스템 시계 기준. 다음 분 정각까지 기다렸다가 알려요. */
class SystemMinuteTicker @Inject constructor(private val now: NowProvider) : MinuteTicker {
    override fun ticks(): Flow<Unit> = flow {
        while (true) {
            val millis = now.millis()
            delay(MINUTE_MILLIS - millis % MINUTE_MILLIS + SLACK_MILLIS)
            emit(Unit)
        }
    }

    private companion object {
        const val MINUTE_MILLIS = 60_000L

        /** 정각 바로 앞에서 깨면 같은 분을 다시 계산하므로 조금 늦게 깨요. */
        const val SLACK_MILLIS = 50L
    }
}
