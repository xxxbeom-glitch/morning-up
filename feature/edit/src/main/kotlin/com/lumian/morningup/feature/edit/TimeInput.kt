package com.lumian.morningup.feature.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.lumian.morningup.core.designsystem.component.WheelItemHeight
import com.lumian.morningup.core.designsystem.theme.FixedFontScale
import com.lumian.morningup.core.designsystem.theme.MorningTheme

/** 시간 드럼롤에서 숫자로 고치는 칸(11-19 사용자 지시 5). */
enum class TimeField { Hour, Minute }

const val TIME_INPUT_TAG = "timeInput"

/**
 * 숫자 입력 규칙(삼성 시계, 구글 시계 방식). 화면과 떨어져 있어 시험하기 쉬워요.
 * - 시(1~12): 첫 숫자가 2~9면 바로 그 시로 정하고 분으로 넘어가요. 1이나 0이면 둘째 숫자를 기다려요.
 *   두 자리가 13 이상이면 둘째 숫자를 받지 않아요. 00은 12시예요.
 * - 분(00~59): 첫 숫자가 6~9면 '0X'분으로 정해요. 두 자리면 정해요.
 * - 아무것도 안 넣고 닫으면 원래 값 그대로예요. 오전, 오후는 숫자 입력으로 바뀌지 않아요.
 */
object TimeEntry {
    data class Step(val text: String, val done: Boolean)

    private const val MAX_DIGITS = 2
    private const val HOURS = 12
    private const val MINUTES = 60
    private const val HOUR_FIRST_DIGIT_DONE = 2
    private const val MINUTE_FIRST_DIGIT_DONE = 6

    fun type(field: TimeField, previous: String, typed: String): Step {
        val digits = typed.filter(Char::isDigit).take(MAX_DIGITS)
        return when (field) {
            TimeField.Hour -> hour(previous, digits)
            TimeField.Minute -> minute(digits)
        }
    }

    private fun hour(previous: String, digits: String): Step = when {
        digits.isEmpty() -> Step("", false)
        digits.length == 1 -> Step(digits, digits.toInt() >= HOUR_FIRST_DIGIT_DONE)
        digits.toInt() > HOURS -> Step(previous.take(1), false)
        else -> Step(digits, true)
    }

    private fun minute(digits: String): Step = when {
        digits.isEmpty() -> Step("", false)
        digits.length == 1 && digits.toInt() >= MINUTE_FIRST_DIGIT_DONE -> Step("0$digits", true)
        digits.length == 1 -> Step(digits, false)
        digits.toInt() >= MINUTES -> Step(digits.take(1), false)
        else -> Step(digits, true)
    }

    /** 입력을 마칠 때의 12시간제 시(1~12). 비었으면 [current]. */
    fun hour12(text: String, current: Int): Int {
        val value = text.toIntOrNull() ?: return current
        return when {
            value == 0 -> HOURS
            value in 1..HOURS -> value
            else -> current
        }
    }

    /** 입력을 마칠 때의 분(0~59). 비었으면 [current]. */
    fun minute(text: String, current: Int): Int {
        val value = text.toIntOrNull() ?: return current
        return if (value in 0 until MINUTES) value else current
    }
}

/**
 * 드럼롤 가운데 줄 자리에 겹쳐 그리는 숫자 입력 칸. 가운데 띠(`surface/card`)와 같은 칠이라 띠 위 글자만 바뀌어 보여요.
 * 글자는 드럼롤 가운데 줄과 같은 `Display/Medium`이고 `primary/text`로 입력 중임을 보여요. 위아래 줄은 그대로라
 * '세 줄 같은 크기, 위아래는 색만 연하게' 규칙을 깨지 않아요. 비어 있으면 원래 값을 `text/disabled`로 보여요.
 */
@Composable
internal fun TimeInputField(
    field: TimeField,
    current: String,
    state: TimeInputState,
    label: String,
    modifier: Modifier = Modifier,
) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    val text = state.text
    val focus = remember { FocusRequester() }
    LaunchedEffect(field) { focus.requestFocus() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(WheelItemHeight)
            .background(colors.surfaceCard),
        contentAlignment = Alignment.Center,
    ) {
        FixedFontScale {
            BasicTextField(
                value = text,
                onValueChange = { typed ->
                    val step = TimeEntry.type(field, text, typed)
                    state.text = step.text
                    if (step.done) state.done(field, step.text, next = true)
                },
                singleLine = true,
                textStyle = typography.displayMedium.copy(color = colors.primaryText, textAlign = TextAlign.Center),
                cursorBrush = SolidColor(colors.primaryDefault),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = if (field == TimeField.Hour) ImeAction.Next else ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { state.done(field, text, next = true) },
                    onDone = { state.done(field, text, next = false) },
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .testTag("$TIME_INPUT_TAG${field.name}")
                    .semantics { contentDescription = label },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (text.isEmpty()) {
                            Text(
                                text = current,
                                style = typography.displayMedium,
                                color = colors.textDisabled,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        inner()
                    }
                },
            )
        }
    }
}

/**
 * 지금 숫자로 고치는 칸과 넣은 글자. [onCommit]은 편집 화면이 매번 새로 달아 줘요(최신 시각으로 저장하려고).
 * 시를 마치면([done], next) 분 칸으로 넘어가고, 분을 마치거나 [finish]하면 닫혀요.
 */
@Stable
internal class TimeInputState {
    var field by mutableStateOf<TimeField?>(null)
        private set
    var text by mutableStateOf("")
    var onCommit: (TimeField, String) -> Unit = { _, _ -> }
    internal var bounds: Rect = Rect.Zero
    internal var rootOffset: Offset = Offset.Zero

    fun start(target: TimeField) {
        if (field == target) return
        finish()
        field = target
        text = ""
    }

    fun done(target: TimeField, value: String, next: Boolean) {
        if (field != target) return
        onCommit(target, value)
        text = ""
        field = if (target == TimeField.Hour && next) TimeField.Minute else null
    }

    fun finish() {
        val current = field ?: return
        onCommit(current, text)
        text = ""
        field = null
    }
}

/** 입력 칸의 화면 위치를 기억해요. 칸 밖을 눌렀는지 가릴 때 써요. */
internal fun Modifier.trackTimeInput(state: TimeInputState): Modifier =
    onGloballyPositioned { state.bounds = it.boundsInWindow() }

/** 화면 어디든 누르는 순간, 입력 칸 밖이면 넣은 값으로 마쳐요. 누른 것 자체는 그대로 아래로 전해요. */
internal fun Modifier.finishTimeInputOnOutsideTap(state: TimeInputState): Modifier = this
    .onGloballyPositioned { state.rootOffset = it.positionInWindow() }
    .pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            if (state.field != null && !state.bounds.contains(down.position + state.rootOffset)) state.finish()
        }
    }
