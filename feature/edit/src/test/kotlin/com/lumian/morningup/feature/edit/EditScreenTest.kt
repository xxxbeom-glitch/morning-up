package com.lumian.morningup.feature.edit

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.MissionType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EditScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(state: EditUiState, actions: EditActions = EditActions(), onDetail: (EditDetail) -> Unit = {}) {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) {
                EditScreen(state = state, actions = actions, onOpenDetail = onDetail)
            }
        }
    }

    @Test
    fun noMission_showsNone_hidesPreview_dimsPowerOff() {
        var opened: EditDetail? = null
        show(EditUiState(loading = false), onDetail = { opened = it })
        compose.onNodeWithText("없음").assertExists()
        compose.onNodeWithText("미리 해보기").assertDoesNotExist()
        compose.onAllNodes(isToggleable() and isNotEnabled()).assertCountEquals(1)
        compose.onNodeWithText("전원 종료 방지").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        assertThat(opened).isNull()
    }

    @Test
    fun withMission_showsPreview_andDetailRowsDoNothingHarmful() {
        var opened: EditDetail? = null
        val draft = EditViewModel.newAlarm().copy(missions = listOf(MissionType.DropWords))
        show(EditUiState(loading = false, draft = draft), onDetail = { opened = it })
        compose.onNodeWithText("미리 해보기").assertExists()
        compose.onNodeWithText("다시 울림").performSemanticsAction(SemanticsActions.OnClick)
        assertThat(opened).isEqualTo(EditDetail.RingAgain)
    }

    @Test
    fun confirmExit_showsDialog_andLeaveDiscards() {
        var discarded = false
        show(EditUiState(loading = false, confirmExit = true), EditActions(onDiscard = { discarded = true }))
        compose.onNodeWithText("저장하지 않고 나갈까요?").assertExists()
        compose.onNodeWithText("나가기").performClick()
        assertThat(discarded).isTrue()
    }

    @Test
    fun save_callsSave() {
        var saved = false
        show(EditUiState(loading = false), EditActions(onSave = { saved = true }))
        compose.onNodeWithText("저장").performClick()
        assertThat(saved).isTrue()
    }
}
