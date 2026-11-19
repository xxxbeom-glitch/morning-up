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
import org.robolectric.annotation.Config

// 상단바가 64+8로 커져서 기본 화면(470dp)에서는 미션 목록 끝이 잘려요. 실제 폰 높이로 돌려요.
@Config(qualifiers = "w411dp-h891dp")
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
    fun noMission_showsFourEmptySlots_noPreview_noPowerOffRow() {
        var opened: EditDetail? = null
        show(EditUiState(loading = false), onDetail = { opened = it })
        // 꺼진 줄은 값이 모두 '사용 안 함'이에요. 전원 종료 방지 줄은 설정 탭으로 옮겨서 없어요(결정 101).
        compose.onAllNodesWithText("사용 안 함").onFirst().assertExists()
        // 결정 87, 95: '미션 선택' 줄과 '미리 해보기' 대신 빈 칸 4개.
        (0 until 4).forEach { compose.onNodeWithTag("${MISSION_SLOT_TAG}Empty$it").assertExists() }
        compose.onNodeWithText("미션 선택").assertDoesNotExist()
        compose.onNodeWithText("미리 해보기").assertDoesNotExist()
        compose.onNodeWithText("전원 종료 방지").assertDoesNotExist()
        compose.onAllNodes(isToggleable() and isNotEnabled()).assertCountEquals(0)
        assertThat(opened).isNull()
    }

    @Test
    fun withMission_fillsSlot_andRingAgainOpensSheet() {
        var opened: EditDetail? = null
        val draft = EditViewModel.newAlarm().copy(missions = listOf(MissionSetting(MissionType.DropWords)))
        show(EditUiState(loading = false, draft = draft), onDetail = { opened = it })
        compose.onNodeWithTag("${MISSION_SLOT_TAG}DropWords").assertExists()
        (1 until 4).forEach { compose.onNodeWithTag("${MISSION_SLOT_TAG}Empty$it").assertExists() }
        compose.onNodeWithText("미리 해보기").assertDoesNotExist()
        // 다시 울림은 페이지 대신 S3 시트가 열려요(결정 78).
        compose.onNodeWithText("다시 울림").performSemanticsAction(SemanticsActions.OnClick)
        assertThat(opened).isNull()
        compose.onNodeWithText("미션을 못 끝내면 다시 울려요").assertExists()
    }

    @Test
    fun ringAgainAndWakeCheckSheets_offerNewChoices() {
        var interval: Int? = null
        var count: Int? = -1
        var ask: Int? = null
        var answer: Int? = null
        val draft = EditViewModel.newAlarm()
        show(
            EditUiState(loading = false, draft = draft),
            EditActions(
                onRingAgainInterval = { interval = it },
                onRingAgainCount = { count = it },
                onWakeCheckAsk = { ask = it },
                onWakeCheckAnswer = { answer = it },
            ),
        )
        compose.onNodeWithText("다시 울림").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("1분").performClick()
        compose.onNodeWithText("무제한").performClick()
        assertThat(interval).isEqualTo(1)
        assertThat(count).isNull()
        compose.onNodeWithText("완료").performClick()
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("다시 잠들기 방지").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("10분 후").performClick()
        compose.onNodeWithText("3분").performClick()
        assertThat(ask).isEqualTo(10)
        assertThat(answer).isEqualTo(180)
    }

    @Test
    fun soundCard_twoRows_volumeOpensSheet_andChipsSetGentle() {
        var gentle: Int? = null
        var gentleOff = false
        val draft = EditViewModel.newAlarm().copy(volumePercent = 80, vibrate = true, gentleWake = false)
        show(
            EditUiState(loading = false, draft = draft),
            EditActions(onGentleSeconds = { gentle = it }, onGentle = { gentleOff = !it }),
        )
        // 결정 97, 98: 카드 이름 '알람 사운드', 줄은 알람음과 볼륨 두 개, 배경 줄 없음.
        compose.onNodeWithText("알람 사운드").assertExists()
        compose.onNodeWithText("알람 화면 배경").assertDoesNotExist()
        compose.onNodeWithText("80%, 진동").performScrollTo().performClick()
        compose.onNodeWithText("소리 크기와 울리는 방식을 정해요").assertExists()
        compose.onNodeWithText("10초").performClick()
        assertThat(gentle).isEqualTo(10)
        compose.onNodeWithText("안 함").performClick()
        assertThat(gentleOff).isTrue()
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
        compose.onNodeWithText("3분").performScrollTo().performClick()
        assertThat(ask).isEqualTo(10)
        assertThat(answer).isEqualTo(180)
    }

    @Test
    fun slots_openSheet_removeBadgeRemoves_andEmptyOpensPickSheet() {
        val opened = mutableListOf<MissionType>()
        val removed = mutableListOf<MissionType>()
        val missions = listOf(MissionSetting(MissionType.QuickMath), MissionSetting(MissionType.ShakeShake))
        val draft = EditViewModel.newAlarm().copy(missions = missions)
        show(
            EditUiState(loading = false, draft = draft),
            EditActions(onOpenMissionSheet = { opened += it }, onRemoveMission = { removed += it }),
        )
        compose.onNodeWithTag("${MISSION_SLOT_TAG}QuickMath").performClick()
        compose.onNodeWithTag("${MISSION_REMOVE_TAG}ShakeShake").performClick()
        assertThat(opened).containsExactly(MissionType.QuickMath)
        assertThat(removed).containsExactly(MissionType.ShakeShake)
        // 빈 칸 → S1-1 미션 추가 시트. 이미 넣은 미션 줄은 누를 수 없고, 다른 줄은 시트를 닫고 설정 시트를 열어요.
        compose.onNodeWithTag("${MISSION_SLOT_TAG}Empty2").performClick()
        compose.onNodeWithText("알람을 끌 때 풀 미션을 하나 골라요").assertExists()
        compose.onNodeWithTag("${MISSION_PICK_ROW_TAG}QuickMath").assertIsNotEnabled()
        compose.onNodeWithTag("${MISSION_PICK_ROW_TAG}PopQuiz").assertIsEnabled().performClick()
        assertThat(opened).containsExactly(MissionType.QuickMath, MissionType.PopQuiz).inOrder()
    }

    @Test
    fun missionSheet_showsOnEditScreen_withPreviewInside() {
        var previewed: List<MissionSetting>? = null
        val setting = MissionSetting(MissionType.QuickMath)
        show(
            EditUiState(
                loading = false,
                draft = EditViewModel.newAlarm().copy(missions = listOf(setting)),
                missionSheet = MissionSheet(setting, selected = true),
            ),
            EditActions(onPreview = { previewed = it }),
        )
        // 미리보기는 미션 설정 시트 안에 남아요(결정 88).
        compose.onNodeWithText("미리보기").performClick()
        assertThat(previewed).containsExactly(setting)
    }

    @Test
    fun missionPage_threePicked_showsValues_andFourthStillOpen() {
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
        // 칸이 4개라(결정 87) 셋을 골라도 남은 하나를 더 고를 수 있어요.
        compose.onNodeWithTag("${MISSION_CARD_TAG}PopQuiz").assertIsEnabled()
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
        // 11-18: 줄 아래 설명 글은 빼고, 머리 아래 예제 영역(스켈레톤 그림)으로 보여 줘요.
        compose.onNodeWithTag(SHEET_EXAMPLE_TAG, useUnmergedTree = true).assertExists()
        compose.onNodeWithText("예: 사과, 나무").assertDoesNotExist()
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
