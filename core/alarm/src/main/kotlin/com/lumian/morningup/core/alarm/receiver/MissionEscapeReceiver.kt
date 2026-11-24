package com.lumian.morningup.core.alarm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.lumian.morningup.core.alarm.AlarmScope
import com.lumian.morningup.core.alarm.ring.MissionEscapeAnswers
import com.lumian.morningup.core.alarm.ring.MissionEscapeNotifier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** 미션 비상구 알림(결정 149)의 바로 답장과, 알림을 밀어서 지운 신호를 받아요. */
@AndroidEntryPoint
class MissionEscapeReceiver : BroadcastReceiver() {
    @Inject lateinit var answers: MissionEscapeAnswers

    @Inject
    @AlarmScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != ACTION_REPLY && action != ACTION_DISMISSED) return
        val text = if (action == ACTION_REPLY) {
            RemoteInput.getResultsFromIntent(intent)?.getCharSequence(MissionEscapeNotifier.KEY_REPLY)
        } else {
            null
        }
        val pending = goAsync()
        scope.launch {
            try {
                if (action == ACTION_REPLY) answers.answer(text) else answers.dismissed()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REPLY = "com.lumian.morningup.alarm.ESCAPE_REPLY"
        const val ACTION_DISMISSED = "com.lumian.morningup.alarm.ESCAPE_DISMISSED"
    }
}
