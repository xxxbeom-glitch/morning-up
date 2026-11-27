package com.lumian.morningup.feature.settings

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionReport
import org.junit.Test

/** 문의하기 메일 양식(결정 201). */
class ContactMailFormTest {
    private val texts = ContactMailTexts(
        intro = "문의 내용을 적어 주세요.",
        divider = "---",
        version = "앱 버전: %1\$s",
        device = "폰 기종: %1\$s",
        android = "안드로이드 버전: %1\$s",
        on = "켜짐",
        off = "꺼짐",
        permissionLabels = mapOf(
            AlarmPermission.ExactAlarm to "정확한 알람",
            AlarmPermission.Battery to "배터리 최적화 예외",
            AlarmPermission.Notifications to "알림",
            AlarmPermission.FullScreen to "전체 화면 알림",
            AlarmPermission.Overlay to "다른 앱 위에 표시",
        ),
    )

    private val report = PermissionReport(
        AlarmPermission.entries.associateWith { true } + mapOf(
            AlarmPermission.Battery to false,
            AlarmPermission.Overlay to false,
        ),
    )

    private val body = ContactMailForm.body(texts, ContactMailInfo("1.2.0", "samsung SM-F731N", "14", report))

    @Test
    fun body_startsWithWritingSpace_thenAutoInfo() {
        assertThat(body).isEqualTo(
            """
            문의 내용을 적어 주세요.




            ---
            앱 버전: 1.2.0
            폰 기종: samsung SM-F731N
            안드로이드 버전: 14
            정확한 알람: 켜짐
            배터리 최적화 예외: 꺼짐
            알림: 켜짐
            전체 화면 알림: 켜짐
            다른 앱 위에 표시: 꺼짐
            """.trimIndent(),
        )
    }

    @Test
    fun body_leavesOutPersonalAndUnrelatedInfo() {
        // 위치 권한, 방해 금지, 알람 목록 같은 것은 넣지 않아요.
        assertThat(body).doesNotContain("Location")
        assertThat(body).doesNotContain("DoNotDisturb")
        assertThat(body.lines()).hasSize(ContactMailForm.Permissions.size + 9)
    }
}
