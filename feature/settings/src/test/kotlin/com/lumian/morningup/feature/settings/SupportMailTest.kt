package com.lumian.morningup.feature.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/** ISS-067: 첨부가 있으면 선택자 없이 메일 앱 이름을 붙인 보내기 인텐트로 열어요. */
@RunWith(RobolectricTestRunner::class)
class SupportMailTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val pm = context.packageManager
    private val zip = Uri.parse("content://com.lumian.wakeup.support/support/report.zip")

    private fun install(pkg: String, sendTo: Boolean, send: Boolean) {
        val shadow = shadowOf(pm)
        if (sendTo) {
            val c = ComponentName(pkg, "$pkg.Compose")
            shadow.addActivityIfNotPresent(c)
            shadow.addIntentFilterForActivity(
                c,
                IntentFilter(Intent.ACTION_SENDTO).apply { addDataScheme("mailto") },
            )
        }
        if (send) {
            val c = ComponentName(pkg, "$pkg.Share")
            shadow.addActivityIfNotPresent(c)
            shadow.addIntentFilterForActivity(
                c,
                IntentFilter(Intent.ACTION_SEND).apply { addDataType("*/*") },
            )
        }
    }

    @Test
    fun noAttachment_isMailto() {
        val intent = SupportMail.intent(pm, "a@b.c", "s", "b", emptyList())!!
        assertThat(intent.action).isEqualTo(Intent.ACTION_SENDTO)
        assertThat(intent.data.toString()).isEqualTo("mailto:")
    }

    @Test
    fun attachment_oneMailApp_opensItDirectly_withoutSelector() {
        install("com.google.android.gm", sendTo = true, send = true)
        install("com.chat.app", sendTo = false, send = true)
        val intent = SupportMail.intent(pm, "a@b.c", "s", "b", listOf(zip))!!
        assertThat(intent.action).isEqualTo(Intent.ACTION_SEND)
        assertThat(intent.`package`).isEqualTo("com.google.android.gm")
        assertThat(intent.selector).isNull()
        assertThat(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION).isNotEqualTo(0)
        assertThat(intent.getStringArrayExtra(Intent.EXTRA_EMAIL)).asList().containsExactly("a@b.c")
    }

    @Test
    fun attachment_twoMailApps_showsChooser() {
        install("com.google.android.gm", sendTo = true, send = true)
        install("com.samsung.android.email.provider", sendTo = true, send = true)
        val intent = SupportMail.intent(pm, "a@b.c", "s", "b", listOf(zip))!!
        assertThat(intent.action).isEqualTo(Intent.ACTION_CHOOSER)
    }

    @Test
    fun attachment_mailAppWithoutSend_isNoMailApp() {
        install("com.mail.only", sendTo = true, send = false)
        assertThat(SupportMail.intent(pm, "a@b.c", "s", "b", listOf(zip))).isNull()
    }
}
