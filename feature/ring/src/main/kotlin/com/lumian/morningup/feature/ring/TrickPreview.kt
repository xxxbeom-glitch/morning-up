package com.lumian.morningup.feature.ring

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import com.lumian.morningup.core.alarm.ring.TrickAccessibility
import com.lumian.morningup.core.designsystem.component.MorningIconButton
import com.lumian.morningup.core.designsystem.theme.FixedFontScale
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.ButtonTrick
import com.lumian.morningup.core.model.DayPart
import com.lumian.morningup.core.model.WakeCheckSettingsStore
import com.lumian.morningup.core.ui.AlarmText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.delay

/** '잘했어요'를 보여 주고 설정으로 돌아가기까지. */
private const val PREVIEW_DONE_MILLIS = 1_500L

/** T3-3에서 지금 고른 강도와 속도를 읽어요. 끈 채로 열어도 고른 값으로 해 봐요. */
@HiltViewModel
class TrickPreviewViewModel @Inject constructor(store: WakeCheckSettingsStore) : ViewModel() {
    val trick: ButtonTrick = store.settings.value.lab.let { ButtonTrick(it.trickLevel, it.trickSpeed) }
}

/**
 * T3-3 버튼 속이기 미리 해보기(결정 146 1절, 2-4). 울림 화면(D0)을 소리 없이 띄워요. 위에 '미리 해보기, 소리는 나지
 * 않아요', 오른쪽 위 닫기(Lucide x). 진짜 버튼을 누르면 버튼 자리에 '잘했어요'와 '곧 설정으로 돌아가요'를 보이고 돌아가요.
 * 접근성 자동 꺼짐은 울림 때와 같이 따라요(기록은 남기지 않아요). 꺼지면 위 줄 아래에 까닭을 한 줄 더 보여요(ISS-041).
 */
@Composable
fun TrickPreviewRoute(onClose: () -> Unit, viewModel: TrickPreviewViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val off = remember { TrickAccessibility.offReason(context) != null }
    TrickPreviewScreen(trick = viewModel.trick.takeUnless { off }, onClose = onClose, trickOff = off)
}

@Composable
fun TrickPreviewScreen(
    trick: ButtonTrick?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    trickOff: Boolean = false,
    date: LocalDate = remember { LocalDate.now() },
    time: LocalTime = remember { LocalTime.now().withSecond(0).withNano(0) },
) {
    var done by rememberSaveable { mutableStateOf(false) }
    val dayPart = DayPart.of(time)
    BackHandler(onBack = onClose)
    LaunchedEffect(done) {
        if (done) {
            delay(PREVIEW_DONE_MILLIS)
            onClose()
        }
    }
    MorningTheme(darkTheme = dayPart == DayPart.Night, reduceMotion = MorningTheme.reduceMotion) {
        Box(modifier.fillMaxSize()) {
            if (done) {
                RingSky(dayPart) { PreviewDone(date, time) }
            } else {
                RingingScreen(
                    date = date,
                    time = time,
                    dayPart = dayPart,
                    hasMissions = true,
                    onMainButton = { done = true },
                    background = Alarm.BACKGROUND_PLAIN,
                    trick = trick,
                    trickKey = Unit,
                )
            }
            PreviewTopBar(trickOff, onClose)
        }
    }
}

/** 위 줄: 왼쪽 '미리 해보기, 소리는 나지 않아요'(접근성으로 꺼지면 아래에 안내 한 줄), 오른쪽 닫기(누르는 칸 48). */
@Composable
private fun PreviewTopBar(trickOff: Boolean, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .systemBarsPadding()
            .padding(start = MorningDimens.space24, end = MorningDimens.space12, top = MorningDimens.space12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.ring_preview_top),
                style = MorningTheme.typography.bodySmall,
                color = MorningTheme.colors.textBody,
            )
            if (trickOff) {
                Text(
                    text = stringResource(R.string.ring_preview_trick_off),
                    style = MorningTheme.typography.bodySmall,
                    color = MorningTheme.colors.textBody,
                )
            }
        }
        MorningIconButton(
            icon = Lucide.X,
            contentDescription = stringResource(R.string.ring_preview_close),
            onClick = onClose,
        )
    }
}

/** '잘했어요' 장: 날짜와 시각 그대로, 버튼 자리에 '잘했어요'와 '곧 설정으로 돌아가요'. */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.PreviewDone(date: LocalDate, time: LocalTime) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val resources = LocalContext.current.resources
    Column(
        modifier = Modifier.align(Alignment.Center).systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = AlarmText.dateLong(resources, date), style = typography.bodySmall, color = colors.textBody)
        Spacer(Modifier.height(MorningDimens.space4))
        FixedFontScale {
            Text(text = AlarmText.time(resources, time), style = typography.displayLarge12h, color = colors.textPrimary)
        }
        Spacer(Modifier.height(MorningDimens.space40))
        Box(Modifier.height(MorningDimens.sizeButtonCircle), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.ring_preview_done),
                    style = typography.headlineLarge,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(MorningDimens.space8))
                Text(
                    text = stringResource(R.string.ring_preview_back),
                    style = typography.bodyMedium,
                    color = colors.textBody,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
