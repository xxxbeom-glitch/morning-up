package com.lumian.morningup.feature.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningButtonStyle
import com.lumian.morningup.core.designsystem.component.MorningMissionCheck
import com.lumian.morningup.core.designsystem.component.MorningModalSheet
import com.lumian.morningup.core.designsystem.component.MorningRadioRow
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.component.MorningValueSwitchRow
import com.lumian.morningup.core.designsystem.component.ValueSwitchType
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.mission.DropWordsSpeed
import com.lumian.morningup.core.ui.AlarmText
import com.lumian.morningup.core.ui.alarmResources

/** 이름표(테스트용). 카드는 `"$MISSION_CARD_TAG${type.name}"`. */
const val MISSION_CARD_TAG = "missionCard"
const val SHEET_LEVEL_TAG = "sheetLevel"
const val SHEET_SPEED_TAG = "sheetSpeed"
const val SHEET_COUNT_TAG = "sheetCount"

/** 피그마 Mission Card: 높이 88(안쪽 16 + 56), 썸네일 56 모서리 12, 카드 사이 8. */
/** 이름과 설명 사이(데모와 같음). */
private val NameGap = 2.dp
private val ThumbSize = 56.dp
private val ThumbRadius = 12.dp
private val PreviewButtonWidth = 106.dp
private const val DISABLED_ALPHA = 0.45f
private const val MILLIS_PER_SECOND = 1_000L

/**
 * S1-2 미션 고르기(전체 화면 페이지). 고른 미션이 고른 순서대로 위로 올라가고, 해제하면 원래 자리로 돌아가요.
 * 카드나 체크 원을 누르면 그 미션 설정 시트가 열려요. 3개를 골랐거나 흔들기 센서가 없으면 그 카드는 누를 수 없어요.
 */
@Composable
fun MissionPickPage(state: EditUiState, actions: EditActions, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBase)
            // 아래에 깔린 S0이 눌리지 않게 막아요.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            MorningTopBar(
                title = stringResource(R.string.edit_mission),
                onBack = actions.onCloseDetail,
                backContentDescription = stringResource(R.string.edit_back),
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = MorningDimens.layoutScreenMargin,
                    end = MorningDimens.layoutScreenMargin,
                    top = MorningDimens.space16,
                    bottom = MorningDimens.space32,
                ),
                verticalArrangement = Arrangement.spacedBy(MorningDimens.space8),
            ) {
                // 맨 위 '사용 안 함'. 누르면 고른 미션을 모두 비워요(미션 없는 알람).
                item(key = "off") {
                    MorningRadioRow(
                        label = stringResource(R.string.edit_off_value),
                        selected = !state.hasMissions,
                        onClick = { actions.onMissions(false) },
                        modifier = Modifier
                            .background(colors.backgroundElevated, RoundedCornerShape(MorningDimens.radiusTile))
                            .padding(horizontal = MorningDimens.space16),
                    )
                }
                items(state.missionOrder, key = { it.name }) { type ->
                    val setting = state.draft.missions.firstOrNull { it.type == type }
                    MissionCard(
                        type = type,
                        selected = setting,
                        enabled = state.canOpen(type),
                        onClick = { actions.onOpenMissionSheet(type) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
        MorningModalSheet(visible = state.missionSheet != null, onDismiss = actions.onCloseMissionSheet) {
            // 닫히는 동안에도 마지막 내용을 그대로 그려요.
            val sheet = state.missionSheet
            if (sheet != null) {
                MissionSheetContent(sheet = sheet, actions = actions)
            }
        }
    }
}

@Composable
private fun MissionCard(
    type: MissionType,
    selected: MissionSetting?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val name = AlarmText.missionName(alarmResources(), type)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("$MISSION_CARD_TAG${type.name}")
            .semantics { this.selected = selected != null }
            .morningClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .background(colors.backgroundElevated, RoundedCornerShape(MorningDimens.radiusTile))
            .padding(MorningDimens.space16)
            .alpha(if (enabled) 1f else DISABLED_ALPHA),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(thumbOf(type)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(ThumbSize).clip(RoundedCornerShape(ThumbRadius)),
        )
        Spacer(Modifier.width(MorningDimens.space16))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = typography.bodyMediumStrong, color = colors.textPrimary)
            Spacer(Modifier.height(NameGap))
            Text(
                text = selected?.let { missionValue(it) } ?: stringResource(descriptionOf(type)),
                style = typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(MorningDimens.space12))
        MorningMissionCheck(selected = selected != null)
    }
}

/** 고른 카드 설명 줄: '1단계, 속도 1, 단어 10개', '1단계, 3문제', '10번'. */
@Composable
internal fun missionValue(setting: MissionSetting): String = when (setting.type) {
    MissionType.DropWords -> stringResource(
        R.string.edit_mission_value_drop_words,
        setting.level,
        setting.speed,
        setting.count,
    )
    MissionType.PopQuiz, MissionType.QuickMath ->
        stringResource(R.string.edit_mission_value_questions, setting.level, setting.count)
    MissionType.ShakeShake -> stringResource(R.string.edit_mission_value_shakes, setting.count)
}

/**
 * S1-3~S1-7 설정 시트 내용. 제목은 미션 이름, 줄은 값 넘기기 줄, 아래 '미리보기'(Soft, 폭 106)와
 * '선택 완료'(안 고른 미션) 또는 '선택 해제'(고른 미션, Secondary).
 */
@Composable
private fun MissionSheetContent(sheet: MissionSheet, actions: EditActions) {
    val setting = sheet.setting
    val typography = MorningTheme.typography
    val levelLabel = stringResource(R.string.edit_mission_level)
    Text(
        text = AlarmText.missionName(alarmResources(), setting.type),
        style = typography.titleMedium,
        color = MorningTheme.colors.textPrimary,
        modifier = Modifier.padding(top = MorningDimens.space4, bottom = MorningDimens.space8),
    )
    if (setting.type != MissionType.ShakeShake) {
        MorningValueSwitchRow(
            label = levelLabel,
            sub = levelExamples(setting.type).getOrNull(setting.level - 1),
            value = stringResource(R.string.edit_mission_level_value, setting.level),
            type = ValueSwitchType.Level,
            canDecrease = sheet.canLevelDown,
            canIncrease = sheet.canLevelUp,
            onDecrease = { actions.onMissionLevel(-1) },
            onIncrease = { actions.onMissionLevel(1) },
            decreaseDescription = stringResource(R.string.edit_mission_decrease, levelLabel),
            increaseDescription = stringResource(R.string.edit_mission_increase, levelLabel),
            tag = SHEET_LEVEL_TAG,
        )
    }
    if (setting.type == MissionType.DropWords) {
        val speedLabel = stringResource(R.string.edit_mission_speed)
        MorningValueSwitchRow(
            label = speedLabel,
            sub = stringResource(
                R.string.edit_mission_speed_sub,
                (DropWordsSpeed.fallMillis(setting.speed) / MILLIS_PER_SECOND).toInt(),
            ),
            value = setting.speed.toString(),
            type = ValueSwitchType.Level,
            canDecrease = sheet.canSpeedDown,
            canIncrease = sheet.canSpeedUp,
            onDecrease = { actions.onMissionSpeed(-1) },
            onIncrease = { actions.onMissionSpeed(1) },
            decreaseDescription = stringResource(R.string.edit_mission_decrease, speedLabel),
            increaseDescription = stringResource(R.string.edit_mission_increase, speedLabel),
            tag = SHEET_SPEED_TAG,
        )
    }
    val countLabel = stringResource(countLabelOf(setting.type))
    MorningValueSwitchRow(
        label = countLabel,
        value = stringResource(countValueOf(setting.type), setting.count),
        type = ValueSwitchType.Count,
        canDecrease = sheet.canCountDown,
        canIncrease = sheet.canCountUp,
        onDecrease = { actions.onMissionCount(-1) },
        onIncrease = { actions.onMissionCount(1) },
        decreaseDescription = stringResource(R.string.edit_mission_decrease, countLabel),
        increaseDescription = stringResource(R.string.edit_mission_increase, countLabel),
        tag = SHEET_COUNT_TAG,
    )
    Spacer(Modifier.height(MorningDimens.space24))
    Row(horizontalArrangement = Arrangement.spacedBy(MorningDimens.space8)) {
        MorningButton(
            text = stringResource(R.string.edit_mission_preview),
            onClick = { actions.onPreview(listOf(setting)) },
            style = MorningButtonStyle.Soft,
            modifier = Modifier.width(PreviewButtonWidth),
        )
        if (sheet.selected) {
            MorningButton(
                text = stringResource(R.string.edit_mission_deselect),
                onClick = actions.onDeselectMission,
                style = MorningButtonStyle.Secondary,
                modifier = Modifier.weight(1f),
            )
        } else {
            MorningButton(
                text = stringResource(R.string.edit_mission_select),
                onClick = actions.onConfirmMission,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun levelExamples(type: MissionType): List<String> = when (type) {
    MissionType.DropWords -> stringArrayResource(R.array.edit_mission_examples_drop_words).toList()
    MissionType.PopQuiz -> stringArrayResource(R.array.edit_mission_examples_pop_quiz).toList()
    MissionType.QuickMath -> stringArrayResource(R.array.edit_mission_examples_quick_math).toList()
    MissionType.ShakeShake -> emptyList()
}

private fun thumbOf(type: MissionType): Int = when (type) {
    MissionType.DropWords -> R.drawable.mission_thumb_drop_words
    MissionType.PopQuiz -> R.drawable.mission_thumb_pop_quiz
    MissionType.QuickMath -> R.drawable.mission_thumb_quick_math
    MissionType.ShakeShake -> R.drawable.mission_thumb_shake_shake
}

private fun descriptionOf(type: MissionType): Int = when (type) {
    MissionType.DropWords -> R.string.edit_mission_desc_drop_words
    MissionType.PopQuiz -> R.string.edit_mission_desc_pop_quiz
    MissionType.QuickMath -> R.string.edit_mission_desc_quick_math
    MissionType.ShakeShake -> R.string.edit_mission_desc_shake_shake
}

private fun countLabelOf(type: MissionType): Int = when (type) {
    MissionType.DropWords -> R.string.edit_mission_words
    MissionType.PopQuiz, MissionType.QuickMath -> R.string.edit_mission_questions
    MissionType.ShakeShake -> R.string.edit_mission_shakes
}

private fun countValueOf(type: MissionType): Int = when (type) {
    MissionType.DropWords -> R.string.edit_mission_words_value
    MissionType.PopQuiz, MissionType.QuickMath -> R.string.edit_mission_questions_value
    MissionType.ShakeShake -> R.string.edit_mission_shakes_value
}
