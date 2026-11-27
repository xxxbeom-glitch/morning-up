package com.lumian.morningup.feature.settings

import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.net.toUri

/**
 * 문의 메일 인텐트(TSK-289, ISS-067).
 *
 * 첨부가 없으면 `mailto:`로 바로 열어요. 첨부가 있으면 보내기(SEND) 인텐트에 메일 앱 이름을 하나씩 붙여 열어요.
 * 예전처럼 SEND에 `mailto:` 선택자(selector)를 달면, 안드로이드 14부터는 고른 메일 앱 화면이 SEND를 받지 않는다고
 * 시스템이 막아요(맞는 앱 없음, -91). 그래서 `mailto:`를 받는 앱 목록을 먼저 찾고, 그중 첨부 보내기를 받는 앱만 써요.
 * 메신저나 드라이브로는 새지 않아요. 여러 개면 고르기 창, 하나면 바로 열어요.
 */
internal object SupportMail {
    private val MAILTO = "mailto:".toUri()

    fun intent(pm: PackageManager, email: String, subject: String, body: String, uris: List<Uri>): Intent? {
        if (uris.isEmpty()) return Intent(Intent.ACTION_SENDTO, MAILTO).withMail(email, subject, body)
        val mail = packages(pm, Intent(Intent.ACTION_SENDTO, MAILTO))
        val targets = packages(pm, send(uris)).filter { it in mail }
            .map { send(uris).withMail(email, subject, body).setPackage(it) }
        return when (targets.size) {
            0 -> null
            1 -> targets.first()
            else -> Intent.createChooser(targets.first(), null)
                .putExtra(Intent.EXTRA_INITIAL_INTENTS, targets.drop(1).toTypedArray())
        }
    }

    /** 이 인텐트를 받는 앱 이름. 메일 앱은 매니페스트 `<queries>`(mailto)에 적어 둬서 보여요. */
    private fun packages(pm: PackageManager, intent: Intent): List<String> =
        pm.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .distinct()

    private fun send(uris: List<Uri>): Intent =
        Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            if (uris.size == 1) {
                putExtra(Intent.EXTRA_STREAM, uris.first())
            } else {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            }
            clipData = ClipData.newRawUri(null, uris.first()).apply {
                uris.drop(1).forEach { addItem(ClipData.Item(it)) }
            }
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    private fun Intent.withMail(email: String, subject: String, body: String): Intent =
        putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body)
}
