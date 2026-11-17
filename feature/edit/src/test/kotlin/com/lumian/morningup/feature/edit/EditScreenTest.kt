package com.lumian.morningup.feature.edit

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.component.VALUE_DEC_SUFFIX
import com.lumian.morningup.core.designsystem.component.VALUE_INC_SUFFIX
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.MissionSetting
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
    fun noMission_showsNone_hidesPreview_noPowerOffRow() {
        var opened: EditDetail? = null
        show(EditUiState(loading = false), onDetail = { opened = it })
        // 꺼진 줄은 값이 모두 '사용 안 함'이에요. 전원 종료 방지 줄은 설정 탭으로 옮겨서 없어요(결정 101).
        compose.onAllNodesWithText("사용 안 함").onFirst().assertExists()
        compose.onNodeWithText("미리 해보기").assertDoesNotExist()
        compose.onNodeWithText("전원 종료 방지").assertDoesNotExist()
        compose.onAllNodes(isToggleable() and isNotEnabled()).assertCountEquals(0)
        assertThat(opened).isNull()
    }

    @Test
    fun withMission_showsPreview_andDetailRowsDoNothingHarmful() {
        var opened: EditDetail? = null
        val draft = EditViewModel.newAlarm().copy(missions = listOf(MissionSetting(MissionType.DropWords)))
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
        // 저장 버튼은 내용 맨 끝에 있어서 내려야 보여요.
        compose.onNodeWithText("저장").performScrollTo().performClick()
        assertThat(saved).isTrue()
    }

    @Test
    fun wakeCheckPage_showsOptions_andPicks() {
        var ask = 0
        var answer = 0
        show(
            EditUiState(loading = false, detail = EditDetail.WakeCheck),
            actions = EditActions(onWakeCheckAsk = { ask = it }, onWakeCheckAnswer = { answer = it }),
        )
        compose.onNodeWithText("언제 물을까요").assertExists()
        compose.onNodeWithText("답할 시간").assertExists()
        compose.onNodeWithText("시간 안에 '깼어요'를 누르지 않으면 알람이 다시 울려요").assertExists()
        compose.onNodeWithText("10분 후").performClick()
        compose.onNodeWithText("2분").performScrollTo().performClick()
        assertThat(ask).isEqualTo(10)
        assertThat(answer).isEqualTo(120)
    }

    @Test
    fun previewButton_sendsPickedMissions() {
        var previewed: List<MissionSetting>? = null
        val missions = listOf(MissionSetting(MissionType.QuickMath), MissionSetting(MissionType.ShakeShake))
        val draft = EditViewModel.newAlarm().copy(missions = missions)
        show(EditUiState(loading = false, draft = draft), EditActions(onPreview = { previewed = it }))
        compose.onNodeWithText("미리 해보기").performSemanticsAction(SemanticsActions.OnClick)
        assertThat(previewed).isEqualTo(missions)
    }

    @Test
    fun missionPage_threePicked_disablesOthers_andShowsValues() {
        val picked = listOf(
            MissionSetting(MissionType.QuickMath, level = 2, count = 4),
            MissionSetting(MissionType.DropWords, level = 1, count = 10, speed = 2),
            MissionSetting(MissionType.ShakeShake, count = 25),
        )
        val opened = mutableListOf<MissionType>()
        show(
            EditUiState(
                loading = false,
                detail = EditDetail.Mission,
                draft = EditViewModel.newAlarm().copy(missions = picked),
            ),
            EditActions(onOpenMissionSheet = { opened += it }),
        )
        compose.onNodeWithText("2단계, 4문제").assertExists()
        compose.onNodeWithText("1단계, 속도 2, 단어 10개").assertExists()
        compose.onNodeWithText("25번").assertExists()
        compose.onNodeWithTag("${MISSION_CARD_TAG}QuickMath").assertIsSelected().assertIsEnabled()
        compose.onNodeWithTag("${MISSION_CARD_TAG}PopQuiz").assertIsNotEnabled()
        compose.onNodeWithTag("${MISSION_CARD_TAG}QuickMath").performClick()
        assertThat(opened).containsExactly(MissionType.QuickMath)
    }

    @Test
    fun missionPage_noMission_showsDescriptions_andShakeDisabledWithoutSensor() {
        show(EditUiState(loading = false, detail = EditDetail.Mission, shakeAvailable = false))
        compose.onNodeWithText("떨어지는 단어를 입력해요").assertExists()
        compose.onNodeWithText("상식 퀴즈를 맞혀요").assertExists()
        compose.onNodeWithTag("${MISSION_CARD_TAG}DropWords").assertIsEnabled()
        compose.onNodeWithTag("${MISSION_CARD_TAG}ShakeShake").assertIsNotEnabled()
    }

    @Test
    fun sheet_firstValues_disableLowerEnds_andSelectConfirms() {
        var confirmed = 0
        var level = 0
        val sheet = MissionSheet(MissionSetting.default(MissionType.DropWords), selected = false)
        show(
            EditUiState(loading = false, detail = EditDetail.Mission, missionSheet = sheet),
            EditActions(onConfirmMission = { confirmed++ }, onMissionLevel = { level += it }),
        )
        compose.onNodeWithText("예: 사과, 나무").assertExists()
        compose.onNodeWithText("바닥까지 10초").assertExists()
        compose.onNodeWithTag(SHEET_LEVEL_TAG + VALUE_DEC_SUFFIX, useUnmergedTree = true).assertIsNotEnabled()
        compose.onNodeWithTag(SHEET_SPEED_TAG + VALUE_DEC_SUFFIX, useUnmergedTree = true).assertIsNotEnabled()
        compose.onNodeWithTag(SHEET_COUNT_TAG + VALUE_DEC_SUFFIX, useUnmergedTree = true).assertIsEnabled()
        compose.onNodeWithTag(
            SHEET_LEVEL_TAG + VALUE_INC_SUFFIX,
            useUnmergedTree = true,
        ).assertIsEnabled().performClick()
        compose.onNodeWithText("선택 완료").performClick()
        assertThat(level).isEqualTo(1)
        assertThat(confirmed).isEqualTo(1)
    }

    @Test
    fun sheet_topValues_disableUpperEnds_andSelectedShowsDeselect() {
        var deselected = 0
        var previewed: List<MissionSetting>? = null
        val setting = MissionSetting(MissionType.QuickMath, level = 7, count = 10)
        show(
            EditUiState(
                loading = false,
                detail = EditDetail.Mission,
                draft = EditViewModel.newAlarm().copy(missions = listOf(setting)),
                missionSheet = MissionSheet(setting, selected = true),
            ),
            EditActions(onDeselectMission = { deselected++ }, onPreview = { previewed = it }),
        )
        compose.onNodeWithTag(SHEET_LEVEL_TAG + VALUE_INC_SUFFIX, useUnmergedTree = true).assertIsNotEnabled()
        compose.onNodeWithTag(SHEET_COUNT_TAG + VALUE_INC_SUFFIX, useUnmergedTree = true).assertIsNotEnabled()
        compose.onNodeWithText("선택 완료").assertDoesNotExist()
        compose.onNodeWithText("미리보기").performClick()
        compose.onNodeWithText("선택 해제").performClick()
        assertThat(previewed).containsExactly(setting)
        assertThat(deselected).isEqualTo(1)
    }
}
