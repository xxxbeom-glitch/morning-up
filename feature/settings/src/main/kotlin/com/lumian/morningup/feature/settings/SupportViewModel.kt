package com.lumian.morningup.feature.settings

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumian.morningup.core.alarm.NowProvider
import com.lumian.morningup.core.data.AlarmRepository
import com.lumian.morningup.core.data.diagnostics.DiagnosticLog
import com.lumian.morningup.core.model.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 보내기 결과. 화면이 토스트로 알려요. */
enum class SupportSendResult { MailOpened, NoMailApp }

/**
 * T7-1 문의 쓰기 임시 방식(서버 전, plan-11-support 1절, TSK-289). 보내기를 누르면 앱 기록 zip을 만들고
 * 메일 앱을 열어요. 사용자가 메일 앱에서 직접 보내요. 공용 Firebase는 쓰지 않아요.
 */
@HiltViewModel
class SupportViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val permissions: PermissionChecker,
    private val repository: AlarmRepository,
    private val log: DiagnosticLog,
) : ViewModel() {
    private val appContext = context.applicationContext
    private val now = NowProvider.System

    private val _preview = MutableStateFlow<String?>(null)

    /** T7-3 보낼 기록 보기에 보여 줄 summary.txt. */
    val preview: StateFlow<String?> = _preview.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    fun loadPreview(versionName: String) {
        viewModelScope.launch { _preview.value = withContext(Dispatchers.IO) { summary(versionName) } }
    }

    fun send(
        form: SupportForm,
        photos: List<Uri>,
        mailTexts: SupportMailTexts,
        versionName: String,
        onResult: (SupportSendResult) -> Unit,
    ) {
        if (_sending.value) return
        _sending.value = true
        viewModelScope.launch {
            val zip = if (form.withLog) {
                withContext(Dispatchers.IO) {
                    val csv = SupportReport.csv(
                        log.entries().map { SupportLogLine(it.at, it.event, it.alarmId, it.detail) },
                        now.millis(),
                        now.now().zone,
                    )
                    SupportReport.writeZip(File(appContext.cacheDir, SUPPORT_DIR), ticket(), summary(versionName), csv)
                }
            } else {
                null
            }
            val uris = buildList {
                zip?.let { add(FileProvider.getUriForFile(appContext, appContext.packageName + AUTHORITY_SUFFIX, it)) }
                addAll(photos)
            }
            val subject = mailTexts.subject.format(mailTexts.typeLabel, ticket())
            val missing = permissions.report.value.missingRequired.joinToString { it.name }.ifEmpty { mailTexts.none }
            val footer = mailTexts.footer.format(
                "${Build.MANUFACTURER} ${Build.MODEL}",
                Build.VERSION.RELEASE,
                versionName,
                missing,
            )
            val body = form.body.trim() + footer
            _sending.value = false
            onResult(if (openMail(subject, body, uris)) SupportSendResult.MailOpened else SupportSendResult.NoMailApp)
        }
    }

    private fun openMail(subject: String, body: String, uris: List<Uri>): Boolean {
        val intent = SupportMail.intent(appContext.packageManager, BuildConfig.SUPPORT_EMAIL, subject, body, uris)
            ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            appContext.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private suspend fun summary(versionName: String): String = SupportReport.summary(
        ticket = ticket(),
        device = SupportDevice("${Build.MANUFACTURER} ${Build.MODEL}", Build.VERSION.RELEASE, versionName),
        report = permissions.report.value,
        alarms = repository.all(),
        exits = exits(),
        zone = now.now().zone,
    )

    private fun exits(): List<SupportExit> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()
        val manager = appContext.getSystemService(ActivityManager::class.java) ?: return emptyList()
        return runCatching {
            manager.getHistoricalProcessExitReasons(appContext.packageName, 0, SupportReport.MAX_EXITS)
                .map { SupportExit(it.timestamp, it.reason, it.description) }
        }.getOrDefault(emptyList())
    }

    private fun ticket(): String = SupportReport.ticketId(installId(), now.now().toLocalDate())

    private fun installId(): String {
        val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_INSTALL, null) ?: UUID.randomUUID().toString().also {
            prefs.edit { putString(KEY_INSTALL, it) }
        }
    }

    private companion object {
        const val PREFS = "support"
        const val KEY_INSTALL = "install_id"
        const val SUPPORT_DIR = "support"
        const val AUTHORITY_SUFFIX = ".support"
    }
}

/** T7-1에서 쓴 것. 화면 회전에도 남게 화면이 들고 있어요. */
data class SupportForm(
    val type: SupportType? = null,
    val frequency: SupportFrequency? = null,
    val body: String = "",
    val withLog: Boolean = false,
    val agreed: Boolean = false,
)

/** 메일 제목 틀('[일어나 문의] %1$s, %2$s'), 종류 이름, 본문 아래 붙는 폰 정보 틀, 꺼진 권한이 없을 때 글. */
data class SupportMailTexts(val subject: String, val typeLabel: String, val footer: String, val none: String)
