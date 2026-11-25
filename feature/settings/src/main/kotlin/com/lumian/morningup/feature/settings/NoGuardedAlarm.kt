package com.lumian.morningup.feature.settings

import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.AlarmBook
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val STOP_TIMEOUT_MILLIS = 5_000L

/** 켜진 알람 가운데 '다시 잠들기 방지'를 켠 알람이 하나도 없으면 참이에요(결정 164). 알람이 없어도 참이에요. */
internal fun noGuardedAlarm(alarms: List<Alarm>): Boolean = alarms.none { it.enabled && it.wakeCheck.enabled }

/** T1, T3 안내용. 처음 값은 거짓이라 목록을 읽기 전에 안내가 번쩍 뜨지 않아요. */
internal fun AlarmBook.noGuardedAlarm(scope: CoroutineScope): StateFlow<Boolean> =
    alarms.map(::noGuardedAlarm).stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), false)
