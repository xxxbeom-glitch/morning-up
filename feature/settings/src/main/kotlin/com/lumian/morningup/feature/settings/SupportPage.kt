package com.lumian.morningup.feature.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.designsystem.component.MorningButton
import com.lumian.morningup.core.designsystem.component.MorningChoiceChip
import com.lumian.morningup.core.designsystem.component.MorningDialog
import com.lumian.morningup.core.designsystem.component.MorningSelectMark
import com.lumian.morningup.core.designsystem.component.MorningSettingRow
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.component.SettingRowState
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val PhotoSize = 72.dp
private val BodyMinHeight = 148.dp
private const val THUMB_PX = 256

/**
 * T7 문의하기 임시판(TSK-289, 피그마 T7-1 `1214:19072`). 서버가 붙기 전이라 목록과 답변 화면(T7, T7-2)은 숨기고
 * 바로 T7-1 쓰기를 열어요. 보내기는 앱 기록 zip을 붙여 메일 앱으로 넘겨요. [bugFirst]면 '오류 신고'가 골라진 채 열려요.
 */
@Composable
fun SupportRoute(
    versionName: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bugFirst: Boolean = false,
    viewModel: SupportViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val sending by viewModel.sending.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    var form by rememberSaveable(stateSaver = SupportFormSaver) {
        mutableStateOf(if (bugFirst) SupportForm(type = SupportType.Bug, withLog = true) else SupportForm())
    }
    val photos = rememberSaveable(saver = UriListSaver) { mutableListOf<Uri>().toMutableStateList() }
    var showLog by rememberSaveable { mutableStateOf(false) }
    var askExit by rememberSaveable { mutableStateOf(false) }
    val dirty = form.type != null || form.body.isNotBlank() || photos.isNotEmpty()
    val leave = { if (dirty) askExit = true else onBack() }

    val subject = stringResource(R.string.support_mail_subject)
    val footer = stringResource(R.string.settings_feedback_body)
    val none = stringResource(R.string.settings_feedback_none)
    val typeLabels = SupportType.entries.associateWith { stringResource(it.label) }

    if (showLog) {
        LaunchedEffect(Unit) { viewModel.loadPreview(versionName) }
        BackHandler { showLog = false }
        SupportLogPage(summary = preview, onBack = { showLog = false }, modifier = modifier)
        return
    }
    BackHandler(onBack = leave)
    SupportPage(
        form = form,
        onForm = { form = it },
        photos = photos,
        onAddPhotos = { added -> photos.addAll(added.filter { it !in photos }.take(SupportReport.MAX_PHOTOS - photos.size)) },
        onRemovePhoto = { photos.remove(it) },
        onViewLog = { showLog = true },
        sending = sending,
        onSend = {
            val type = form.type ?: return@SupportPage
            Toast.makeText(context, R.string.support_mail_hint, Toast.LENGTH_SHORT).show()
            viewModel.send(
                form = form,
                photos = photos.toList(),
                mailTexts = SupportMailTexts(subject, typeLabels.getValue(type), footer, none),
                versionName = versionName,
            ) { result ->
                if (result == SupportSendResult.NoMailApp) {
                    Toast.makeText(context, R.string.support_no_mail, Toast.LENGTH_SHORT).show()
                }
            }
        },
        onBack = leave,
        modifier = modifier,
    )
    if (askExit) {
        MorningDialog(
            title = stringResource(R.string.support_exit_title),
            message = stringResource(R.string.support_exit_message),
            dismissLabel = stringResource(R.string.support_exit_keep),
            confirmLabel = stringResource(R.string.support_exit_discard),
            onDismiss = { askExit = false },
            onConfirm = {
                askExit = false
                onBack()
            },
        )
    }
}

/** T7-1 문의 쓰기 화면. 상태는 부르는 쪽이 들고 있어요(시험하기 쉽게). */
@Composable
fun SupportPage(
    form: SupportForm,
    onForm: (SupportForm) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    photos: List<Uri> = emptyList(),
    onAddPhotos: (List<Uri>) -> Unit = {},
    onRemovePhoto: (Uri) -> Unit = {},
    onViewLog: () -> Unit = {},
    sending: Boolean = false,
) {
    val colors = MorningTheme.colors
    val typography = MorningTheme.typography
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBase)
            .statusBarsPadding(),
    ) {
        MorningTopBar(
            title = stringResource(R.string.support_title),
            onBack = onBack,
            backContentDescription = stringResource(R.string.settings_back),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = MorningDimens.layoutScreenMargin,
                    end = MorningDimens.layoutScreenMargin,
                    top = MorningDimens.space16,
                    bottom = MorningDimens.space16,
                ),
            verticalArrangement = Arrangement.spacedBy(MorningDimens.space24),
        ) {
            Section(stringResource(R.string.support_type_title)) {
                ChipRow(SupportType.entries, form.type, { stringResource(it.label) }) { type ->
                    // 오류 신고면 앱 기록 스위치가 켜진 채, 나머지는 꺼진 채로 시작해요.
                    onForm(form.copy(type = type, withLog = type == SupportType.Bug))
                }
            }
            if (form.type == SupportType.Bug) {
                Section(stringResource(R.string.support_freq_title)) {
                    ChipRow(SupportFrequency.entries, form.frequency, { stringResource(it.label) }) {
                        onForm(form.copy(frequency = it))
                    }
                }
            }
            Section(stringResource(R.string.support_body_title)) {
                BodyField(form.body, hint = stringResource(form.type.hint)) { onForm(form.copy(body = it)) }
                Text(
                    text = stringResource(R.string.support_body_min),
                    style = typography.captionMedium,
                    color = colors.textTertiary,
                    modifier = Modifier.padding(horizontal = MorningDimens.space4),
                )
            }
            Section(stringResource(R.string.support_photo)) {
                PhotoRow(photos, onAddPhotos, onRemovePhoto)
            }
            MorningSettingsCard(title = null) {
                MorningSettingRow(
                    label = stringResource(R.string.support_log),
                    value = stringResource(R.string.support_log_desc),
                    state = if (form.withLog) SettingRowState.On else SettingRowState.Off,
                    onCheckedChange = { onForm(form.copy(withLog = it)) },
                    modifier = Modifier.heightIn(min = SettingsToggleRowHeight),
                )
                LinkText(stringResource(R.string.support_log_view), onViewLog)
            }
            MorningSettingsCard(title = null) {
                Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space8)) {
                    listOf(R.string.support_consent_items, R.string.support_consent_purpose, R.string.support_consent_keep)
                        .forEach { Text(stringResource(it), style = typography.bodySmall, color = colors.textSecondary) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .morningClickable(haptic = null, role = Role.Checkbox) { onForm(form.copy(agreed = !form.agreed)) }
                            .padding(vertical = MorningDimens.space8),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MorningSelectMark(selected = form.agreed)
                        Spacer(Modifier.width(MorningDimens.space8))
                        Text(stringResource(R.string.support_consent_check), style = typography.bodyMedium, color = colors.textBody)
                    }
                }
            }
        }
        MorningButton(
            text = stringResource(R.string.support_send),
            onClick = onSend,
            enabled = !sending && SupportReport.canSend(form.type, form.body, form.agreed),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = MorningDimens.layoutScreenMargin,
                    end = MorningDimens.layoutScreenMargin,
                    bottom = MorningDimens.space16,
                )
                .testTag(SUPPORT_SEND_TAG),
        )
    }
}

/** T7-3 보낼 기록 보기(피그마 `1215:19200`). 맨 위 안내 한 줄과 summary.txt 그대로. */
@Composable
fun SupportLogPage(summary: String?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MorningTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundBase)
            .statusBarsPadding(),
    ) {
        MorningTopBar(
            title = stringResource(R.string.support_log_view),
            onBack = onBack,
            backContentDescription = stringResource(R.string.settings_back),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(MorningDimens.layoutScreenMargin),
            verticalArrangement = Arrangement.spacedBy(MorningDimens.space16),
        ) {
            Text(
                text = stringResource(R.string.support_log_note),
                style = MorningTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = MorningDimens.layoutCardPadding),
            )
            MorningSettingsCard(title = null) {
                Text(
                    text = summary.orEmpty(),
                    style = MorningTheme.typography.captionMedium,
                    color = colors.textBody,
                    modifier = Modifier.padding(vertical = MorningDimens.space8),
                )
            }
        }
    }
}

internal const val SUPPORT_SEND_TAG = "supportSend"

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space12)) {
        Text(
            text = title,
            style = MorningTheme.typography.captionMedium,
            color = MorningTheme.colors.textSecondary,
            modifier = Modifier.padding(horizontal = MorningDimens.space4),
        )
        content()
    }
}

@Composable
private fun <T> ChipRow(items: List<T>, selected: T?, label: @Composable (T) -> String, onPick: (T) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(MorningDimens.space8)) {
        items.forEach { item ->
            MorningChoiceChip(
                label = label(item),
                selected = item == selected,
                onClick = { onPick(item) },
                modifier = Modifier.width(ChipWidth),
            )
        }
    }
}

/** 칩 너비. 시안의 '궁금한 점' 칩이 들어가는 크기로 셋을 같게 둬요. */
private val ChipWidth = 76.dp

@Composable
private fun BodyField(value: String, hint: String, onChange: (String) -> Unit) {
    val colors = MorningTheme.colors
    val style = MorningTheme.typography.bodyMedium.copy(color = colors.textBody)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = BodyMinHeight)
            .background(colors.surfaceCard, RoundedCornerShape(MorningDimens.radiusCard))
            .padding(MorningDimens.layoutCardPadding),
    ) {
        if (value.isEmpty()) Text(hint, style = style, color = colors.textTertiary)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            textStyle = style,
            cursorBrush = SolidColor(colors.primaryDefault),
            modifier = Modifier.fillMaxWidth().testTag(SUPPORT_BODY_TAG),
        )
    }
}

internal const val SUPPORT_BODY_TAG = "supportBody"

@Composable
private fun PhotoRow(photos: List<Uri>, onAdd: (List<Uri>) -> Unit, onRemove: (Uri) -> Unit) {
    val colors = MorningTheme.colors
    // 안드로이드 사진 고르기 창이라 사진 권한을 묻지 않아요.
    val pick = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(SupportReport.MAX_PHOTOS),
    ) { uris -> if (uris.isNotEmpty()) onAdd(uris) }
    val removeLabel = stringResource(R.string.support_photo_remove)
    val addLabel = stringResource(R.string.support_photo_add)
    Row(horizontalArrangement = Arrangement.spacedBy(MorningDimens.space8)) {
        photos.forEach { uri ->
            val bitmap = rememberThumbnail(uri)
            Box(
                modifier = Modifier
                    .size(PhotoSize)
                    .clip(RoundedCornerShape(MorningDimens.radiusTile))
                    .background(colors.surfaceCard)
                    .morningClickable(haptic = null, role = Role.Button) { onRemove(uri) }
                    .semantics { contentDescription = removeLabel },
            ) {
                bitmap?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
        }
        if (photos.size < SupportReport.MAX_PHOTOS) {
            Box(
                modifier = Modifier
                    .size(PhotoSize)
                    .background(colors.surfaceCard, RoundedCornerShape(MorningDimens.radiusTile))
                    .morningClickable(haptic = null, role = Role.Button) {
                        pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    .semantics { contentDescription = addLabel },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.ImagePlus, null, Modifier.size(MorningDimens.space24), tint = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MorningTheme.typography.bodySmallStrong,
        color = MorningTheme.colors.primaryText,
        modifier = Modifier
            .morningClickable(haptic = null, role = Role.Button, onClick = onClick)
            .padding(vertical = MorningDimens.space8),
    )
}

@Composable
private fun rememberThumbnail(uri: Uri): ImageBitmap? {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) { runCatching { thumbnail(context, uri)?.asImageBitmap() }.getOrNull() }
    }.value
}

private fun thumbnail(context: Context, uri: Uri): Bitmap? {
    val resolver = context.contentResolver
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return resolver.loadThumbnail(uri, Size(THUMB_PX, THUMB_PX), null)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= THUMB_PX && bounds.outHeight / (sample * 2) >= THUMB_PX) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}

private val SupportType.label: Int
    get() = when (this) {
        SupportType.Bug -> R.string.support_type_bug
        SupportType.Question -> R.string.support_type_question
        SupportType.Idea -> R.string.support_type_idea
    }

private val SupportFrequency.label: Int
    get() = when (this) {
        SupportFrequency.Always -> R.string.support_freq_always
        SupportFrequency.Sometimes -> R.string.support_freq_sometimes
        SupportFrequency.Once -> R.string.support_freq_once
    }

private val SupportType?.hint: Int
    get() = when (this) {
        SupportType.Question -> R.string.support_body_hint_question
        SupportType.Idea -> R.string.support_body_hint_idea
        else -> R.string.support_body_hint_bug
    }

private val SupportFormSaver = androidx.compose.runtime.saveable.Saver<SupportForm, List<Any?>>(
    save = { listOf(it.type?.name, it.frequency?.name, it.body, it.withLog, it.agreed) },
    restore = {
        SupportForm(
            type = (it[0] as String?)?.let(SupportType::valueOf),
            frequency = (it[1] as String?)?.let(SupportFrequency::valueOf),
            body = it[2] as String,
            withLog = it[3] as Boolean,
            agreed = it[4] as Boolean,
        )
    },
)

private val UriListSaver = androidx.compose.runtime.saveable.Saver<
    androidx.compose.runtime.snapshots.SnapshotStateList<Uri>,
    ArrayList<String>,
    >(
    save = { list -> ArrayList(list.map { it.toString() }) },
    restore = { saved -> saved.map(Uri::parse).toMutableStateList() },
)
