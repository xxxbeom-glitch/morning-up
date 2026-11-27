package com.lumian.morningup.feature.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningButtonStyle
import com.lumian.morningup.core.designsystem.component.MorningChoiceChip
import com.lumian.morningup.core.designsystem.component.MorningMessageHost
import com.lumian.morningup.core.designsystem.component.MorningMissionCheck
import com.lumian.morningup.core.designsystem.component.MorningRadioRow
import com.lumian.morningup.core.designsystem.component.MorningToast
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.component.MorningWheelPicker
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.mission.QuizCategory
import com.lumian.morningup.core.ui.AlarmText
import com.lumian.morningup.core.ui.alarmResources
import kotlinx.coroutines.delay

/** 이름표(테스트용). 카드는 `"$MISSION_CARD_TAG${type.name}"`. */
const val MISSION_CARD_TAG = "missionCard"
const val SHEET_LEVEL_TAG = "sheetLevel"
const val SHEET_SPEED_TAG = "sheetSpeed"
const val SHEET_COUNT_TAG = "sheetCount"

/** 피그마 Mission Card: 높이 88(안쪽 16 + 56), 썸네일 56 모서리 12, 카드 사이 8. */
/** 이름과 설명 사이(데모와 같음). */
private val NameGap = 2.dp
private val ThumbSize = 56.dp
private val PreviewButtonWidth = 106.dp
private const val DISABLED_ALPHA = 0.45f

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
        // 미션 설정 시트는 편집 화면(EditScreen)이 이 페이지 위에 띄워요.
    }
}

/** 미션 카드 한 장. [showCheck]가 false면 오른쪽 체크 원을 그리지 않아요(미션 확인용 앱 목록). */
@Composable
internal fun MissionCard(
    type: MissionType,
    selected: MissionSetting?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showCheck: Boolean = true,
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
            modifier = Modifier.size(ThumbSize).clip(MissionThumbShape),
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
        if (showCheck) {
            Spacer(Modifier.width(MorningDimens.space12))
            MorningMissionCheck(selected = selected != null)
        }
    }
}

/** 고른 카드 설명 줄: '1단계, 속도 1, 10문제', '1단계, 3문제', '10번'. */
@Composable
internal fun missionValue(setting: MissionSetting): String = when (setting.type) {
    MissionType.DropWords -> stringResource(
        R.string.edit_mission_value_drop_words,
        setting.level,
        setting.speed,
        setting.count,
    )
    MissionType.PopQuiz -> if (setting.allQuizCategories) {
        stringResource(R.string.edit_mission_value_questions, setting.level, setting.count)
    } else {
        stringResource(
            R.string.edit_mission_value_questions_category,
            setting.level,
            setting.count,
            AlarmText.quizCategories(alarmResources(), setting.quizCategories),
        )
    }
    MissionType.QuickMath -> stringResource(R.string.edit_mission_value_questions, setting.level, setting.count)
    MissionType.ShakeShake -> stringResource(R.string.edit_mission_value_shakes, setting.count)
}

/**
 * S1-3~S1-7 설정 시트 내용. 제목은 미션 이름, 줄은 값 넘기기 줄, 아래 '미리보기'(Soft, 폭 106)와
 * '선택 완료'(안 고른 미션) 또는 '선택 해제'(고른 미션, Secondary).
 * [onStart]가 있으면(미션 확인용 앱) 버튼은 '시작하기' 하나예요.
 */
@Composable
internal fun MissionSheetContent(sheet: MissionSheet, actions: EditActions, onStart: (() -> Unit)? = null) {
    val setting = sheet.setting
    val typography = MorningTheme.typography
    Text(
        text = AlarmText.missionName(alarmResources(), setting.type),
        style = typography.titleMedium,
        color = MorningTheme.colors.textPrimary,
        modifier = Modifier.padding(top = MorningDimens.space4, bottom = MorningDimens.space8),
    )
    // 결정 87: 머리 아래 예제 영역(TSK-264로 높이 160, 흔들기만 그림에 맞춰 216). 미션 화면을 덩어리로 그린 스켈레톤 예제가 움직여요(11-18 사용자 지시).
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (setting.type == MissionType.ShakeShake) ShakeExampleHeight else ExampleAreaHeight)
            .clip(RoundedCornerShape(MorningDimens.radiusTile))
            .background(MorningTheme.colors.backgroundRaised)
            .testTag(SHEET_EXAMPLE_TAG),
    ) {
        MissionExample(
            type = setting.type,
            level = setting.level,
            speed = setting.speed,
            count = setting.count,
            modifier = Modifier.fillMaxSize(),
        )
    }
    Spacer(Modifier.height(MorningDimens.space16))
    // TSK-293(시안 TSK-264): 값 줄 대신 큰 드럼롤을 나란히. 위에 이름, 세 줄 56, 가운데 띠, 숫자 40 + 단위 16. 설명 글 없음.
    MissionWheels(missionWheelsOf(setting, actions))
    if (setting.type == MissionType.PopQuiz) {
        QuizCategoryRow(setting.quizCategories, actions.onQuizCategory)
    }
    Spacer(Modifier.height(MorningDimens.space24))
    if (onStart != null) {
        MorningButton(
            text = stringResource(R.string.edit_mission_start),
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
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

/**
 * Pop Quiz '분야' 줄(TSK-284, 시안 S1-6). 제목 오른쪽에 '전체' 또는 'n개 골랐어요', 아래 '전체' 칩과 분야 칩 7개가 감겨요.
 * 마지막 하나를 빼려 하면 빠지지 않고 토스트 '분야를 하나 이상 골라 주세요'.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuizCategoryRow(selected: Set<QuizCategory>, onToggle: (QuizCategory?) -> Unit) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val all = selected.containsAll(MissionSetting.ALL_QUIZ_CATEGORIES)
    var minNotice by remember { mutableIntStateOf(0) }
    LaunchedEffect(minNotice) {
        if (minNotice > 0) {
            delay(CATEGORY_NOTICE_MILLIS)
            minNotice = 0
        }
    }
    Box(Modifier.zIndex(1f).testTag(SHEET_CATEGORY_TAG)) {
        Column(Modifier.padding(top = MorningDimens.space16)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.edit_mission_category),
                    style = typography.bodySmallStrong,
                    color = colors.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (all) {
                        stringResource(R.string.edit_mission_category_all)
                    } else {
                        stringResource(R.string.edit_mission_category_count, selected.size)
                    },
                    style = typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            Spacer(Modifier.height(MorningDimens.space12))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MorningDimens.space8),
                verticalArrangement = Arrangement.spacedBy(MorningDimens.space8),
            ) {
                MorningChoiceChip(
                    label = stringResource(R.string.edit_mission_category_all),
                    selected = all,
                    onClick = { onToggle(null) },
                )
                QuizCategory.entries.forEach { category ->
                    MorningChoiceChip(
                        label = stringResource(AlarmText.quizCategoryRes(category)),
                        selected = category in selected,
                        onClick = {
                            if (MissionSetting.toggleQuizCategory(selected, category) == null) {
                                minNotice++
                            } else {
                                onToggle(category)
                            }
                        },
                    )
                }
            }
        }
        MorningMessageHost(
            message = minNotice.takeIf { it > 0 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = CategoryNoticeOffset),
        ) {
            MorningToast(text = stringResource(R.string.edit_mission_category_min))
        }
    }
}

private const val CATEGORY_NOTICE_MILLIS = 2_000L
private val CategoryNoticeOffset = 64.dp
const val SHEET_CATEGORY_TAG = "missionSheetCategory"

/** 미션 섬네일 모서리. 크기와 상관없이 한 변의 22%예요(TSK-233: 56은 12, 40은 9). */
internal val MissionThumbShape = RoundedCornerShape(percent = 22)

/** 미션 섬네일 도형 아이콘(TSK-233, 벡터). Light와 Dark 모두 같은 그림이에요. */
internal fun thumbOf(type: MissionType): Int = when (type) {
    MissionType.DropWords -> R.drawable.mission_thumb_drop_words
    MissionType.PopQuiz -> R.drawable.mission_thumb_pop_quiz
    MissionType.QuickMath -> R.drawable.mission_thumb_quick_math
    MissionType.ShakeShake -> R.drawable.mission_thumb_shake_shake
}

internal fun descriptionOf(type: MissionType): Int = when (type) {
    MissionType.DropWords -> R.string.edit_mission_desc_drop_words
    MissionType.PopQuiz -> R.string.edit_mission_desc_pop_quiz
    MissionType.QuickMath -> R.string.edit_mission_desc_quick_math
    MissionType.ShakeShake -> R.string.edit_mission_desc_shake_shake
}

/** 미션 설정 시트 예제 영역 높이(피그마 S1-3A 등, TSK-264로 260 → 160). 흔들기는 폰 그림에 맞춰 216. */
private val ExampleAreaHeight = 160.dp
private val ShakeExampleHeight = 216.dp

/** 미션 설정 드럼롤 칸 높이(TSK-264). 세 줄 모두 같아요. */
private val MissionWheelItemHeight = 56.dp

/** 미션 설정 드럼롤 한 칸: 위 이름, 고를 수 있는 값 [range], 지금 값 [value], 숫자 뒤 [unit]. */
internal data class MissionWheel(
    val label: Int,
    val unit: Int,
    val range: IntRange,
    val value: Int,
    val tag: String,
    val onDelta: (Int) -> Unit,
)

/** 미션 종류마다 보여 줄 드럼롤: 흔들기는 횟수만, Drop Words는 난이도, 속도, 단어 수, 나머지는 난이도와 문제 수. */
internal fun missionWheelsOf(setting: MissionSetting, actions: EditActions): List<MissionWheel> {
    val range = MissionSetting.rangeOf(setting.type)
    val level = MissionWheel(
        R.string.edit_mission_level, R.string.edit_mission_unit_level, range.levels, setting.level, SHEET_LEVEL_TAG,
        actions.onMissionLevel,
    )
    val speed = MissionWheel(
        R.string.edit_mission_speed, R.string.edit_mission_unit_level, MissionSetting.SPEEDS, setting.speed,
        SHEET_SPEED_TAG, actions.onMissionSpeed,
    )
    val (countLabel, countUnit) = when (setting.type) {
        MissionType.DropWords -> R.string.edit_mission_words to R.string.edit_mission_unit_words
        MissionType.ShakeShake -> R.string.edit_mission_shakes to R.string.edit_mission_unit_shakes
        MissionType.PopQuiz, MissionType.QuickMath ->
            R.string.edit_mission_questions to R.string.edit_mission_unit_questions
    }
    val count = MissionWheel(
        countLabel, countUnit, range.counts, setting.count, SHEET_COUNT_TAG, actions.onMissionCount,
    )
    return when (setting.type) {
        MissionType.ShakeShake -> listOf(count)
        MissionType.DropWords -> listOf(level, speed, count)
        MissionType.PopQuiz, MissionType.QuickMath -> listOf(level, count)
    }
}

/** 드럼롤 칸을 나란히(시안 TSK-264). 멈춘 칸의 값과 지금 값의 차이를 보내요(액션이 증감이라서). */
@Composable
private fun MissionWheels(wheels: List<MissionWheel>) {
    val colors = MorningTheme.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(MorningDimens.space8)) {
        wheels.forEach { wheel ->
            val label = stringResource(wheel.label)
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = label, style = MorningTheme.typography.bodyMedium, color = colors.textPrimary)
                Spacer(Modifier.height(MorningDimens.space8))
                MorningWheelPicker(
                    items = wheel.range.map { it.toString() },
                    selectedIndex = (wheel.value - wheel.range.first).coerceIn(0, wheel.range.count() - 1),
                    onSelect = { index ->
                        val delta = wheel.range.first + index - wheel.value
                        if (delta != 0) wheel.onDelta(delta)
                    },
                    contentDescription = label,
                    itemHeight = MissionWheelItemHeight,
                    unit = stringResource(wheel.unit),
                    bandColor = colors.backgroundRaised,
                    modifier = Modifier.fillMaxWidth().testTag(wheel.tag),
                )
            }
        }
    }
}

const val SHEET_EXAMPLE_TAG = "missionSheetExample"
