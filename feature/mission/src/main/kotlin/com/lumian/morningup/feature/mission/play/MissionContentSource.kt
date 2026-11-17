package com.lumian.morningup.feature.mission.play

import android.content.Context
import com.lumian.morningup.core.model.mission.MissionContent
import com.lumian.morningup.feature.mission.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json

/** 미션 문제와 단어 목록. 테스트는 작은 목록을 직접 넣어요. */
fun interface MissionContentSource {
    fun load(): MissionContent
}

/**
 * `res/raw/mission_content.json`을 한 번 읽어 둬요. 실제 목록(F15, F16)은 이 파일만 바꾸면 돼요.
 * 파일이 작아서(수십 KB) 처음 미션을 열 때 읽어요.
 */
@Singleton
class RawMissionContentSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : MissionContentSource {
    private val content: MissionContent by lazy {
        context.resources.openRawResource(R.raw.mission_content).bufferedReader().use { reader ->
            Json { ignoreUnknownKeys = true }.decodeFromString(MissionContent.serializer(), reader.readText())
        }
    }

    override fun load(): MissionContent = content
}
