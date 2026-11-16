package com.lumian.morningup.core.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.lumian.morningup.core.model.engine.RingSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * 울리는 중 상태를 기기 보호 DataStore(`ring_session`)에 저장해요. 앱이 죽었다 살아나도 하던 곳부터 이어가요.
 * 읽을 수 없는 값(앱 업데이트로 모양이 바뀐 경우)은 없는 것으로 보고 지워요.
 */
class RingSessionStore(private val dataStore: DataStore<Preferences>) {
    private val key = stringPreferencesKey("session")
    private val json = Json { ignoreUnknownKeys = true }

    val session: Flow<RingSession?> = dataStore.data.map { prefs -> prefs[key]?.let(::decode) }

    suspend fun load(): RingSession? = session.first()

    suspend fun save(session: RingSession?) {
        dataStore.edit { prefs ->
            if (session == null) {
                prefs.remove(key)
            } else {
                prefs[key] = json.encodeToString(RingSession.serializer(), session)
            }
        }
    }

    private fun decode(text: String): RingSession? = try {
        json.decodeFromString(RingSession.serializer(), text)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
