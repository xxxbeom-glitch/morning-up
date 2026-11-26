package com.lumian.morningup.core.model.engine

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** 알람 볼륨 0이면 최대의 30%로 올리기(결정 143, C-14). */
class VolumeFloorTest {
    @Test
    fun zero_raisesToThirtyPercentRoundedUp() {
        assertThat(VolumeFloor.raisedTo(current = 0, min = 0, max = 15)).isEqualTo(5)
        assertThat(VolumeFloor.raisedTo(current = 0, min = 0, max = 7)).isEqualTo(3)
    }

    @Test
    fun atDeviceMinimum_alsoRaises() {
        // 기기 최소가 1인 폰은 1이 가장 작은 크기예요.
        assertThat(VolumeFloor.raisedTo(current = 1, min = 1, max = 15)).isEqualTo(5)
    }

    @Test
    fun tinyMax_raisesAtLeastOneStep() {
        assertThat(VolumeFloor.raisedTo(current = 0, min = 0, max = 1)).isEqualTo(1)
    }

    @Test
    fun aboveMinimum_keeps() {
        assertThat(VolumeFloor.raisedTo(current = 1, min = 0, max = 15)).isNull()
        assertThat(VolumeFloor.raisedTo(current = 0, min = 0, max = 0)).isNull()
    }

    @Test
    fun restore_onlyWhenUserDidNotChange() {
        assertThat(VolumeFloor.shouldRestore(current = 5, raised = 5)).isTrue()
        assertThat(VolumeFloor.shouldRestore(current = 9, raised = 5)).isFalse()
    }
}
