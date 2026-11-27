package com.lumian.morningup.feature.mission

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.mission.MathKey
import com.lumian.morningup.feature.mission.play.MissionPlay
import com.lumian.morningup.feature.mission.play.MissionRunner
import com.lumian.morningup.feature.mission.play.MissionUiState
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MissionHostScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val t0 = 1_000_000L

    private fun runner(setting: MissionSetting) = MissionRunner(setting, testContent, Random(SEED), t0)

    private fun show(state: MissionUiState, actions: MissionActions = MissionActions(), onClose: (() -> Unit)? = null) {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                MissionHostScreen(state = state, nowMillis = t0, actions = actions, onClose = onClose)
            }
        }
    }

    private fun stateOf(runner: MissionRunner) = MissionUiState(
        play = runner.play,
        total = 1,
        timerEndsAt = t0 + 42_000,
    )

    @Test
    fun quickMath_showsProblem_andKeysFollowShuffledOrder() {
        val r = runner(MissionSetting(MissionType.QuickMath, level = 1, count = 3))
        val game = (r.play as MissionPlay.Math).game
        val pressed = mutableListOf<MathKey>()
        show(stateOf(r), MissionActions(onMathKey = { pressed += it }))
        compose.onNodeWithText(game.problem.text).assertExists()
        // 결정 89, 92: 가운데 '정답 0 / 3', 위 타임라인 바. '틀리면 새 문제로' 문구는 없어요.
        compose.onNodeWithText("정답 0 / 3").assertExists()
        compose.onNodeWithTag(MISSION_TIMELINE_TAG).assertExists()
        compose.onNodeWithText("틀리면", substring = true).assertDoesNotExist()
        compose.onNodeWithTag("${MATH_KEY_TAG}0").assertContentDescriptionEquals(game.keypad[0].toString())
        compose.onNodeWithTag("${MATH_KEY_TAG}0").performClick()
        // 결정 90: 마지막 줄 '.', 숫자, 지우기. 확인은 입력 칸 안 엔터.
        compose.onNodeWithTag("${MATH_KEY_TAG}9").performClick()
        compose.onNodeWithTag("${MATH_KEY_TAG}11").performClick()
        compose.onNodeWithTag(MATH_INPUT_TAG + ENTER_SUFFIX).performClick()
        assertThat(pressed)
            .containsExactly(MathKey.Digit(game.keypad[0]), MathKey.Dot, MathKey.Delete, MathKey.Confirm)
            .inOrder()
    }

    @Test
    fun popQuiz_showsCategoryQuestionAndFourAnswers_andSendsChoice() {
        val r = runner(MissionSetting(MissionType.PopQuiz, level = 1, count = 3))
        val round = (r.play as MissionPlay.Quiz).game.round
        val chosen = mutableListOf<Int>()
        show(stateOf(r), MissionActions(onQuizAnswer = { chosen += it }))
        compose.onNodeWithText("과학 용어").assertExists()
        compose.onNodeWithText(round.question.question).assertExists()
        round.choices.forEach { compose.onNodeWithText(it).assertExists() }
        compose.onNodeWithTag("${QUIZ_ANSWER_TAG}2").performClick()
        assertThat(chosen).containsExactly(2)
    }

    @Test
    fun popQuiz_correctAnswer_movesToNewQuestion_andStreakGoesUp() {
        val r = runner(MissionSetting(MissionType.PopQuiz, level = 1, count = 3))
        var state by mutableStateOf(stateOf(r))
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                MissionHostScreen(
                    state = state,
                    nowMillis = t0,
                    actions = MissionActions(onQuizAnswer = {
                        r.quizAnswer(it, t0)
                        state = state.copy(play = r.play)
                    }),
                    onClose = null,
                )
            }
        }
        val first = (r.play as MissionPlay.Quiz).game.round
        compose.onNodeWithTag("${QUIZ_ANSWER_TAG}${first.answerIndex}").performClick()
        compose.waitForIdle()
        val next = (r.play as MissionPlay.Quiz).game.round
        compose.onNodeWithText("정답 1 / 3").assertExists()
        compose.onNodeWithText(next.question.question).assertExists()
    }

    @Test
    fun popQuiz_wrongAnswer_showsBigX_andKeepsQuestionUntilHoldEnds() {
        val r = runner(MissionSetting(MissionType.PopQuiz, level = 1, count = 3))
        val first = (r.play as MissionPlay.Quiz).game.round
        r.quizAnswer((first.answerIndex + 1) % first.choices.size, t0)
        show(stateOf(r))
        // 결정 93: 가운데 큰 X와 '틀렸어요', '괜찮아요, 새 문제가 나와요'. 맞힌 수는 그대로예요.
        compose.onNodeWithTag(QUIZ_WRONG_TAG).assertExists()
        compose.onNodeWithText("틀렸어요").assertExists()
        compose.onNodeWithText("괜찮아요, 새 문제가 나와요").assertExists()
        compose.onNodeWithText("정답 0 / 3").assertExists()
        first.choices.forEach { compose.onNodeWithText(it).assertExists() }
    }

    @Test
    fun dropWords_wrongInput_staysVisible_whileHolding_andEnterSubmits() {
        val r = runner(MissionSetting(MissionType.DropWords, level = 1, count = 5))
        var submitted = 0
        val game = (r.play as MissionPlay.Drop).game
        val wrong = game.keypad.first { syllable -> game.words.none { it.text.startsWith(syllable) } }
        r.dropKey(wrong, t0)
        show(stateOf(r), MissionActions(onDropSubmit = { submitted++ }))
        compose.onNode(hasText(wrong) and hasAnyAncestor(hasTestTag(DROP_INPUT_TAG))).assertExists()
        compose.onNodeWithTag(DROP_INPUT_TAG + ENTER_SUFFIX).performClick()
        assertThat(submitted).isEqualTo(1)
    }

    @Test
    fun dropWords_showsWord_counter_andSyllableKeys() {
        val r = runner(MissionSetting(MissionType.DropWords, level = 1, count = 5))
        val game = (r.play as MissionPlay.Drop).game
        val typed = mutableListOf<String>()
        var deleted = 0
        show(stateOf(r), MissionActions(onDropKey = { typed += it }, onDropDelete = { deleted++ }))
        compose.onNodeWithText(game.words.single().text).assertExists()
        compose.onNodeWithText("정답 0 / 5").assertExists()
        compose.onNodeWithTag("${DROP_KEY_TAG}0").assertContentDescriptionEquals(game.keypad[0])
        compose.onNodeWithTag("${DROP_KEY_TAG}18").performClick()
        compose.onNodeWithTag(DROP_DELETE_TAG).performClick()
        assertThat(typed).containsExactly(game.keypad[18])
        assertThat(deleted).isEqualTo(1)
    }

    @Test
    fun shake_showsRemainingCount_withoutTopBar() {
        val r = runner(MissionSetting(MissionType.ShakeShake, count = 18))
        show(stateOf(r))
        compose.onNodeWithContentDescription("18").assertExists()
        compose.onNodeWithText("스마트폰을 흔들어주세요").assertExists()
        compose.onNodeWithText("정답", substring = true).assertDoesNotExist()
    }

    @Test
    fun transition_showsNextMissionNumber() {
        val r = runner(MissionSetting(MissionType.QuickMath))
        show(stateOf(r).copy(transitionTo = 2, total = 3))
        compose.onNodeWithText("다음 미션 2 / 3").assertExists()
    }

    @Test
    fun preview_hasCloseButton_andShowsWellDone() {
        val r = runner(MissionSetting(MissionType.QuickMath))
        var closed = 0
        show(stateOf(r).copy(preview = true, completed = true), onClose = { closed++ })
        compose.onNodeWithText("잘했어요").assertExists()
        compose.onNodeWithTag(MISSION_CLOSE_TAG).performClick()
        assertThat(closed).isEqualTo(1)
    }

    private companion object {
        const val SEED = 7
    }
}
