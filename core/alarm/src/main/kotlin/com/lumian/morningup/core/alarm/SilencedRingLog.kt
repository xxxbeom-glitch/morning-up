package com.lumian.morningup.core.alarm

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 울린 뒤 기록(TSK-177 답 2, R2 '울린 뒤'). 알람이 울릴 때 방해 금지가 소리를 막고 있었으면 남겨 두고,
 * 다음에 앱을 열 때 한 번 알려요([takePending]). 잠금 풀기 전에도 울릴 수 있어 기기 저장소에 둬요.
 */
@Singleton
class SilencedRingLog @Inject constructor(
    @ApplicationContext context: Context,
    private val silence: AlarmSilenceReader,
    private val diagnostics: AlarmDiagnostics,
) {
    private val prefs by lazy {
        context.createDeviceProtectedStorageContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    /** 알람이 울리기 시작할 때 불러요. 막혀 있으면 기록해요. */
    fun onRing(alarmId: Long) {
        if (!silence.silenced()) return
        diagnostics.record(EVENT, alarmId)
        prefs.edit { putBoolean(KEY_PENDING, true) }
    }

    /** 울리기 10분 전 점검(TSK-177 R2). 막혀 있으면 진단 기록만 남겨요. 막혔는지 돌려줘요. */
    fun onPreRing(alarmId: Long?): Boolean {
        val silenced = silence.silenced()
        if (silenced) diagnostics.record(EVENT_PRE, alarmId)
        return silenced
    }

    /** 알릴 것이 남아 있으면 true를 돌려주고 지워요(한 번만 알려요). */
    fun takePending(): Boolean {
        val pending = prefs.getBoolean(KEY_PENDING, false)
        if (pending) prefs.edit { remove(KEY_PENDING) }
        return pending
    }

    private companion object {
        const val PREFS = "silenced_ring"
        const val KEY_PENDING = "pending"
        const val EVENT = "rang_silenced"
        const val EVENT_PRE = "dnd_blocks_alarms_pre"
    }
}
