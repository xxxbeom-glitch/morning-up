package com.lumian.morningup.feature.settings

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionReport

/** 문의 메일 아래쪽에 붙이는 기기 정보. 개인을 알아볼 수 있는 값은 넣지 않아요(결정 201). */
data class ContactMailInfo(
    val versionName: String,
    val device: String,
    val android: String,
    val report: PermissionReport,
)

/** 문의 메일 본문에 쓰는 글(문구 파일에서 읽어요). 시험에서는 직접 채워요. */
data class ContactMailTexts(
    val intro: String,
    val divider: String,
    val version: String,
    val device: String,
    val android: String,
    val on: String,
    val off: String,
    val permissionLabels: Map<AlarmPermission, String>,
)

/**
 * 문의하기 메일 양식(결정 201). 위쪽은 사용자가 쓸 자리, 아래쪽은 자동 정보예요.
 * 알람 권한은 울림에 직접 닿는 것만 켜짐, 꺼짐으로 적어요.
 */
object ContactMailForm {
    /** 본문에 적는 권한 순서. */
    val Permissions = listOf(
        AlarmPermission.ExactAlarm,
        AlarmPermission.Battery,
        AlarmPermission.Notifications,
        AlarmPermission.FullScreen,
        AlarmPermission.Overlay,
    )

    private const val BLANK_LINES = 4

    fun body(texts: ContactMailTexts, info: ContactMailInfo): String = buildString {
        appendLine(texts.intro)
        repeat(BLANK_LINES) { appendLine() }
        appendLine(texts.divider)
        appendLine(texts.version.format(info.versionName))
        appendLine(texts.device.format(info.device))
        appendLine(texts.android.format(info.android))
        Permissions.forEach { permission ->
            val label = texts.permissionLabels[permission] ?: permission.name
            appendLine("$label: ${if (info.report.isGranted(permission)) texts.on else texts.off}")
        }
    }.trimEnd()
}

/**
 * 문의하기를 누르면 메일 앱을 열고 받는 주소, 제목, 본문을 미리 채워요(결정 201).
 * 메일 앱이 여럿이면 안드로이드가 고르기 창을 보여 줘요. 메일 앱이 없으면 주소를 복사하고 알려요.
 * 받는 주소를 아직 넣지 않은 빌드면 준비 중이라고만 알려요.
 */
fun openSupportMail(context: Context, versionName: String, report: PermissionReport) {
    val email = BuildConfig.SUPPORT_EMAIL
    if (email.isBlank()) {
        Toast.makeText(context, R.string.contact_mail_not_ready, Toast.LENGTH_SHORT).show()
        return
    }
    val info = ContactMailInfo(versionName, "${Build.MANUFACTURER} ${Build.MODEL}", Build.VERSION.RELEASE, report)
    val intent = Intent(Intent.ACTION_SENDTO, "mailto:".toUri())
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.contact_mail_subject))
        .putExtra(Intent.EXTRA_TEXT, ContactMailForm.body(contactMailTexts(context), info))
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        context.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.contact_mail_subject), email))
        Toast.makeText(context, R.string.contact_mail_no_app, Toast.LENGTH_LONG).show()
    }
}

/** 화면에서 쓰는 문의하기 동작. 지금 권한 상태를 본문에 넣어요. */
@Composable
fun rememberOpenSupportMail(versionName: String, viewModel: SettingsViewModel = hiltViewModel()): () -> Unit {
    val context = LocalContext.current
    val report by viewModel.report.collectAsStateWithLifecycle()
    val current by rememberUpdatedState(report)
    return remember(context, versionName) { { openSupportMail(context, versionName, current) } }
}

private fun contactMailTexts(context: Context) = ContactMailTexts(
    intro = context.getString(R.string.contact_mail_intro),
    divider = context.getString(R.string.contact_mail_divider),
    version = context.getString(R.string.contact_mail_version),
    device = context.getString(R.string.contact_mail_device),
    android = context.getString(R.string.contact_mail_android),
    on = context.getString(R.string.contact_mail_on),
    off = context.getString(R.string.contact_mail_off),
    permissionLabels = mapOf(
        AlarmPermission.ExactAlarm to context.getString(R.string.contact_mail_exact_alarm),
        AlarmPermission.Battery to context.getString(R.string.contact_mail_battery),
        AlarmPermission.Notifications to context.getString(R.string.contact_mail_notifications),
        AlarmPermission.FullScreen to context.getString(R.string.contact_mail_full_screen),
        AlarmPermission.Overlay to context.getString(R.string.contact_mail_overlay),
    ),
)
