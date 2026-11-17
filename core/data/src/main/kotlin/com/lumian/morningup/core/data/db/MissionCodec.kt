package com.lumian.morningup.core.data.db

import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/**
 * `alarms.missions` 칸의 글자. v2부터 미션 설정 목록 JSON이에요(예: `[{"type":"QuickMath","level":1,"count":3}]`).
 * v1의 쉼표 이름 목록(`QuickMath,DropWords`)도 읽을 수 있어요(처음 값으로 채움).
 * 모르는 미션(앞으로 지운 미션)이나 망가진 칸은 건너뛰어 알람 자체는 늘 읽혀요.
 */
object MissionCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(missions: List<MissionSetting>): String =
        json.encodeToString(JsonArray.serializer(), JsonArray(missions.map(::toJson)))

    private fun toJson(setting: MissionSetting): JsonElement =
        json.encodeToJsonElement(MissionSetting.serializer(), setting)

    fun decode(text: String): List<MissionSetting> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (!trimmed.startsWith("[")) return decodeLegacy(trimmed)
        // 망가진 글자는 버려요(SerializationException은 IllegalArgumentException의 한 종류예요).
        val array = runCatching { json.parseToJsonElement(trimmed) as? JsonArray }.getOrNull() ?: return emptyList()
        return array.mapNotNull { element ->
            runCatching { json.decodeFromJsonElement(MissionSetting.serializer(), element).normalized() }.getOrNull()
        }
    }

    /** v1 형식: 미션 이름을 쉼표로 이은 글자. */
    fun decodeLegacy(text: String): List<MissionSetting> =
        text.split(',').map(String::trim).filter(String::isNotEmpty).mapNotNull { name ->
            MissionType.entries.firstOrNull { it.name == name }?.let(MissionSetting::default)
        }
}
