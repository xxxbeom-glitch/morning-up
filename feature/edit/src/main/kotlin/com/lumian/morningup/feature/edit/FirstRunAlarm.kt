package com.lumian.morningup.feature.edit

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.alarm.ring.RingtonePreviewer
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningIconButton
import com.lumian.morningup.core.designsystem.component.MorningRadioRow
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.designsystem.theme.darkMorningColors
import com.lumian.morningup.core.designsystem.theme.lightMorningColors
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.Ringtones
import com.lumian.morningup.core.ui.AlarmBackground
import com.lumian.morningup.core.ui.AlarmBackgrounds
import com.lumian.morningup.core.ui.AlarmText
import com.lumian.morningup.core.ui.BackgroundGroup
import com.lumian.morningup.core.ui.alarmResources
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** O4-4 타일 순서(기획 O4-4). */
private val FIRST_RUN_MISSIONS =
    listOf(MissionType.QuickMath, MissionType.PopQuiz, MissionType.DropWords, MissionType.ShakeShake)

/** 미리 듣기 크기. 폰 알람 볼륨의 50%까지만(갑자기 크게 울리지 않게, 기획 O4-3). */
private const val PREVIEW_VOLUME_PERCENT = 50
private const val BG_COLUMNS = 3

const val FIRST_RUN_NEXT_TAG = "firstRunNext"
const val FIRST_RUN_SKIP_TAG = "firstRunSkip"
const val FIRST_RUN_MISSION_NONE_TAG = "firstRunMissionNone"

/**
 * 첫 알람 만들기(결정 191, O4-1~O4-4). 단계마다 알람 편집과 같은 부품(드럼롤, 배경 타일, 알람음 줄, 미션 카드)을 써요.
 * O4-1 '다음'에서 [needsNotificationCard]가 true면 [notificationCard]를 띄우고, 카드가 닫히면 O4-2로 가요.
 * 끝나면(완료로 저장, 또는 건너뛰기) [onFinished]. [demo](첫 실행 확인용 앱)면 저장하지 않아요.
 */
@Composable
fun FirstRunAlarmRoute(
    onFinished: (FirstRunExit) -> Unit,
    needsNotificationCard: () -> Boolean,
    notificationCard: @Composable (onDone: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    demo: Boolean = false,
    viewModel: FirstRunViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCard by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.exit) { state.exit?.let(onFinished) }
    // O4-1에서는 시스템 뒤로 가기가 그대로 앱을 나가요.
    BackHandler(enabled = state.step != FirstRunStep.Time) { viewModel.back() }
    FirstRunAlarmScreen(
        state = state,
        actions = FirstRunActions(
            onTime = viewModel::setTime,
            onBackground = viewModel::setBackground,
            onSound = viewModel::setSound,
            onMission = viewModel::setMission,
            onNext = {
                if (state.step == FirstRunStep.Time && needsNotificationCard()) {
                    showCard = true
                } else {
                    viewModel.next(save = !demo)
                }
            },
            onBack = { viewModel.back() },
            onSkip = viewModel::skip,
        ),
        modifier = modifier,
    )
    if (showCard) {
        notificationCard {
            showCard = false
            viewModel.next(save = !demo)
        }
    }
}

/** 첫 알람 화면이 부르는 동작. */
data class FirstRunActions(
    val onTime: (Int, Int) -> Unit = { _, _ -> },
    val onBackground: (String?) -> Unit = {},
    val onSound: (String) -> Unit = {},
    val onMission: (MissionType?) -> Unit = {},
    val onNext: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onSkip: () -> Unit = {},
)

/** O4-1~O4-4 한 장. 공통 틀(뒤로, 진행 막대 4칸, 건너뛰기, 제목, 아래 큰 버튼) 안에 단계별 내용을 그려요. */
@Composable
fun FirstRunAlarmScreen(state: FirstRunUiState, actions: FirstRunActions, modifier: Modifier = Modifier) {
    val timeInput = remember { TimeInputState() }
    val step = state.step
    FirstRunFrame(
        step = step,
        title = stringResource(titleOf(step)),
        button = stringResource(if (step.next == null) R.string.first_run_done else R.string.first_run_next),
        buttonEnabled = !state.saving,
        actions = actions,
        modifier = modifier.finishTimeInputOnOutsideTap(timeInput),
    ) {
        when (step) {
            FirstRunStep.Time -> TimeStep(state.draft, actions.onTime, timeInput)
            FirstRunStep.Background -> BackgroundStep(state.draft, actions.onBackground)
            FirstRunStep.Sound -> SoundStep(state.draft.soundId, actions.onSound)
            FirstRunStep.Mission -> MissionStep(state, actions.onMission)
        }
    }
}

private fun titleOf(step: FirstRunStep): Int = when (step) {
    FirstRunStep.Time -> R.string.first_run_time_title
    FirstRunStep.Background -> R.string.first_run_bg_title
    FirstRunStep.Sound -> R.string.first_run_sound_title
    FirstRunStep.Mission -> R.string.first_run_mission_title
}

@Composable
private fun FirstRunFrame(
    step: FirstRunStep,
    title: String,
    button: String,
    buttonEnabled: Boolean,
    actions: FirstRunActions,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MorningTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBase)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TopRowHeight)
                .padding(horizontal = MorningDimens.space4),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(SideSlot), contentAlignment = Alignment.CenterStart) {
                if (step.previous != null) {
                    MorningIconButton(
                        icon = Lucide.ChevronLeft,
                        contentDescription = stringResource(R.string.edit_back),
                        onClick = actions.onBack,
                    )
                }
            }
            FirstRunProgress(step, Modifier.weight(1f))
            Box(Modifier.width(SideSlot), contentAlignment = Alignment.CenterEnd) {
                Text(
                    text = stringResource(R.string.first_run_skip),
                    style = MorningTheme.typography.bodyMediumStrong,
                    color = colors.textTertiary,
                    modifier = Modifier
                        .testTag(FIRST_RUN_SKIP_TAG)
                        .clip(RoundedCornerShape(MorningDimens.space8))
                        .morningClickable(onClick = actions.onSkip)
                        .padding(horizontal = MorningDimens.space12, vertical = MorningDimens.space8),
                )
            }
        }
        Text(
            text = title,
            style = MorningTheme.typography.headlineLarge,
            color = colors.textPrimary,
            modifier = Modifier.padding(
                start = MorningDimens.layoutScreenMargin,
                end = MorningDimens.layoutScreenMargin,
                top = MorningDimens.space16,
                bottom = MorningDimens.space24,
            ),
        )
        Column(Modifier.weight(1f).fillMaxWidth(), content = content)
        MorningButton(
            text = button,
            onClick = actions.onNext,
            enabled = buttonEnabled,
            modifier = Modifier
                .testTag(FIRST_RUN_NEXT_TAG)
                .padding(
                    start = MorningDimens.space16,
                    end = MorningDimens.space16,
                    top = MorningDimens.space16,
                    bottom = MorningDimens.space8,
                ),
        )
    }
}

/** 위 가운데 4칸 진행 막대. 지난 칸과 지금 칸은 칠해요. */
@Composable
private fun FirstRunProgress(step: FirstRunStep, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    val total = FirstRunStep.entries.size
    val label = stringResource(R.string.first_run_progress, step.ordinal + 1, total)
    Row(
        modifier = modifier
            .padding(horizontal = MorningDimens.space8)
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space4),
    ) {
        repeat(total) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(ProgressHeight)
                    .background(
                        if (index <= step.ordinal) colors.primaryDefault else colors.controlTrackOff,
                        RoundedCornerShape(ProgressHeight),
                    ),
            )
        }
    }
}

/** O4-1: 알람 편집과 같은 드럼롤(오전 오후, 시, 분)과 아래 작은 글. */
@Composable
private fun TimeStep(alarm: Alarm, onTime: (Int, Int) -> Unit, input: TimeInputState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MorningDimens.layoutScreenMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(MorningDimens.space24))
        TimeWheels(alarm, onTime, input)
        Spacer(Modifier.height(MorningDimens.space16))
        Text(
            text = stringResource(R.string.first_run_time_note),
            style = MorningTheme.typography.bodySmall,
            color = MorningTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

/** O4-2: 위에 울림 화면 작은 미리보기, 아래 알람 편집 배경 타일(하늘, 자연, 단색). '내 사진'은 빼요. */
@Composable
private fun ColumnScope.BackgroundStep(alarm: Alarm, onSelect: (String?) -> Unit) {
    val named = remember { AlarmBackgrounds.all.filter { it.group != BackgroundGroup.Photo }.groupBy { it.group } }
    val selected = AlarmBackgrounds.all.firstOrNull { it.id == alarm.background } ?: AlarmBackgrounds.all.first()
    RingPreview(
        bg = selected,
        alarm = alarm,
        modifier = Modifier.padding(horizontal = MorningDimens.layoutScreenMargin),
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(BG_COLUMNS),
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(
            start = MorningDimens.layoutScreenMargin,
            end = MorningDimens.layoutScreenMargin,
            top = MorningDimens.space16,
            bottom = MorningDimens.space16,
        ),
        horizontalArrangement = Arrangement.spacedBy(MorningDimens.space12),
        verticalArrangement = Arrangement.spacedBy(MorningDimens.space12),
    ) {
        named.forEach { (group, list) ->
            item(key = "group-$group", span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = stringResource(group.title),
                    style = MorningTheme.typography.bodySmallStrong,
                    color = MorningTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = MorningDimens.space8),
                )
            }
            list.forEach { bg ->
                item(key = bg.id ?: "time-sky") {
                    NamedTile(bg, selected = alarm.background == bg.id, onClick = { onSelect(bg.id) })
                }
            }
        }
    }
}

/** 울림 화면 작은 미리보기: 고른 배경 위에 날짜, 시각, '미션 시작'. */
@Composable
private fun RingPreview(bg: AlarmBackground, alarm: Alarm, modifier: Modifier = Modifier) {
    val resources = alarmResources()
    val ink = remember(bg.darkText) {
        if (bg.darkText) lightMorningColors().textPrimary else darkMorningColors().textPrimary
    }
    val date = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(PreviewHeight)
            .clip(RoundedCornerShape(MorningDimens.radiusTile))
            .background(Brush.verticalGradient(listOf(bg.top, bg.bottom))),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = date, style = MorningTheme.typography.bodySmall, color = ink)
        Text(text = AlarmText.time(resources, alarm.time), style = MorningTheme.typography.displayMedium, color = ink)
        Spacer(Modifier.height(MorningDimens.space8))
        Text(
            text = stringResource(R.string.first_run_preview_mission),
            style = MorningTheme.typography.bodySmallStrong,
            color = ink,
            modifier = Modifier
                .background(ink.copy(alpha = PILL_ALPHA), RoundedCornerShape(MorningDimens.radiusButton))
                .padding(horizontal = MorningDimens.space16, vertical = MorningDimens.space8),
        )
    }
}

/** O4-3: 알람 편집 '알람음'과 같은 줄. 누르면 고르고 바로 들려줘요. 이 단계를 떠나면 소리를 멈춰요. */
@Composable
private fun ColumnScope.SoundStep(selectedId: String, onSelect: (String) -> Unit) {
    val context = LocalContext.current
    val previewer = remember(context) { RingtonePreviewer(context.applicationContext) }
    DisposableEffect(previewer) { onDispose { previewer.stop() } }
    val selected = Ringtones.find(selectedId).id
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(
            start = MorningDimens.space24,
            end = MorningDimens.space24,
            bottom = MorningDimens.space16,
        ),
    ) {
        Ringtones.groups.forEachIndexed { index, group ->
            item(key = "group-${group.name}") {
                Text(
                    text = group.name,
                    style = MorningTheme.typography.bodySmallStrong,
                    color = MorningTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = if (index == 0) 0.dp else MorningDimens.space24),
                )
            }
            items(group.ringtones, key = { it.id }) { tone ->
                MorningRadioRow(
                    label = tone.name,
                    selected = tone.id == selected,
                    onClick = {
                        onSelect(tone.id)
                        previewer.preview(tone.id, PREVIEW_VOLUME_PERCENT)
                    },
                    modifier = Modifier.height(MorningDimens.sizeRowPlain),
                )
            }
        }
    }
}

/** O4-4: '미션 없이'와 미션 카드 4장. 하나만 골라요. 흔들기 센서가 없으면 Shake Shake는 흐리고 누르면 안내 토스트. */
@Composable
private fun ColumnScope.MissionStep(state: FirstRunUiState, onSelect: (MissionType?) -> Unit) {
    val colors = MorningTheme.colors
    val context = LocalContext.current
    val noSensor = stringResource(R.string.first_run_mission_no_sensor)
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(
            start = MorningDimens.layoutScreenMargin,
            end = MorningDimens.layoutScreenMargin,
            bottom = MorningDimens.space16,
        ),
        verticalArrangement = Arrangement.spacedBy(MorningDimens.space8),
    ) {
        item(key = "none") {
            MorningRadioRow(
                label = stringResource(R.string.first_run_mission_none),
                selected = state.mission == null,
                onClick = { onSelect(null) },
                modifier = Modifier
                    .testTag(FIRST_RUN_MISSION_NONE_TAG)
                    .background(colors.backgroundElevated, RoundedCornerShape(MorningDimens.radiusTile))
                    .padding(horizontal = MorningDimens.space16),
            )
        }
        items(FIRST_RUN_MISSIONS, key = { it.name }) { type ->
            val usable = type != MissionType.ShakeShake || state.shakeAvailable
            Box {
                MissionCard(
                    type = type,
                    selected = state.draft.missions.firstOrNull { it.type == type },
                    enabled = usable,
                    onClick = { onSelect(type) },
                )
                if (!usable) {
                    Box(
                        Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { Toast.makeText(context, noSensor, Toast.LENGTH_SHORT).show() },
                    )
                }
            }
        }
    }
}

private const val PILL_ALPHA = 0.16f
private val TopRowHeight = 56.dp
private val SideSlot = 88.dp
private val ProgressHeight = 4.dp
private val PreviewHeight = 180.dp
