package com.lumian.morningup.feature.settings

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.InMemoryWakeCheckSettingsStore
import com.lumian.morningup.core.model.WakeCheckSettings
import com.lumian.morningup.core.testing.FakeWakeCheckGate
import com.lumian.morningup.core.testing.MainDispatcherRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** T1 '깼는지 묻기', '한 번 더 확인' 카드와 시트(결정 145). */
@RunWith(RobolectricTestRunner::class)
class WakeCheckCardTest {
    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val main = MainDispatcherRule()

    private val asked = mutableListOf<Int>()
    private val answered = mutableListOf<Int>()
    private val recheck = mutableListOf<Boolean>()
    private val recheckMinutes = mutableListOf<Int>()

    private fun show(settings: WakeCheckSettings = WakeCheckSettings()) = compose.setContent {
        MorningTheme(darkTheme = false, reduceMotion = true) {
            WakeCheckCardContent(
                settings = settings,
                onAskAfter = { asked += it },
                onAnswer = { answered += it },
                onRecheck = { recheck += it },
                onRecheckMinutes = { recheckMinutes += it },
            )
        }
    }

    @Test
    fun card_showsValues_andNote() {
        show()
        compose.onNodeWithText("깼는지 묻기").assertExists()
        compose.onNodeWithText("3분 후, 1분").assertExists()
        compose.onNodeWithText("한 번 더 확인").assertExists()
        compose.onNodeWithText("10초 동안 눌러야 완전히 꺼져요").assertExists()
        compose.onNodeWithText("알람마다 켜고 끄기는 알람 편집에서 해요").assertExists()
    }

    @Test
    fun askRow_opensSheet_andChipsPick() {
        show(WakeCheckSettings(askAfterMinutes = 5, answerSeconds = 30))
        compose.onNodeWithText("5분 후, 30초").performClick()
        compose.onNodeWithTag(WAKE_CHECK_ASK_SHEET_TAG).assertExists()
        compose.onNodeWithText("알람을 끈 뒤 깼는지 다시 물어요").assertExists()
        compose.onNodeWithText("10분 후").performClick()
        compose.onNodeWithText("3분").performClick()
        assertThat(asked).containsExactly(10)
        assertThat(answered).containsExactly(180)
        compose.onNodeWithTag(WAKE_CHECK_DONE_TAG).performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithTag(WAKE_CHECK_ASK_SHEET_TAG).assertDoesNotExist()
    }

    @Test
    fun recheckSwitch_toggles_andLabelOpensSheet() {
        show()
        compose.onNode(isToggleable()).performClick()
        assertThat(recheck).containsExactly(false)
        compose.onNodeWithText("한 번 더 확인").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithTag(WAKE_CHECK_RECHECK_SHEET_TAG).assertExists()
        compose.onNodeWithText("확인할 시간").assertExists()
        compose.onNodeWithText("5분").performClick()
        assertThat(recheckMinutes).containsExactly(5)
    }

    @Test
    fun viewModel_savesValues_ignoresOthers_andTurningOffStopsW3() {
        val store = InMemoryWakeCheckSettingsStore()
        val gate = FakeWakeCheckGate()
        val vm = WakeCheckSettingsViewModel(store, gate)
        vm.setAskAfter(10)
        vm.setAskAfter(7)
        vm.setAnswer(30)
        vm.setRecheckMinutes(3)
        vm.setRecheck(false)
        val expected =
            WakeCheckSettings(askAfterMinutes = 10, answerSeconds = 30, recheckEnabled = false, recheckMinutes = 3)
        assertThat(vm.settings.value).isEqualTo(expected)
        assertThat(gate.calls).containsExactly("recheck_off")
        vm.setRecheck(true)
        assertThat(gate.calls).containsExactly("recheck_off")
    }
}
