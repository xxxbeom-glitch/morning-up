package com.lumian.morningup.feature.edit

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
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
import com.lumian.morningup.core.model.mission.QuizCategory
import com.lumian.morningup.core.model.RingAgain
import com.lumian.morningup.core.model.WakeCheck
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
        // 새 알람은 다시 울림이 켜져 있어서(결정 163) 꺼진 줄을 보려고 꺼 둬요.
        val draft = EditViewModel.newAlarm().copy(ringAgain = RingAgain(count = 0))
        show(EditUiState(loading = false, draft = draft), onDetail = { opened = it })
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
        compose.onNodeWithText("해제하지 않고 두면 저절로 다시 울려요").assertExists()
    }

    @Test
    fun ringAgainSheet_offersChoices_andWakeCheckRowOpensSheet() {
        var interval: Int? = null
        var count: Int? = -1
        var ask: Int? = null
        val draft = EditViewModel.newAlarm()
        show(
            EditUiState(loading = false, draft = draft),
            EditActions(
                onRingAgainInterval = { interval = it },
                onRingAgainCount = { count = it },
                onWakeCheckAsk = { ask = it },
            ),
        )
        compose.onNodeWithText("다시 울림").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("1분").performClick()
        compose.onNodeWithText("무제한").performClick()
        assertThat(interval).isEqualTo(1)
        assertThat(count).isNull()
        compose.onNodeWithText("완료").performClick()
        compose.mainClock.advanceTimeBy(1_000)
        // 결정 180: 다시 잠들기 방지 줄 글자를 누르면 S4 시트가 열려요(시안 TSK-226).
        compose.onNodeWithText("다시 잠들기 방지").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("알람 해제 후 재수면 여부를 재확인해요").assertExists()
        compose.onNodeWithText("10분 후").performClick()
        assertThat(ask).isEqualTo(10)
    }

    @Test
    fun wakeCheckSheet_twoRows_countUsesPlusMinus() {
        val steps = mutableListOf<Int>()
        val draft = EditViewModel.newAlarm().copy(wakeCheck = WakeCheck(enabled = true))
        show(
            EditUiState(loading = false, draft = draft, labRecheck = true),
            EditActions(onRecheckStep = { steps += it }),
        )
        // 실험실에서 켜면 줄 값에 횟수도 보여요.
        compose.onNodeWithText("3분 후, 횟수 1번").assertExists()
        compose.onNodeWithText("다시 잠들기 방지").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        // 결정 186: 줄은 '간격'(1, 3, 5, 10분)과 '횟수'(+, -) 둘. '안 함' 칸과 3번 끝은 없어요.
        compose.onNodeWithText("간격").assertExists()
        compose.onNodeWithText("1분 후").assertExists()
        compose.onNodeWithText("횟수").assertExists()
        compose.onNodeWithText("한 번 더 확인").assertDoesNotExist()
        compose.onNodeWithText("안 함").assertDoesNotExist()
        compose.onNodeWithText("대답할 시간").assertDoesNotExist()
        compose.onNodeWithText("확인까지").assertDoesNotExist()
        // 1번이 최소라 - 는 막혀 있고 + 는 눌려요.
        compose.onNodeWithTag(RECHECK_COUNT_TAG + VALUE_DEC_SUFFIX, useUnmergedTree = true).assertIsNotEnabled()
        compose.onNodeWithTag(RECHECK_COUNT_TAG + VALUE_INC_SUFFIX, useUnmergedTree = true)
            .assertIsEnabled()
            .performSemanticsAction(SemanticsActions.OnClick)
        assertThat(steps).containsExactly(1)
    }

    @Test
    @Config(qualifiers = "w360dp-h748dp")
    fun wakeCheckSheet_narrowScreen_intervalChipsAllShown() {
        // ISS-058: Z플립5 너비에서도 간격 칩 4개가 다 보이고 '횟수' 줄 이름은 한 줄이에요.
        val draft = EditViewModel.newAlarm().copy(wakeCheck = WakeCheck(enabled = true))
        show(EditUiState(loading = false, draft = draft, labRecheck = true), EditActions())
        compose.onNodeWithText("다시 잠들기 방지").performScrollTo().performSemanticsAction(SemanticsActions.OnClick)
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("횟수").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertThat(layouts.single().lineCount).isEqualTo(1)
        listOf("1분 후", "3분 후", "5분 후", "10분 후").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }

    @Test
    fun wakeCheckSheet_labRecheckOff_onlyIntervalRow_andValueIsIntervalOnly() {
        // 결정 184: 실험실 '한 번 더 확인'이 꺼져 있으면 시트는 '간격' 한 줄, 줄 값은 '3분 후'처럼 간격만이에요.
        val draft = EditViewModel.newAlarm().copy(wakeCheck = WakeCheck(enabled = true, recheckCount = 2))
        show(EditUiState(loading = false, draft = draft, labRecheck = false), EditActions())
        compose.onNodeWithText("3분 후").performScrollTo().assertExists()
        compose.onNodeWithText("다시 잠들기 방지").performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithText("간격").assertExists()
        compose.onNodeWithText("횟수").assertDoesNotExist()
        compose.onNodeWithText("2번").assertDoesNotExist()
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
        // 시트 제목 '볼륨'이 줄 이름과 함께 두 곳에 보여요(설명 글은 TSK-285로 뺌).
        assertThat(compose.onAllNodesWithText("볼륨").fetchSemanticsNodes().size).isEqualTo(2)
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
        compose.onNodeWithText("미션 추가").assertExists()
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
        compose.onNodeWithText("1단계, 속도 2, 10문제").assertExists()
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
        compose.onNodeWithText("비처럼 쏟아지는 단어를 캐치해요").assertExists()
        compose.onNodeWithText("다양한 분야의 랜덤 퀴즈").assertExists()
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
        compose.onNodeWithTag(SHEET_COUNT_TAG + VALUE_DEC_SUFFIX, useUnmergedTree = true).assertIsNotEnabled()
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

    /** TSK-284: Pop Quiz 시트 맨 아래 '분야' 줄. 마지막 하나는 빠지지 않고 토스트를 띄워요. */
    @Test
    fun popQuizSheet_categoryRow_countAndLastOneShowsNotice() {
        val picked = mutableListOf<QuizCategory?>()
        val setting = MissionSetting(MissionType.PopQuiz, quizCategories = setOf(QuizCategory.Science))
        show(
            EditUiState(
                loading = false,
                detail = EditDetail.Mission,
                missionSheet = MissionSheet(setting, selected = false),
            ),
            EditActions(onQuizCategory = { picked += it }),
        )
        compose.onNodeWithTag(SHEET_CATEGORY_TAG, useUnmergedTree = true).assertExists()
        compose.onNodeWithText("분야", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("1개 골랐어요", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("과학 용어").performClick()
        assertThat(picked).isEmpty()
        compose.onNodeWithText("분야를 하나 이상 골라 주세요", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("국사").performClick()
        compose.onNodeWithText("전체").performClick()
        assertThat(picked).containsExactly(QuizCategory.KoreanHistory, null).inOrder()
    }

    @Test
    fun quickMathSheet_hasNoCategoryRow() {
        val sheet = MissionSheet(MissionSetting.default(MissionType.QuickMath), selected = false)
        show(EditUiState(loading = false, detail = EditDetail.Mission, missionSheet = sheet))
        compose.onNodeWithTag(SHEET_CATEGORY_TAG, useUnmergedTree = true).assertDoesNotExist()
    }
}
