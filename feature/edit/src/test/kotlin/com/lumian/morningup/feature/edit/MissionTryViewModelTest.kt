package com.lumian.morningup.feature.edit

import com.google.common.truth.Truth.assertThat
import com.lumian.morningup.core.alarm.ShakeSupport
import com.lumian.morningup.core.model.MissionSetting
import com.lumian.morningup.core.model.MissionType
import org.junit.Test

/** 미션 확인용 앱 목록과 설정 시트(11-20 사용자 지시). */
class MissionTryViewModelTest {
    @Test
    fun open_startsWithDefault_andStartRemembersValue() {
        val vm = MissionTryViewModel(ShakeSupport { true })
        vm.open(MissionType.QuickMath)
        assertThat(vm.uiState.value.sheet?.setting).isEqualTo(MissionSetting.default(MissionType.QuickMath))

        vm.stepLevel(1)
        vm.stepCount(1)
        val started = vm.start()

        assertThat(started?.level).isEqualTo(2)
        assertThat(started?.count).isEqualTo(2)
        assertThat(vm.uiState.value.sheet).isNull()
        // 다시 열면 마지막으로 시작한 값으로 열려요.
        vm.open(MissionType.QuickMath)
        assertThat(vm.uiState.value.sheet?.setting).isEqualTo(started)
    }

    @Test
    fun steps_stayInRange() {
        val vm = MissionTryViewModel(ShakeSupport { true })
        vm.open(MissionType.PopQuiz)
        repeat(20) { vm.stepLevel(-1) }
        assertThat(vm.uiState.value.sheet?.setting?.level).isEqualTo(1)
    }

    @Test
    fun close_dropsChanges() {
        val vm = MissionTryViewModel(ShakeSupport { true })
        vm.open(MissionType.DropWords)
        vm.stepSpeed(1)
        vm.close()
        vm.open(MissionType.DropWords)
        assertThat(vm.uiState.value.sheet?.setting).isEqualTo(MissionSetting.default(MissionType.DropWords))
    }

    @Test
    fun shake_withoutSensor_doesNotOpen() {
        val vm = MissionTryViewModel(ShakeSupport { false })
        vm.open(MissionType.ShakeShake)
        assertThat(vm.uiState.value.sheet).isNull()
    }
}
