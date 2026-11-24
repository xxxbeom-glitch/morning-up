package com.lumian.morningup.feature.ring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumian.morningup.core.alarm.MinuteTicker
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ring.RingLink
import com.lumian.morningup.core.alarm.ring.RingScreen
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
        )
    }
}
