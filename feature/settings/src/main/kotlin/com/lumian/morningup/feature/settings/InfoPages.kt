package com.lumian.morningup.feature.settings

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide
import com.lumian.morningup.core.designsystem.component.MorningSettingsCard
import com.lumian.morningup.core.designsystem.component.MorningTopBar
import com.lumian.morningup.core.designsystem.motion.morningClickable
import com.lumian.morningup.core.designsystem.theme.MorningDimens
import com.lumian.morningup.core.designsystem.theme.MorningTheme

private val ChevronSize = 16.dp
private const val OPEN_ROTATION = 180f

/** 앱에 넣은 오픈소스. 새로 가져오면 여기에 한 줄 더해요(dev-plan '오픈소스는 T5에 바로 적어요'). */
private val Licenses = listOf(
    "AndroidX (Core, Activity, Lifecycle, Navigation, Room, DataStore)" to "Apache License 2.0",
    "Jetpack Compose, Material 3" to "Apache License 2.0",
    "Dagger Hilt" to "Apache License 2.0",
    "Kotlin, kotlinx.coroutines, kotlinx.serialization" to "Apache License 2.0",
    "Timber" to "Apache License 2.0",
    "Lucide Icons (composables lucide)" to "ISC License",
    "Pretendard" to "SIL Open Font License 1.1",
)

private const val OFL_ASSET = "licenses/pretendard-OFL.txt"

/** 공통 틀: 회색 바탕, Top Bar Back, 아래로 넘기는 카드 묶음(카드형 규칙). */
@Composable
private fun InfoFrame(title: String, onBack: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MorningTheme.colors.backgroundBase)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        MorningTopBar(title = title, onBack = onBack, backContentDescription = stringResource(R.string.settings_back))
        Column(
            modifier = Modifier.padding(
                start = MorningDimens.layoutScreenMargin,
                end = MorningDimens.layoutScreenMargin,
                top = MorningDimens.space16,
                bottom = MorningDimens.space32,
            ),
            verticalArrangement = Arrangement.spacedBy(MorningDimens.layoutCardGap),
        ) { content() }
    }
}

/** T5 오픈소스 라이선스. 이름과 라이선스를 적고, 글꼴 OFL 전문은 접었다 펴서 보여 줘요. */
@Composable
fun LicensesPage(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val ofl = remember {
        runCatching { context.assets.open(OFL_ASSET).bufferedReader().use { it.readText() } }.getOrDefault("")
    }
    val colors = MorningTheme.colors
    InfoFrame(stringResource(R.string.settings_licenses), onBack, modifier) {
        MorningSettingsCard(title = stringResource(R.string.licenses_card)) {
            Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space16)) {
                Licenses.forEach { (name, license) ->
                    Column {
                        Text(name, style = MorningTheme.typography.bodyMediumStrong, color = colors.textPrimary)
                        Text(license, style = MorningTheme.typography.captionMedium, color = colors.textSecondary)
                    }
                }
            }
        }
        if (ofl.isNotEmpty()) {
            MorningSettingsCard(title = stringResource(R.string.licenses_full)) {
                Fold(title = "Pretendard (SIL OFL 1.1)", body = ofl)
            }
        }
    }
}

/** 자주 묻는 질문. 앱 안에서 접었다 펴는 목록이에요(design-system T1). */
@Composable
fun FaqPage(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val items = listOf(
        R.string.faq_q1 to R.string.faq_a1,
        R.string.faq_q2 to R.string.faq_a2,
        R.string.faq_q3 to R.string.faq_a3,
        R.string.faq_q4 to R.string.faq_a4,
        R.string.faq_q5 to R.string.faq_a5,
        R.string.faq_q6 to R.string.faq_a6,
        R.string.faq_q7 to R.string.faq_a7,
    )
    InfoFrame(stringResource(R.string.settings_faq), onBack, modifier) {
        MorningSettingsCard(title = stringResource(R.string.faq_card)) {
            Column(verticalArrangement = Arrangement.spacedBy(MorningDimens.space8)) {
                items.forEach { (q, a) -> Fold(title = stringResource(q), body = stringResource(a)) }
            }
        }
    }
}

@Composable
private fun Fold(title: String, body: String) {
    var open by rememberSaveable(title) { mutableStateOf(false) }
    val colors = MorningTheme.colors
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .morningClickable(haptic = null, role = Role.Button) { open = !open }
                .padding(vertical = MorningDimens.space12),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MorningTheme.typography.bodyMediumStrong,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Lucide.ChevronDown,
                null,
                Modifier.size(ChevronSize).rotate(if (open) OPEN_ROTATION else 0f),
                tint = colors.textTertiary,
            )
        }
        if (open) {
            Text(
                text = body,
                style = MorningTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = MorningDimens.space12),
            )
        }
    }
}
