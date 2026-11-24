package com.lumian.morningup.feature.ring

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.MinuteTicker
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ring.RingScreen
import com.lumian.morningup.core.model.DayPart
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.WeatherKind
import com.lumian.morningup.core.model.WeatherSource
import com.lumian.morningup.core.model.engine.RingEvent
import com.lumian.morningup.core.model.engine.RingPhase
import com.lumian.morningup.core.testing.FakeRingLink
import com.lumian.morningup.core.testing.MainDispatcherRule
import com.lumian.morningup.core.testing.testRingSession
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class RingViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private var clock = ZonedDateTime.of(2026, 11, 17, 6, 30, 12, 0, ZoneId.of("Asia/Seoul"))
    private val now = NowProvider { clock }
    private val ticks = MutableSharedFlow<Unit>()
    private val ticker = MinuteTicker { ticks }
    private val math = MissionSetting(MissionType.QuickMath)

    private fun viewModel(link: FakeRingLink) = RingViewModel(link, now, ticker, NoWeather)

    @Test
    fun ringing_showsDateTimeAndMorning() = runTest {
        val vm = viewModel(FakeRingLink(testRingSession(missions = listOf(math)), RingScreen.Ringing))
        val state = vm.uiState.value
        assertThat(state.screen).isEqualTo(RingScreen.Ringing)
        assertThat(state.date).isEqualTo(LocalDate.of(2026, 11, 17))
        assertThat(state.time).isEqualTo(LocalTime.of(6, 30))
        assertThat(state.dayPart).isEqualTo(DayPart.Morning)
        assertThat(state.hasMissions).isTrue()
    }

    @Test
    fun minuteTick_updatesTime_andNight() = runTest {
        val vm = viewModel(FakeRingLink(testRingSession(), RingScreen.Ringing))
        clock = clock.withHour(22).withMinute(5)
        ticks.emit(Unit)
        assertThat(vm.uiState.value.time).isEqualTo(LocalTime.of(22, 5))
        assertThat(vm.uiState.value.dayPart).isEqualTo(DayPart.Night)
    }

    @Test
    fun mainButton_withMissions_startsMission() = runTest {
        val link = FakeRingLink(testRingSession(missions = listOf(math)), RingScreen.Ringing)
        viewModel(link).onMainButton()
        assertThat(link.sent).containsExactly(RingEvent.MissionStarted)
    }

    @Test
    fun mainButton_withoutMissions_dismisses() = runTest {
        val link = FakeRingLink(testRingSession(), RingScreen.Ringing)
        val vm = viewModel(link)
        assertThat(vm.uiState.value.hasMissions).isFalse()
        vm.onMainButton()
        assertThat(link.sent).containsExactly(RingEvent.DismissWithoutMission)
    }

    @Test
    fun wakeUp_close_clearsScreen() = runTest {
        val link = FakeRingLink(testRingSession(RingPhase.AwaitingWakeCheck), RingScreen.WakeUp)
        val vm = viewModel(link)
        assertThat(vm.uiState.value.screen).isEqualTo(RingScreen.WakeUp)
        vm.onWakeUpClosed()
        assertThat(link.wakeUpClosed).isEqualTo(1)
        assertThat(vm.uiState.value.screen).isEqualTo(RingScreen.None)
    }

    @Test
    fun wakeCheck_carriesDeadline_andAwakeSendsConfirm() = runTest {
        val session = testRingSession(
            RingPhase.WakeCheckAsking,
            phaseEndsAt = 99_000L,
            wakeCheckAskAfterMinutes = 3,
            recheckEnabled = true,
        )
        val link = FakeRingLink(session, RingScreen.WakeCheck)
        val vm = viewModel(link)
        assertThat(vm.uiState.value.checkEndsAt).isEqualTo(99_000L)
        // 한 번 더 확인이 켜져 있으면 W2 뒤 '3분 뒤 한 번 더 물어볼게요'.
        assertThat(vm.uiState.value.nextAskMinutes).isEqualTo(3)
        vm.onAwake()
        assertThat(link.sent).containsExactly(RingEvent.WakeCheckConfirmed)
    }

    @Test
    fun recheck_withoutRecheck_hasNoNextAsk_andDoneSendsRecheckConfirmed() = runTest {
        val session = testRingSession(RingPhase.RecheckAsking, phaseEndsAt = 50_000L)
        val link = FakeRingLink(session, RingScreen.Recheck)
        val vm = viewModel(link)
        assertThat(vm.uiState.value.nextAskMinutes).isNull()
        vm.onRecheckDone()
        assertThat(link.sent).containsExactly(RingEvent.RecheckConfirmed)
    }

    @Test
    fun pausedForCall_hidesDeadline() = runTest {
        val session = testRingSession(RingPhase.WakeCheckAsking, phaseEndsAt = 99_000L, pausedLeftMillis = 30_000L)
        val vm = viewModel(FakeRingLink(session, RingScreen.WakeCheck))
        assertThat(vm.uiState.value.checkEndsAt).isNull()
    }

    @Test
    fun shownScreen_followsPhase_whenNoShowEffect() {
        fun shown(
            phase: RingPhase,
        ) = RingViewModel.shownScreen(RingScreen.None, testRingSession(phase, missionsDone = 1))
        // 쉬는 중에는 화면을 닫아요(결정 70). 쉬는 중 알림을 누르면 컨트롤러가 Ringing을 정해 줘요.
        assertThat(shown(RingPhase.Resting)).isEqualTo(RingScreen.None)
        assertThat(shown(RingPhase.InMission)).isEqualTo(RingScreen.Mission(1))
        assertThat(shown(RingPhase.WakeCheckAsking)).isEqualTo(RingScreen.WakeCheck)
        assertThat(shown(RingPhase.AwaitingWakeCheck)).isEqualTo(RingScreen.None)
        assertThat(shown(RingPhase.AwaitingRecheck)).isEqualTo(RingScreen.None)
        assertThat(shown(RingPhase.RecheckAsking)).isEqualTo(RingScreen.Recheck)
        assertThat(RingViewModel.shownScreen(RingScreen.None, null)).isEqualTo(RingScreen.None)
    }
}

/** 날씨 없는 시험용 출처. */
private object NoWeather : WeatherSource {
    override val weather: StateFlow<WeatherKind?> = MutableStateFlow(null)

    override fun refresh(force: Boolean) = Unit
}
