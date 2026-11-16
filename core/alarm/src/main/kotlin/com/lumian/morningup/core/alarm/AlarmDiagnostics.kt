package com.lumian.morningup.core.alarm

import com.lumian.morningup.core.data.diagnostics.DiagnosticLog
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/** 진단 기록을 suspend가 아닌 곳(예약, 신호 받는 곳)에서도 남길 수 있게 해요. */
@Singleton
class AlarmDiagnostics @Inject constructor(
    private val log: DiagnosticLog,
    private val now: NowProvider,
    @AlarmScope private val scope: CoroutineScope,
) {
    /** 지금 시각. 효과 실행기가 기록과 같은 시계를 쓰게 해요. */
    fun now() = now.now()

    fun record(event: String, alarmId: Long? = null, detail: String = "") {
        Timber.i("alarm %s id=%s %s", event, alarmId, detail)
        scope.launch { log.record(now.millis(), event, alarmId, detail) }
    }
}
