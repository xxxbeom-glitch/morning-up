package com.lumian.morningup.feature.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumian.morningup.core.alarm.ShakeSupport
import com.lumian.morningup.core.designsystem.component.MorningModalSheet
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import com.lumian.morningup.core.model.mission.QuizCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** 미션 확인용 앱 목록 상태. [sheet]가 열린 설정 시트, [remembered]는 미션마다 마지막으로 시작한 값이에요. */
data class MissionTryState(
    val sheet: MissionSheet? = null,
    val remembered: Map<MissionType, MissionSetting> = emptyMap(),
    val shakeAvailable: Boolean = true,
)

/**
 * 미션 확인용 앱 '모닝업 미션'(11-20 사용자 지시)의 목록과 설정 시트 상태.
 * 알람을 만들거나 저장하지 않아요. 고른 값은 이 앱을 켜 둔 동안만 기억해요.
 */
@HiltViewModel
class MissionTryViewModel @Inject constructor(shakeSupport: ShakeSupport) : ViewModel() {
    private val state = MutableStateFlow(MissionTryState(shakeAvailable = shakeSupport.available()))
    val uiState: StateFlow<MissionTryState> = state.asStateFlow()

    /** 카드를 눌러 설정 시트를 열어요. 흔들기 센서가 없으면 Shake Shake는 열리지 않아요. */
    fun open(type: MissionType) = state.update { s ->
        if (type == MissionType.ShakeShake && !s.shakeAvailable) {
            s
        } else {
            s.copy(sheet = MissionSheet(s.remembered[type] ?: MissionSetting.default(type), selected = false))
        }
    }

    fun close() = state.update { it.copy(sheet = null) }

    fun stepLevel(delta: Int) = change { it.copy(level = it.level + delta) }

    fun stepSpeed(delta: Int) = change { it.copy(speed = it.speed + delta) }

    fun stepCount(delta: Int) = change { it.copy(count = it.count + delta * MissionSetting.rangeOf(it.type).countStep) }

    fun toggleQuizCategory(category: QuizCategory?) = change {
        it.copy(quizCategories = MissionSetting.toggleQuizCategory(it.quizCategories, category) ?: it.quizCategories)
    }

    /** '시작하기'. 값을 기억하고 시트를 닫은 뒤 시작할 설정을 돌려줘요. */
    fun start(): MissionSetting? {
        val setting = state.value.sheet?.setting ?: return null
        state.update { it.copy(sheet = null, remembered = it.remembered + (setting.type to setting)) }
        return setting
    }

    private fun change(block: (MissionSetting) -> MissionSetting) = state.update { s ->
        val sheet = s.sheet ?: return@update s
        s.copy(sheet = sheet.copy(setting = block(sheet.setting).normalized()))
    }
}

/**
 * 미션 확인용 앱 첫 화면. S1-2 미션 카드 네 장을 S1-2와 같은 순서로 보여 주고, 누르면 S1-3~S1-7 설정 시트가 떠요.
 * 시트의 '시작하기'를 누르면 [onStart]로 그 미션 하나를 넘겨요(실제 알람과 같은 M1~M4 화면, 소리 없음).
 */
@Composable
fun MissionTryRoute(
    onStart: (MissionSetting) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MissionTryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        EditActions(
            onMissionLevel = viewModel::stepLevel,
            onMissionSpeed = viewModel::stepSpeed,
            onMissionCount = viewModel::stepCount,
            onQuizCategory = viewModel::toggleQuizCategory,
        )
    }
    MissionTryPage(
        state = state,
        onOpen = viewModel::open,
        onClose = viewModel::close,
        actions = actions,
        onStart = { viewModel.start()?.let(onStart) },
        modifier = modifier,
    )
}

@Composable
internal fun MissionTryPage(
    state: MissionTryState,
    onOpen: (MissionType) -> Unit,
    onClose: () -> Unit,
    actions: EditActions,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(MorningTheme.colors.backgroundBase)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            MorningTopBar(title = stringResource(R.string.mission_try_title))
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
                items(EditUiState.MISSION_LIST_ORDER, key = { it.name }) { type ->
                    MissionCard(
                        type = type,
                        // 한 번 시작한 미션은 설명 대신 마지막 값('1단계, 3문제')을 보여 줘요.
                        selected = state.remembered[type],
                        enabled = type != MissionType.ShakeShake || state.shakeAvailable,
                        onClick = { onOpen(type) },
                        showCheck = false,
                    )
                }
            }
        }
        // 닫히는 동안에도 마지막 내용을 그대로 그려요.
        var lastSheet by remember { mutableStateOf<MissionSheet?>(null) }
        if (state.sheet != null) lastSheet = state.sheet
        MorningModalSheet(visible = state.sheet != null, onDismiss = onClose, tag = MISSION_SHEET_TAG) {
            lastSheet?.let { MissionSheetContent(sheet = it, actions = actions, onStart = onStart) }
        }
    }
}
