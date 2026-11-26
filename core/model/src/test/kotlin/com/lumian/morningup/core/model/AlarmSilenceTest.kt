package com.lumian.morningup.core.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** 방해 금지가 알람 소리를 막는지(결정 143, TSK-214 결과). */
class AlarmSilenceTest {
    @Test
    fun priority_blocksOnlyWhenAlarmsNotAllowed() {
        assertThat(AlarmSilence.blocks(DndMode.Priority, alarmsAllowed = false)).isTrue()
        assertThat(AlarmSilence.blocks(DndMode.Priority, alarmsAllowed = true)).isFalse()
    }

    @Test
    fun totalSilence_alwaysBlocks() {
        assertThat(AlarmSilence.blocks(DndMode.Total, alarmsAllowed = true)).isTrue()
    }

    @Test
    fun offAlarmsOnlyUnknown_neverBlock() {
        listOf(DndMode.Off, DndMode.AlarmsOnly, DndMode.Unknown).forEach {
            assertThat(AlarmSilence.blocks(it, alarmsAllowed = false)).isFalse()
        }
    }
}
