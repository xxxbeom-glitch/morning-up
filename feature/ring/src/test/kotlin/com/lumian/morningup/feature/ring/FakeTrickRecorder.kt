package com.lumian.morningup.feature.ring

/** 가짜 버튼 속이기 기록. [reason]을 넣으면 접근성 때문에 꺼진 것으로 봐요. 남긴 기록은 [events]에 쌓여요. */
class FakeTrickRecorder(var reason: String? = null) : TrickRecorder {
    data class Event(val event: String, val alarmId: Long?, val detail: String, val kept: Boolean)

    val events = mutableListOf<Event>()

    override fun offReason(): String? = reason

    override fun record(event: String, alarmId: Long?, detail: String, kept: Boolean) {
        events += Event(event, alarmId, detail, kept)
    }
}
