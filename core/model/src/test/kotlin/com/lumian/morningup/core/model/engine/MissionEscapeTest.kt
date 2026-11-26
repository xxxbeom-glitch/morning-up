package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.LabSettings
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.WakeCheckSettings
import com.lumian.morningup.core.model.engine.RingEffect.ClearEscape
import com.lumian.morningup.core.model.engine.RingEffect.EscapeUsed
import com.lumian.morningup.core.model.engine.RingEffect.ScheduleTimer
import com.lumian.morningup.core.model.engine.RingEffect.SessionEnded
import com.lumian.morningup.core.model.engine.RingEffect.ShowEscape
import com.lumian.morningup.core.model.engine.RingEffect.ShowWakeUp
import com.lumian.morningup.core.model.engine.RingEffect.StopSound
import java.text.Normalizer
import java.time.LocalTime
import kotlin.random.Random
import org.junit.Test

/** 미션 비상구(결정 149~151, TSK-182). */
class MissionEscapeTest {
    private val t0 = 1_800_000_000_000L
    private val sec = 1_000L
    private val min = 60 * sec
    private val sentence = "지금은 미션을 할 수 없어서 알람을 끄지만 내일 아침에는 꼭 제시간에 일어날게요."

    private fun plan(
        missions: List<MissionType> = listOf(MissionType.QuickMath, MissionType.PopQuiz),
        wakeCheck: Boolean = true,
        hidden: Boolean = false,
        recheckCount: Int = 1,
    ) = RingPlan(
        alarmId = 3,
        missions = missions,
        intervalMinutes = 5,
        ringAgainCount = 2,
        wakeCheckEnabled = wakeCheck,
        wakeCheckAskAfterMinutes = 3,
        wakeCheckAnswerSeconds = 60,
        recheckEnabled = true,
        recheckCount = recheckCount,
        escapeHidden = hidden,
    )

    private fun reduce(s: RingSession?, e: RingEvent, now: Long) = RingStateMachine.reduce(s, e, now)

    private fun ringing(p: RingPlan = plan()) = reduce(null, RingEvent.AlarmFired(p), t0).session!!

    private fun started(p: RingPlan = plan()) = reduce(ringing(p), RingEvent.MissionStarted, t0 + 10 * sec)

    // 맞는지 보기 (8절 3번) ---------------------------------------------------

    @Test
    fun matches_exactSentence() {
        assertThat(MissionEscape.matches(sentence, sentence)).isTrue()
    }

    @Test
    fun matches_ignoresOnlyLeadingAndTrailingSpacesAndNewlines() {
        assertThat(MissionEscape.matches("  $sentence \n", sentence)).isTrue()
        assertThat(MissionEscape.matches("\n$sentence", sentence)).isTrue()
    }

    @Test
    fun matches_oneLetterTypo_isWrong() {
        assertThat(MissionEscape.matches(sentence.replace("미션을", "미선을"), sentence)).isFalse()
    }

    @Test
    fun matches_oneSpaceMissingOrExtra_isWrong() {
        assertThat(MissionEscape.matches(sentence.replaceFirst("지금은 ", "지금은"), sentence)).isFalse()
        assertThat(MissionEscape.matches(sentence.replaceFirst("지금은 ", "지금은  "), sentence)).isFalse()
    }

    @Test
    fun matches_punctuationDifferent_isWrong() {
        assertThat(MissionEscape.matches(sentence.dropLast(1), sentence)).isFalse()
        assertThat(MissionEscape.matches(sentence.dropLast(1) + "!", sentence)).isFalse()
    }

    @Test
    fun matches_keyboardAddedPeriod_isWrong() {
        assertThat(MissionEscape.matches("$sentence.", sentence)).isFalse()
    }

    @Test
    fun matches_innerNewline_isWrong() {
        assertThat(MissionEscape.matches(sentence.replaceFirst(" ", "\n"), sentence)).isFalse()
    }

    @Test
    fun matches_decomposedHangul_isSameLetters() {
        val decomposed = Normalizer.normalize(sentence, Normalizer.Form.NFD)
        assertThat(decomposed).isNotEqualTo(sentence)
        assertThat(MissionEscape.matches(decomposed, sentence)).isTrue()
    }

    @Test
    fun matches_nullOrEmpty_isWrong() {
        assertThat(MissionEscape.matches(null, sentence)).isFalse()
        assertThat(MissionEscape.matches("   ", sentence)).isFalse()
    }

    // 예문 고르기 (4절) -----------------------------------------------------

    @Test
    fun pick_neverRepeatsPrevious_andCoversOthers() {
        val random = Random(42)
        val seen = mutableSetOf<Int>()
        repeat(2_000) {
            val picked = MissionEscape.pick(20, previous = 7, random = random)
            assertThat(picked).isNotEqualTo(7)
            assertThat(picked in 0 until 20).isTrue()
            seen += picked
        }
        assertThat(seen).hasSize(19)
    }

    @Test
    fun pick_withoutPrevious_canBeAny() {
        val random = Random(1)
        val seen = (1..2_000).map { MissionEscape.pick(20, previous = null, random = random) }.toSet()
        assertThat(seen).hasSize(20)
    }

    // 언제 뜨나 (2절) ----------------------------------------------------------

    @Test
    fun firstMissionStart_showsEscapeOnce() {
        val t = started()
        assertThat(t.effects).contains(ShowEscape())
        assertThat(t.session!!.escapeShown).isTrue()
    }

    @Test
    fun missionTimeoutThenStartAgain_doesNotShowAgain() {
        val s = started().session!!
        val again = reduce(s, RingEvent.TimerFired(TimerKind.MissionTimeout), t0 + 70 * sec)
        assertThat(again.session!!.phase).isEqualTo(RingPhase.Ringing)
        assertThat(again.session!!.escapeShown).isTrue()
        assertThat(again.effects).doesNotContain(ClearEscape)
        val restart = reduce(again.session, RingEvent.MissionStarted, t0 + 80 * sec)
        assertThat(restart.effects.filterIsInstance<ShowEscape>()).isEmpty()
    }

    @Test
    fun secondMission_doesNotShowAgain() {
        val s = started().session!!
        val t = reduce(s, RingEvent.MissionCompleted, t0 + 30 * sec)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.InMission)
        assertThat(t.effects.filterIsInstance<ShowEscape>()).isEmpty()
        assertThat(t.effects).doesNotContain(ClearEscape)
    }

    @Test
    fun noMissionAlarm_neverShows() {
        val t = started(plan(missions = emptyList()))
        assertThat(t.effects.filterIsInstance<ShowEscape>()).isEmpty()
    }

    @Test
    fun allMissionsDone_clearsEscape() {
        var s = started().session!!
        s = reduce(s, RingEvent.MissionCompleted, t0 + 30 * sec).session!!
        val t = reduce(s, RingEvent.MissionCompleted, t0 + 50 * sec)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.AwaitingWakeCheck)
        assertThat(t.session!!.escapeShown).isFalse()
        assertThat(t.effects).contains(ClearEscape)
    }

    @Test
    fun ringEndsUnanswered_clearsEscape() {
        val p = plan().copy(ringAgainCount = 0)
        val s = started(p).session!!
        val ringingAgain = reduce(s, RingEvent.TimerFired(TimerKind.MissionTimeout), t0 + 70 * sec).session!!
        val t = reduce(ringingAgain, RingEvent.TimerFired(TimerKind.RingTimeout), ringingAgain.phaseEndsAt)
        assertThat(t.session).isNull()
        assertThat(t.effects).contains(ClearEscape)
    }

    // 써서 끄기 (3절, 5절) -------------------------------------------------------

    @Test
    fun escaped_inMission_sameAsAllMissionsDone_goesToW1ThenW2() {
        val s = started().session!!
        val t = reduce(s, RingEvent.MissionEscaped, t0 + 40 * sec)
        val after = t.session!!
        assertThat(after.phase).isEqualTo(RingPhase.AwaitingWakeCheck)
        assertThat(after.missionsDone).isEqualTo(2)
        assertThat(after.escapeShown).isFalse()
        assertThat(t.effects).containsAtLeast(EscapeUsed(3), StopSound, ShowWakeUp, ClearEscape).inOrder()
        assertThat(t.effects).contains(ScheduleTimer(t0 + 40 * sec + 3 * min, TimerKind.WakeCheckAsk))
    }

    @Test
    fun escaped_withoutWakeCheck_endsAtW1() {
        val s = started(plan(wakeCheck = false)).session!!
        val t = reduce(s, RingEvent.MissionEscaped, t0 + 40 * sec)
        assertThat(t.session).isNull()
        assertThat(t.effects).containsAtLeast(EscapeUsed(3), ShowWakeUp, SessionEnded(3, RingOutcome.Dismissed))
    }

    @Test
    fun escaped_whileRingingAgainOrResting_stillWorks() {
        val s = started().session!!
        val ringingAgain = reduce(s, RingEvent.TimerFired(TimerKind.MissionTimeout), t0 + 70 * sec).session!!
        assertThat(reduce(ringingAgain, RingEvent.MissionEscaped, t0 + 80 * sec).effects).contains(EscapeUsed(3))
        val resting = reduce(ringingAgain, RingEvent.TimerFired(TimerKind.RingTimeout), ringingAgain.phaseEndsAt)
        assertThat(resting.session!!.phase).isEqualTo(RingPhase.Resting)
        val t = reduce(resting.session, RingEvent.MissionEscaped, ringingAgain.phaseEndsAt + min)
        assertThat(t.effects).contains(EscapeUsed(3))
        assertThat(t.session!!.phase).isEqualTo(RingPhase.AwaitingWakeCheck)
    }

    @Test
    fun escaped_beforeShown_orAfterMissions_isIgnored() {
        val before = ringing()
        assertThat(reduce(before, RingEvent.MissionEscaped, t0).session).isEqualTo(before)
        var s = started().session!!
        s = reduce(s, RingEvent.MissionCompleted, t0 + 30 * sec).session!!
        s = reduce(s, RingEvent.MissionCompleted, t0 + 50 * sec).session!!
        val t = reduce(s, RingEvent.MissionEscaped, t0 + 60 * sec)
        assertThat(t.session).isEqualTo(s)
        assertThat(t.effects).isEmpty()
    }

    @Test
    fun wrongAnswer_isMissionInteraction_refills59Seconds() {
        val s = started().session!!
        val t = reduce(s, RingEvent.MissionInteraction, t0 + 50 * sec)
        assertThat(t.effects).containsExactly(ScheduleTimer(t0 + 50 * sec + 59 * sec, TimerKind.MissionTimeout))
    }

    @Test
    fun escaped_recheckCount_keepsFullCount() {
        val s = started(plan(recheckCount = 3)).session!!
        val t = reduce(s, RingEvent.MissionEscaped, t0 + 40 * sec)
        assertThat(t.session!!.checksDone).isEqualTo(0)
        assertThat(t.session!!.checksLeft).isEqualTo(3)
    }

    // 다시 잠들기 방지로 처음부터 다시 울린 바퀴 (2절) ----------------------------------

    @Test
    fun newRound_showsAgainAtFirstMission() {
        val s = reduce(started().session, RingEvent.MissionEscaped, t0 + 40 * sec).session!!
        val asking = reduce(s, RingEvent.TimerFired(TimerKind.WakeCheckAsk), s.phaseEndsAt).session!!
        val again = reduce(asking, RingEvent.TimerFired(TimerKind.WakeCheckTimeout), asking.phaseEndsAt).session!!
        assertThat(again.rounds).isEqualTo(1)
        assertThat(again.escapeShown).isFalse()
        val t = reduce(again, RingEvent.MissionStarted, again.phaseEndsAt - min)
        assertThat(t.effects).contains(ShowEscape())
    }

    // 숨기기 (5절) ------------------------------------------------------------

    @Test
    fun hidden_doesNotShow_andCannotEscape() {
        val t = started(plan(hidden = true))
        assertThat(t.effects.filterIsInstance<ShowEscape>()).isEmpty()
        assertThat(t.session!!.escapeShown).isFalse()
        assertThat(reduce(t.session, RingEvent.MissionEscaped, t0 + 20 * sec).effects).isEmpty()
    }

    @Test
    fun hidden_lastRound_stillShows() {
        val last = ringing(plan(hidden = true)).copy(rounds = RingStateMachine.MAX_ROUNDS)
        val t = reduce(last, RingEvent.MissionStarted, t0 + 10 * sec)
        assertThat(t.effects).contains(ShowEscape())
    }

    @Test
    fun hidden_planFrom_keepsShowingForShakeWithoutSensor() {
        val shake = Alarm(
            id = 9,
            time = LocalTime.of(6, 30),
            missions = listOf(MissionSetting.default(MissionType.ShakeShake)),
        )
        val math = shake.copy(missions = listOf(MissionSetting.default(MissionType.QuickMath)))
        val settings = WakeCheckSettings(lab = LabSettings(escapeHidden = true))
        assertThat(RingPlan.from(shake, settings, shakeSupported = false).escapeHidden).isFalse()
        assertThat(RingPlan.from(shake, settings, shakeSupported = true).escapeHidden).isTrue()
        assertThat(RingPlan.from(math, settings, shakeSupported = false).escapeHidden).isTrue()
        assertThat(RingPlan.from(math, WakeCheckSettings(), shakeSupported = false).escapeHidden).isFalse()
    }

    // 앱이 다시 살아났을 때 ------------------------------------------------------------

    @Test
    fun resume_repostsSameSentence() {
        val s = started().session!!
        assertThat(RingStateMachine.resume(s)).contains(ShowEscape(fresh = false))
        assertThat(RingStateMachine.resume(ringing()).filterIsInstance<ShowEscape>()).isEmpty()
    }

    // M5 미션 포기 화면 열기 (결정 171) ---------------------------------------

    @Test
    fun escapeOpened_duringMission_refillsMissionTime_withoutShowingMission() {
        val s = started().session!!
        val t = reduce(s, RingEvent.EscapeOpened, t0 + 40 * sec)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.InMission)
        assertThat(t.session!!.phaseEndsAt).isEqualTo(t0 + 40 * sec + RingStateMachine.ESCAPE_HOLD_MILLIS)
        assertThat(t.effects.none { it is RingEffect.ShowMission }).isTrue()
    }

    @Test
    fun escapeOpened_whileRingingAgain_stopsSound_andBecomesMission() {
        val s = started().session!!
        val ringingAgain = reduce(s, RingEvent.TimerFired(TimerKind.MissionTimeout), t0 + 3 * min).session!!
        assertThat(ringingAgain.phase).isEqualTo(RingPhase.Ringing)
        val t = reduce(ringingAgain, RingEvent.EscapeOpened, t0 + 4 * min)
        assertThat(t.session!!.phase).isEqualTo(RingPhase.InMission)
        assertThat(t.session!!.escapeShown).isTrue()
        assertThat(t.effects).contains(StopSound)
        assertThat(t.effects.none { it is RingEffect.ShowMission }).isTrue()
        val endsAt = t0 + 4 * min + RingStateMachine.ESCAPE_HOLD_MILLIS
        assertThat(t.effects).contains(ScheduleTimer(endsAt, TimerKind.MissionTimeout))
    }

    @Test
    fun escapeHold_outlastsEscapeIdle_soNoRingBeforeBackToMission() {
        // ISS-051: M5에서 1분 안 쓰면 미션으로 돌아가요. 그 전에 미션 시간이 끝나 다시 울리면 안 돼요.
        assertThat(RingStateMachine.ESCAPE_HOLD_MILLIS).isGreaterThan(MissionEscape.IDLE_MILLIS)
        val s = started().session!!
        val typed = reduce(s, RingEvent.EscapeTyped, t0 + 40 * sec).session!!
        val idleBack = t0 + 40 * sec + MissionEscape.IDLE_MILLIS
        assertThat(typed.phaseEndsAt - idleBack).isEqualTo(RingStateMachine.MISSION_MILLIS)
    }

    @Test
    fun escapeOpened_whenEscapeClosed_doesNothing() {
        val s = ringing()
        val t = reduce(s, RingEvent.EscapeOpened, t0 + 5 * sec)
        assertThat(t.session).isEqualTo(s)
        assertThat(t.effects).isEmpty()
    }

    @Test
    fun escapeOpened_thenCorrect_endsLikeMissionDone() {
        val s = reduce(started().session!!, RingEvent.EscapeOpened, t0 + 40 * sec).session!!
        val t = reduce(s, RingEvent.MissionEscaped, t0 + 50 * sec)
        assertThat(t.effects).contains(EscapeUsed(3))
        assertThat(t.effects).contains(ShowWakeUp)
    }

    @Test
    fun testSentence_isTheOnlyOneWhileTesting() {
        assertThat(MissionEscape.TEST_SENTENCE).isEqualTo("테스트 문장 12345, @@" + "$" + "$")
        assertThat(MissionEscape.matches("테스트 문장 12345, @@" + "$" + "$", MissionEscape.TEST_SENTENCE)).isTrue()
        assertThat(MissionEscape.matches("테스트 문장12345, @@" + "$" + "$", MissionEscape.TEST_SENTENCE)).isFalse()
    }
}
