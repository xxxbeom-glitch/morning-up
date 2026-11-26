package com.lumian.morningup.core.alarm.ring

import android.content.Context
import com.lumian.morningup.core.alarm.AlarmDiagnostics
import com.lumian.morningup.core.data.session.RingSessionStore
import com.lumian.morningup.core.model.engine.MissionEscape
import com.lumian.morningup.core.model.engine.RingEvent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 미션 비상구에 보낸 글을 본 결과. */
enum class EscapeAnswer {
    /** 예문과 같아서 껐어요. */
    Correct,

    /** 예문과 달라요. 알림 글이 바뀌고 59초가 다시 차요. */
    Wrong,

    /** 알림이 이미 필요 없어졌어요(미션을 끝냈거나 울림이 끝남). 알림만 지워요. */
    Ignored,
}

/**
 * 미션 비상구 바로 답장을 처리해요(결정 149~151 3절, 4절). 알림 받는 곳과 디버그 adb 입구가 같은 길을 써요.
 * 맞으면 '미션 없이 끄기'와 같은 [RingEvent.MissionEscaped], 틀리면 '미션 조작'([RingEvent.MissionInteraction])을
 * 울림 서비스로 보내요. 기록에는 맞음, 틀림과 글자 수 차이만 남기고 쓴 글은 남기지 않아요.
 */
@Singleton
class MissionEscapeAnswers @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifier: MissionEscapeNotifier,
    private val sessions: RingSessionStore,
    private val controller: RingController,
    private val diagnostics: AlarmDiagnostics,
) {
    /** [fromScreen]이면 M5 화면에서 보낸 답이에요. 틀리면 M5 시간을 다시 걸어요(ISS-051). */
    suspend fun answer(text: CharSequence?, fromScreen: Boolean = false): EscapeAnswer {
        val session = sessions.load()
        val sentence = notifier.currentSentence
        if (session?.escapeShown != true || sentence == null) {
            notifier.clear()
            FlowLog.event(MissionEscapeNotifier.EVENT_SENT, session?.plan?.alarmId, "result=ignored")
            return EscapeAnswer.Ignored
        }
        val alarmId = session.plan.alarmId
        val correct = MissionEscape.matches(text, sentence)
        val lengthDiff = (text?.let { MissionEscape.normalize(it).length } ?: 0) - sentence.length
        FlowLog.event(
            MissionEscapeNotifier.EVENT_SENT,
            alarmId,
            "result=${if (correct) "correct" else "wrong"} length_diff=$lengthDiff",
        )
        return if (correct) {
            notifier.showDone()
            send(RingEvent.MissionEscaped)
            EscapeAnswer.Correct
        } else {
            notifier.showWrong()
            // 알림 창에서 쓰는 동안 미션 화면을 안 만져도 59초가 지나 다시 울리지 않게, 보낼 때마다 '미션 조작'으로 쳐요.
            send(if (fromScreen) RingEvent.EscapeTyped else RingEvent.MissionInteraction)
            EscapeAnswer.Wrong
        }
    }

    /** 사용자가 알림을 밀어서 지웠어요. 아직 쓸 수 있는 때면 같은 예문으로 다시 올려요. */
    suspend fun dismissed() {
        val session = sessions.load()
        if (session?.escapeShown == true) {
            notifier.show(session.plan.alarmId, fresh = false)
        } else {
            notifier.clear()
        }
    }

    /**
     * 울림 서비스로 보내 화면과 알림까지 함께 고쳐요. 알림 버튼에서 온 신호라 앞 서비스를 띄울 수 있어요.
     * 막히면(디버그 adb 입구 등) 울림 한 곳에 바로 넣어요.
     */
    private suspend fun send(event: RingEvent) {
        try {
            RingService.send(context, event)
        } catch (e: IllegalStateException) {
            diagnostics.record("escape_direct", detail = e.javaClass.simpleName)
            controller.send(event)
        }
    }
}
