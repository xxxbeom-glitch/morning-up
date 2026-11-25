package com.lumian.morningup.feature.ring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumian.morningup.core.alarm.MinuteTicker
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ring.RingLink
import com.lumian.morningup.core.alarm.ring.LabLog
import com.lumian.morningup.core.alarm.ring.RingScreen
import com.lumian.morningup.core.model.ButtonTrick
import com.lumian.morningup.core.model.DayPart
import com.lumian.morningup.core.model.WeatherKind
import com.lumian.morningup.core.model.WeatherSource
import com.lumian.morningup.core.model.engine.RingEvent
import com.lumian.morningup.core.model.engine.RingPhase
import com.lumian.morningup.core.model.engine.RingSession
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn

/** 울림 화면(D0, M, W1, W2, W3)이 그릴 것. */
data class RingUiState(
    /** 지금 보여 줄 화면. [RingScreen.None]이면 닫을 때예요. */
    val screen: RingScreen = RingScreen.None,
    val date: LocalDate = LocalDate.MIN,
    val time: LocalTime = LocalTime.MIDNIGHT,
    val dayPart: DayPart = DayPart.Morning,
    /** 미션 없는 알람이면 D0 버튼 글이 '알람 끄기'예요(dev-readiness 4-1). */
    val hasMissions: Boolean = false,
    /** W2, W3 답할 시간이 끝나는 시각(epoch 밀리초). 묻는 중이 아니거나 통화로 멈췄으면 null이에요. */
    val checkEndsAt: Long? = null,
    /** W2를 마치면 이 분 뒤 W3를 물어요. 한 번 더 확인이 꺼져 있으면 null이에요. */
    val nextAskMinutes: Int? = null,
    /** S8 알람 화면 배경. D0에만 써요. */
    val background: String? = null,
    /** W1 날씨 한마디(화면에서 채워요). */
    val weather: WeatherKind? = null,
    /** 울리는 알람 번호(실험실 기록용). */
    val alarmId: Long? = null,
    /** 이번 울림에 걸 버튼 속이기. 실험실에서 껐으면 null이에요(접근성 자동 꺼짐은 화면이 따로 봐요). */
    val trick: ButtonTrick? = null,
    /** 울림 화면이 새로 뜰 때마다 바뀌는 값. 바뀌면 버튼 속이기가 처음부터 다시 걸려요. */
    val trickKey: Long? = null,
    /** 여러 번 묻기에서 지금 묻는(또는 미리 연) 확인이 몇 번째인지. 꺼져 있으면 null이에요. */
    val ask: AskCount? = null,
)

/**
 * D0 알람 울림, W1 해제 완료, W2 깨어 있나요. 상태는 [RingLink]의 session과 screen만 봐요.
 * 미션 화면(M)은 `:feature:mission`이 그리고, 이 화면은 그 자리만 비워 줘요.
 */
@HiltViewModel
class RingViewModel @Inject constructor(
    private val link: RingLink,
    private val now: NowProvider,
    ticker: MinuteTicker,
    weatherSource: WeatherSource,
    private val trickRecorder: TrickRecorder,
    private val trickRuns: TrickRuns,
) : ViewModel() {
    /** W1 날씨 한마디. 위치 동의가 없거나 못 불러왔으면 null이라 짧은 인사만 보여요. */
    val weather: StateFlow<WeatherKind?> = weatherSource.weather

    init {
        weatherSource.refresh()
    }

    private val clock = merge(flowOf(Unit), ticker.ticks()).map { now.now() }

    val uiState: StateFlow<RingUiState> = combine(link.screen, link.session, clock) { screen, session, at ->
        stateOf(screen, session, at)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, stateOf(link.screen.value, link.session.value, now.now()))

    /** 남은 시간 계산에 쓰는 지금 시각. */
    fun nowMillis(): Long = now.millis()

    /** D0 버튼. 미션이 있으면 미션 시작, 없으면 바로 끄기. */
    fun onMainButton() {
        val event = if (uiState.value.hasMissions) RingEvent.MissionStarted else RingEvent.DismissWithoutMission
        link.send(event)
    }

    /** W2 '깼어요'(10초 길게 누르기를 마침). */
    fun onAwake() = link.send(RingEvent.WakeCheckConfirmed)

    /** W3 '깼어요'(10초 길게 누르기를 마침). 기다리는 중에 미리 연 경우도 같아요. */
    fun onRecheckDone() = link.send(RingEvent.RecheckConfirmed)

    /** W1 '확인'. */
    fun onWakeUpClosed() = link.closeWakeUp()

    /**
     * 울림 한 번에 한 번: 버튼 속이기를 지금 걸 수 있는지 봐요. 접근성 때문에 끄면 그 까닭을 진단 기록에 남기고 null을 줘요.
     * 화면에는 꺼진 까닭을 알리지 않아요(결정 146 2-4). 같은 울림에서 화면이 다시 그려지면(화면 껐다 켜기 등)
     * 지난번 [TrickRun]을 그대로 주고 기록도 다시 남기지 않아요(ISS-048).
     */
    fun trickFor(state: RingUiState): TrickRun? {
        // 울리는 단계(D0)에서만 걸어요. 미션, W1, W2로 넘어가면 trickKey가 비면서 다시 불리는데,
        // 이때 trick_shown이 또 찍혀 W2에도 걸린 것처럼 보였어요(기록만 겹침, 화면에는 안 걸림).
        val key = state.trickKey ?: return null
        val trick = state.trick ?: return null
        return trickRuns.get(key) {
            val reason = trickRecorder.offReason()
            if (reason != null) {
                trickRecorder.record(LabLog.TRICK_AUTO_OFF, state.alarmId, "reason=$reason", kept = true)
                null
            } else {
                trickRecorder.record(LabLog.TRICK_SHOWN, state.alarmId, trick.detail())
                TrickRun(trick)
            }
        }
    }

    /** 버튼 속이기 기록(결정 146 4절). trick_end만 출시 빌드 진단 기록에도 남겨요. */
    fun trickListener(state: RingUiState): TrickListener = object : TrickListener {
        override fun onStart() =
            trickRecorder.record(LabLog.TRICK_START, state.alarmId, state.trick?.detail().orEmpty())

        override fun onEnd(seconds: Int, expired: Boolean) = trickRecorder.record(
            LabLog.TRICK_END,
            state.alarmId,
            "seconds=$seconds expired=$expired",
            kept = true,
        )
    }

    private fun ButtonTrick.detail() = "speed=${speed.key}"

    companion object {
        /**
         * 보여 줄 화면. 앱이 다시 살아나 이어가는 경우 쉬는 중에는 Show 효과가 없어서 단계로 골라요.
         * W2를 기다리는 중(W1을 닫은 뒤)에는 보여 줄 것이 없어 닫아요.
         */
        fun shownScreen(screen: RingScreen, session: RingSession?): RingScreen = when {
            screen != RingScreen.None -> screen
            session == null -> RingScreen.None
            session.phase == RingPhase.AwaitingWakeCheck -> RingScreen.None
            // W3를 기다리는 중에는 닫아요. 알림이나 홈 띠를 누르면 컨트롤러가 W3 화면을 정해 줘요.
            session.phase == RingPhase.AwaitingRecheck -> RingScreen.None
            // 통화 때문에 멈춘 W2, W3는 통화가 끝나 다시 물을 때 열어요.
            session.pausedLeftMillis != null -> RingScreen.None
            // 쉬는 중에는 화면을 닫아요. 쉬는 중 알림을 누르면 컨트롤러가 울림 화면을 정해 줘요(결정 70).
            session.phase == RingPhase.Resting -> RingScreen.None
            session.phase == RingPhase.InMission -> RingScreen.Mission(session.missionsDone)
            session.phase == RingPhase.WakeCheckAsking -> RingScreen.WakeCheck
            session.phase == RingPhase.RecheckAsking -> RingScreen.Recheck
            else -> RingScreen.Ringing
        }

        internal fun stateOf(screen: RingScreen, session: RingSession?, at: ZonedDateTime) = RingUiState(
            screen = shownScreen(screen, session),
            date = at.toLocalDate(),
            time = at.toLocalTime().withSecond(0).withNano(0),
            dayPart = DayPart.of(at.toLocalTime()),
            hasMissions = session?.plan?.missions?.isNotEmpty() == true,
            checkEndsAt = session?.takeIf { it.phase.isAsking && it.pausedLeftMillis == null }?.phaseEndsAt,
            nextAskMinutes = session?.plan?.takeIf { it.recheckEnabled }?.wakeCheckAskAfterMinutes,
            background = session?.plan?.background,
            alarmId = session?.plan?.alarmId,
            trick = session?.plan?.trick,
            trickKey = session?.takeIf { it.phase == RingPhase.Ringing }?.phaseEndsAt,
            ask = session?.let { askOf(it) },
        )

        /** 여러 번 묻기에서 지금 묻는 확인 번호(마친 수 + 1). */
        private fun askOf(session: RingSession): AskCount? {
            val total = session.plan.askTotal ?: return null
            return AskCount(number = (session.checksDone + 1).coerceAtMost(total), total = total)
        }
    }
}
