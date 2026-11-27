package com.lumian.morningup.feature.settings

import com.lumian.morningup.core.model.Alarm
import com.lumian.morningup.core.model.AlarmPermission
import com.lumian.morningup.core.model.PermissionReport
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** 문의 종류(T7-1 첫 줄 칩). */
enum class SupportType { Bug, Question, Idea }

/** 오류 신고일 때 '자주 그런가요?' 칩. */
enum class SupportFrequency { Always, Sometimes, Once }

/** 진단 기록 한 줄(기기 안 DB에서 옮겨 적어요). */
data class SupportLogLine(val at: Long, val event: String, val alarmId: Long?, val detail: String)

/** 앱이 꺼진 이유 한 줄(안드로이드 11부터 읽을 수 있어요). */
data class SupportExit(val at: Long, val reason: Int, val description: String?)

/** 폰 정보. 시험에서 바꿔 넣을 수 있게 따로 받아요. */
data class SupportDevice(val model: String, val android: String, val appVersion: String)

/**
 * 문의하기 앱 기록 파일(plan-11-support 3절, TSK-289). `summary.txt` + `log.csv`를 zip 하나로 묶어요.
 * 알람 이름은 '알람 1'처럼 바꿔 적고, 위치, 다른 앱 목록, 연락처, 메일 주소는 넣지 않아요.
 * zip은 [MAX_ZIP_BYTES]를 넘지 않게 오래된 기록 줄부터 잘라요.
 */
object SupportReport {
    const val MAX_ZIP_BYTES = 2 * 1024 * 1024
    const val LOG_DAYS = 7L
    const val MAX_EXITS = 10
    const val MIN_BODY = 10
    const val MAX_PHOTOS = 3

    private val STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val DAY = DateTimeFormatter.ofPattern("yyyyMMdd")

    /** 문의 번호: 설치 번호 앞 8자 + 날짜(plan-11-support 4절). */
    fun ticketId(installId: String, today: LocalDate): String =
        installId.filter { it.isLetterOrDigit() }.take(8) + "-" + DAY.format(today)

    /** 보내기를 누를 수 있는지: 종류를 고르고, 내용 10자 이상, 동의 체크. */
    fun canSend(type: SupportType?, body: String, agreed: Boolean): Boolean =
        type != null && body.trim().length >= MIN_BODY && agreed

    fun summary(
        ticket: String,
        device: SupportDevice,
        report: PermissionReport,
        alarms: List<Alarm>,
        exits: List<SupportExit>,
        zone: ZoneId,
    ): String = buildString {
        appendLine("ticket: $ticket")
        appendLine("device: ${device.model}")
        appendLine("android: ${device.android}")
        appendLine("app: ${device.appVersion}")
        appendLine()
        appendLine("[permissions]")
        AlarmPermission.entries.forEach { appendLine("${it.name}: ${if (report.isGranted(it)) "on" else "off"}") }
        appendLine("backgroundRestricted: ${report.backgroundRestricted}")
        appendLine("alarmsSilenced: ${report.alarmsSilenced}")
        appendLine()
        appendLine("[alarms]")
        alarms.forEachIndexed { index, alarm ->
            val days = if (alarm.isRepeating) alarm.repeatDays.sorted().joinToString(",") { it.name.take(3) } else "once"
            val next = alarm.nextTriggerAt?.let { stamp(it, zone) } ?: "-"
            appendLine(
                "alarm ${index + 1}: ${alarm.time} days=$days enabled=${alarm.enabled} " +
                    "missions=${alarm.missionTypes.joinToString(",")} ringAgain=${alarm.ringAgain} " +
                    "wakeCheck=${alarm.wakeCheck} volume=${alarm.volumePercent} vibrate=${alarm.vibrate} next=$next",
            )
        }
        appendLine()
        appendLine("[exits]")
        exits.take(MAX_EXITS).forEach { appendLine("${stamp(it.at, zone)} reason=${it.reason} ${it.description.orEmpty()}") }
    }

    /** 최근 [LOG_DAYS]일 진단 기록을 오래된 것부터 CSV로. [maxBytes]를 넘으면 오래된 줄부터 버려요. */
    fun csv(lines: List<SupportLogLine>, now: Long, zone: ZoneId, maxBytes: Int = MAX_ZIP_BYTES): String {
        val since = now - LOG_DAYS * DAY_MILLIS
        val rows = lines.filter { it.at >= since }.sortedBy { it.at }.map { line ->
            listOf(stamp(line.at, zone), line.event, line.alarmId?.toString().orEmpty(), line.detail)
                .joinToString(",") { cell(it) }
        }
        val header = "at,event,alarm,detail"
        var size = header.toByteArray().size + 1
        val kept = ArrayDeque<String>()
        for (row in rows.asReversed()) {
            val bytes = row.toByteArray().size + 1
            if (size + bytes > maxBytes) break
            kept.addFirst(row)
            size += bytes
        }
        return (listOf(header) + kept).joinToString("\n", postfix = "\n")
    }

    /** summary.txt와 log.csv를 zip 바이트로. 넘치면 기록을 반씩 줄여 다시 묶어요. */
    fun zip(summary: String, csv: String): ByteArray {
        var log = csv
        while (true) {
            val bytes = zipOnce(summary, log)
            if (bytes.size <= MAX_ZIP_BYTES || log.lines().size <= 2) return bytes
            val rows = log.trimEnd('\n').lines()
            log = (listOf(rows.first()) + rows.drop(1).let { it.drop(it.size / 2) }).joinToString("\n", postfix = "\n")
        }
    }

    fun writeZip(dir: File, ticket: String, summary: String, csv: String): File {
        dir.mkdirs()
        dir.listFiles()?.forEach { it.delete() }
        return File(dir, "ireona-$ticket.zip").apply { writeBytes(zip(summary, csv)) }
    }

    private fun zipOnce(summary: String, csv: String): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("summary.txt"))
            zip.write(summary.toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("log.csv"))
            zip.write(csv.toByteArray())
            zip.closeEntry()
        }
        return out.toByteArray()
    }

    private fun stamp(millis: Long, zone: ZoneId): String = STAMP.format(Instant.ofEpochMilli(millis).atZone(zone))

    private fun cell(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value

    private const val DAY_MILLIS = 24L * 60 * 60 * 1000
}
