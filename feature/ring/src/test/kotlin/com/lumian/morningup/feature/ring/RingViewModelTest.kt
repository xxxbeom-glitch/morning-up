package com.lumian.morningup.feature.ring

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.MinuteTicker
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.alarm.ring.LabLog
import com.lumian.morningup.core.alarm.ring.RingScreen
import com.lumian.morningup.core.alarm.ring.TrickAccessibility
import com.lumian.morningup.core.model.ButtonTrick
import com.lumian.morningup.core.model.DayPart
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.TrickSpeed
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
    private val recorder = FakeTrickRecorder()
    private val trickRuns = TrickRuns()

    private fun viewModel(link: FakeRingLink) = RingViewModel(link, now, ticker, NoWeather, recorder, trickRuns)

    @Test
    fun release_showsW4WithoutCountdown_andReportsDone() = runTest {
        // TSK-266: W2 기다림 중 W4. 남은 시간 숫자가 없고, 10초를 마치면 해제하고 홈으로 알려요.
        val waiting = testRingSession(missions = listOf(math)).copy(phase = RingPhase.AwaitingWakeCheck)
        val link = FakeRingLink(waiting, RingScreen.Release)
        val vm = viewModel(link)
        assertThat(vm.uiState.value.screen).isEqualTo(RingScreen.Release)
        assertThat(vm.uiState.value.checkEndsAt).isNull()
        var done = 0
        vm.onRelease { done++ }
        assertThat(link.released).isEqualTo(1)
        assertThat(done).isEqualTo(1)
        vm.onReleaseClosed()
        assertThat(link.screen.value).isEqualTo(RingScreen.None)
    }

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
    fun snooze_buttonOnlyWhileRingingWithCountsLeft_andSendsEvent() = runTest {
        // 결정 195: 미루기를 켰고 횟수가 남았을 때만 'n분 뒤 다시'. 누르면 미루기 사건을 보내요.
        val base = testRingSession(missions = listOf(math))
        val ringing = base.copy(plan = base.plan.copy(snoozeMinutes = 3, snoozeCount = 2), snoozesLeft = 2)
        val link = FakeRingLink(ringing, RingScreen.Ringing)
        val vm = viewModel(link)
        assertThat(vm.uiState.value.snoozeMinutes).isEqualTo(3)
        vm.onSnooze()
        assertThat(link.sent).containsExactly(RingEvent.Snoozed)
        link.session.value = ringing.copy(snoozesLeft = 0)
        assertThat(vm.uiState.value.snoozeMinutes).isNull()
        link.session.value = ringing.copy(phase = RingPhase.Resting, snoozed = true)
        assertThat(vm.uiState.value.snoozeMinutes).isNull()
        assertThat(viewModel(FakeRingLink(base, RingScreen.Ringing)).uiState.value.snoozeMinutes).isNull()
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

    private fun trickSession(phaseEndsAt: Long = 5_000L) = testRingSession(phaseEndsAt = phaseEndsAt, alarmId = 4).let {
        it.copy(plan = it.plan.copy(trick = ButtonTrick(TrickSpeed.Fast)))
    }

    @Test
    fun trick_isPassedWhileRinging_withKeyFromThisRing() = runTest {
        val vm = viewModel(FakeRingLink(trickSession(), RingScreen.Ringing))
        val state = vm.uiState.value
        assertThat(state.trick).isEqualTo(ButtonTrick(TrickSpeed.Fast))
        assertThat(state.trickKey).isEqualTo(5_000L)
        assertThat(state.alarmId).isEqualTo(4)
        assertThat(vm.trickFor(state)).isNotNull()
        assertThat(recorder.events.single().event).isEqualTo(LabLog.TRICK_SHOWN)
    }

    @Test
    fun trick_sameRing_keepsRun_andRecordsShownOnce() = runTest {
        // ISS-048: 울림 중 화면을 껐다 켜면 화면이 다시 그려지고 뷰모델도 새로 생길 수 있어요.
        // 같은 울림이면 같은 TrickRun(처음 만진 때, 자리)을 받고 trick_shown은 한 번만 찍혀요.
        val link = FakeRingLink(trickSession(), RingScreen.Ringing)
        val first = viewModel(link).trickFor(viewModel(link).uiState.value)
        val again = viewModel(link).trickFor(viewModel(link).uiState.value)
        assertThat(again).isSameInstanceAs(first)
        assertThat(recorder.events.map { it.event }).containsExactly(LabLog.TRICK_SHOWN)
        // 다시 울려 trickKey가 바뀌면 새로 걸고 다시 기록해요.
        val next = viewModel(FakeRingLink(trickSession(phaseEndsAt = 9_000L), RingScreen.Ringing))
        assertThat(next.trickFor(next.uiState.value)).isNotSameInstanceAs(first)
        assertThat(recorder.events.map { it.event }).containsExactly(LabLog.TRICK_SHOWN, LabLog.TRICK_SHOWN)
    }

    @Test
    fun trick_autoOff_sameRing_recordsOnce() = runTest {
        recorder.reason = TrickAccessibility.TALKBACK
        val link = FakeRingLink(trickSession(), RingScreen.Ringing)
        repeat(2) { assertThat(viewModel(link).trickFor(viewModel(link).uiState.value)).isNull() }
        assertThat(recorder.events.map { it.event }).containsExactly(LabLog.TRICK_AUTO_OFF)
    }

    @Test
    fun trick_afterRinging_isNotShownAgain_andNotRecorded() = runTest {
        val vm = viewModel(FakeRingLink(trickSession(), RingScreen.Ringing))
        // 미션, W1, W2로 넘어가면 trickKey가 비어요. 이때 다시 불려도 걸지 않고 기록도 남기지 않아요.
        val later = vm.uiState.value.copy(trickKey = null)
        assertThat(vm.trickFor(later)).isNull()
        assertThat(recorder.events).isEmpty()
    }

    @Test
    fun trick_autoOff_forAccessibility_recordsReasonKept() = runTest {
        recorder.reason = TrickAccessibility.TALKBACK
        val vm = viewModel(FakeRingLink(trickSession(), RingScreen.Ringing))
        assertThat(vm.trickFor(vm.uiState.value)).isNull()
        assertThat(recorder.events.single())
            .isEqualTo(FakeTrickRecorder.Event(LabLog.TRICK_AUTO_OFF, 4, "reason=talkback", kept = true))
    }

    @Test
    fun trickEnd_isKept_othersDebugOnly() = runTest {
        val vm = viewModel(FakeRingLink(trickSession(), RingScreen.Ringing))
        val listener = vm.trickListener(vm.uiState.value)
        listener.onStart()
        listener.onEnd(seconds = 7)
        assertThat(recorder.events.map { it.event to it.kept }).containsExactly(
            LabLog.TRICK_START to false,
            LabLog.TRICK_END to true,
        ).inOrder()
        assertThat(recorder.events.last().detail).isEqualTo("seconds=7")
    }

    @Test
    fun ask_countsFromChecksDone_onlyWhenRecheckCountOverOne() = runTest {
        val base = testRingSession(RingPhase.RecheckAsking, recheckEnabled = true)
        assertThat(viewModel(FakeRingLink(base, RingScreen.Recheck)).uiState.value.ask).isNull()
        val multi = testRingSession(RingPhase.RecheckAsking, recheckEnabled = true, recheckCount = 3, checksDone = 1)
        assertThat(viewModel(FakeRingLink(multi, RingScreen.Recheck)).uiState.value.ask).isEqualTo(AskCount(2, 3))
        val last = multi.copy(checksDone = 2)
        assertThat(viewModel(FakeRingLink(last, RingScreen.Recheck)).uiState.value.ask!!.isLast).isTrue()
        // W2를 묻는 동안은 W3 번호를 보이지 않아요.
        val w2 = testRingSession(RingPhase.WakeCheckAsking, recheckEnabled = true, recheckCount = 3)
        assertThat(viewModel(FakeRingLink(w2, RingScreen.WakeCheck)).uiState.value.ask).isNull()
    }

    @Test
    fun w2Done_nextAskMinutes_isRecheckWait() = runTest {
        val s = testRingSession(
            RingPhase.WakeCheckAsking,
            recheckEnabled = true,
            wakeCheckAskAfterMinutes = 10,
            recheckWaitMinutes = 1,
        )
        assertThat(viewModel(FakeRingLink(s, RingScreen.WakeCheck)).uiState.value.nextAskMinutes).isEqualTo(1)
    }
}

/** 날씨 없는 시험용 출처. */
private object NoWeather : WeatherSource {
    override val weather: StateFlow<WeatherKind?> = MutableStateFlow(null)

    override fun refresh(force: Boolean) = Unit
}
