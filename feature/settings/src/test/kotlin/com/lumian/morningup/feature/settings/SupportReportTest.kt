package com.lumian.morningup.feature.settings

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionReport
import java.io.ByteArrayInputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.zip.ZipInputStream
import org.junit.Test

/** 문의하기 앱 기록 파일(TSK-289): 이름 숨김, 7일, 2MB, 보내기 조건. */
class SupportReportTest {
    private val zone = ZoneId.of("Asia/Seoul")
    private val day = 24L * 60 * 60 * 1000

    @Test
    fun ticketId_installPrefixAndDate() {
        assertThat(SupportReport.ticketId("ab12-cd34-ef56", LocalDate.of(2026, 11, 27))).isEqualTo("ab12cd34-20261127")
    }

    @Test
    fun canSend_needsTypeTenCharsAndConsent() {
        assertThat(SupportReport.canSend(null, "열 글자가 넘는 내용이에요", true)).isFalse()
        assertThat(SupportReport.canSend(SupportType.Bug, "짧아요", true)).isFalse()
        assertThat(SupportReport.canSend(SupportType.Bug, "열 글자가 넘는 내용이에요", false)).isFalse()
        assertThat(SupportReport.canSend(SupportType.Bug, "열 글자가 넘는 내용이에요", true)).isTrue()
    }

    @Test
    fun summary_numbersAlarms() {
        val alarm = Alarm(id = 7, time = LocalTime.of(6, 30), repeatDays = setOf(DayOfWeek.MONDAY))
        val text = SupportReport.summary(
            ticket = "t",
            device = SupportDevice("Samsung SM-F731N", "14", "1.0.0"),
            report = PermissionReport(AlarmPermission.entries.associateWith { true }),
            alarms = listOf(alarm),
            exits = emptyList(),
            zone = zone,
        )
        assertThat(text).contains("alarm 1: 06:30")
        assertThat(text).contains("days=MON")
        assertThat(text).doesNotContain("id=7")
    }

    @Test
    fun csv_keepsSevenDaysAndCapsSize() {
        val now = 100 * day
        val lines = listOf(
            SupportLogLine(now - 8 * day, "old", null, ""),
            SupportLogLine(now - day, "ring", 1, "a,b"),
        )
        val csv = SupportReport.csv(lines, now, zone)
        assertThat(csv).doesNotContain("old")
        assertThat(csv).contains("\"a,b\"")
        val many = (1..1000).map { SupportLogLine(now - it * 1000L, "e$it", null, "x".repeat(100)) }
        assertThat(SupportReport.csv(many, now, zone, maxBytes = 5000).toByteArray().size).isAtMost(5000)
    }

    @Test
    fun zip_hasTwoFiles() {
        val bytes = SupportReport.zip("summary", "at,event,alarm,detail\n")
        val names = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) names += (zip.nextEntry ?: break).name
        }
        assertThat(names).containsExactly("summary.txt", "log.csv")
        assertThat(bytes.size).isAtMost(SupportReport.MAX_ZIP_BYTES)
    }
}
