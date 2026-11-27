package com.lumian.morningup.feature.edit

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.MissionType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(qualifiers = "w411dp-h891dp")
@RunWith(RobolectricTestRunner::class)
class FirstRunAlarmScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(state: FirstRunUiState, actions: FirstRunActions = FirstRunActions()) {
        compose.setContent {
            MorningTheme(darkTheme = false, reduceMotion = true) { FirstRunAlarmScreen(state, actions) }
        }
    }

    @Test
    fun timeStep_showsPlanCopy_noBack_andNextAndSkipWork() {
        var next = false
        var skipped = false
        show(FirstRunUiState(), FirstRunActions(onNext = { next = true }, onSkip = { skipped = true }))
        compose.onNodeWithText("몇 시에 깨워 드릴까요?").assertExists()
        compose.onNodeWithText("매일 울려요. 요일은 나중에 바꿀 수 있어요").assertExists()
        compose.onNodeWithContentDescription("1 / 4 단계").assertExists()
        compose.onNodeWithContentDescription("뒤로").assertDoesNotExist()
        compose.onNodeWithText("다음").performClick()
        compose.onNodeWithTag(FIRST_RUN_SKIP_TAG).performClick()
        assertThat(next).isTrue()
        assertThat(skipped).isTrue()
    }

    @Test
    fun backgroundStep_hasPreview_andNoMyPhotoGroup() {
        var back = false
        show(FirstRunUiState(step = FirstRunStep.Background), FirstRunActions(onBack = { back = true }))
        compose.onNodeWithText("알람 화면 배경을 골라 주세요").assertExists()
        compose.onNodeWithText("미션 시작").assertExists()
        compose.onNodeWithText("내 사진").assertDoesNotExist()
        compose.onNodeWithContentDescription("뒤로").performClick()
        assertThat(back).isTrue()
    }

    @Test
    fun soundStep_listsRingtones_andPicks() {
        var picked: String? = null
        show(FirstRunUiState(step = FirstRunStep.Sound), FirstRunActions(onSound = { picked = it }))
        compose.onNodeWithText("어떤 소리로 깨워 드릴까요?").assertExists()
        compose.onNodeWithText("기본 알람음").performClick()
        assertThat(picked).isEqualTo("default")
    }

    @Test
    fun missionStep_doneButton_andPicksOneOrNone() {
        var picked: MissionType? = MissionType.QuickMath
        var done = false
        show(
            FirstRunUiState(step = FirstRunStep.Mission),
            FirstRunActions(onMission = { picked = it }, onNext = { done = true }),
        )
        compose.onNodeWithText("알람을 해제할 미션을 골라 주세요").assertExists()
        compose.onNodeWithTag(FIRST_RUN_MISSION_NONE_TAG).performClick()
        assertThat(picked).isNull()
        compose.onNodeWithTag("${MISSION_CARD_TAG}PopQuiz").performClick()
        assertThat(picked).isEqualTo(MissionType.PopQuiz)
        compose.onNodeWithText("완료").performClick()
        assertThat(done).isTrue()
    }

    @Test
    fun missionStep_withoutShakeSensor_doesNotPickShake() {
        var picked: MissionType? = null
        show(
            FirstRunUiState(step = FirstRunStep.Mission, shakeAvailable = false),
            FirstRunActions(onMission = { picked = it }),
        )
        compose.onNodeWithText("Shake Shake").performClick()
        assertThat(picked).isNull()
    }
}
