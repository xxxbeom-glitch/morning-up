package com.lumian.morningup.core.alarm

import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Qualifier

/**
 * 지금 시각. 매번 시스템 시간대를 새로 읽어요.
 * (Clock.systemDefaultZone은 만든 때의 시간대를 붙잡아서, 여행 중 시간대가 바뀌면 틀려요.)
 */
fun interface NowProvider {
    fun now(): ZonedDateTime

    fun millis(): Long = now().toInstant().toEpochMilli()

    companion object {
        val System = NowProvider { ZonedDateTime.now(ZoneId.systemDefault()) }
    }
}

/** 알람 모듈이 화면과 상관없이 오래 돌리는 일에 쓰는 범위. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AlarmScope
