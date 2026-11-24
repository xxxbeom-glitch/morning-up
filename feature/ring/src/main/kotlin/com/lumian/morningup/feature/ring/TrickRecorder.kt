package com.lumian.morningup.feature.ring

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.lumian.morningup.core.alarm.ring.LabLog
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

/**
 * 버튼 속이기(결정 146 2절)를 지금 걸 수 있는지 보고, 실험실 기록을 남기는 곳.
 * 울림 화면 뷰모델이 써요. 시험에서는 가짜로 바꿔 끼워요.
 */
interface TrickRecorder {
    /** 버튼 속이기를 자동으로 꺼야 하는 까닭(2-4). 걸 수 있으면 null이에요. */
    fun offReason(): String?

    /** [LabLog.record]와 같아요. */
    fun record(event: String, alarmId: Long?, detail: String = "", kept: Boolean = false)
}

/**
 * 접근성 자동 꺼짐 까닭(2-4, 사용자 확정 11-24).
 * - `talkback`: 터치 탐색이 켜져 있어요.
 * - `a11y_service`: 스위치 제어 같은 다른 접근성 서비스가 켜져 있어요. 우리 앱의 전원 종료 방지(PowerGuardService)는 세지 않아요.
 * - `no_animation`: 폰 설정 '애니메이션 삭제'(애니메이터 길이 배율 0)예요.
 */
object TrickAccessibility {
    const val TALKBACK = "talkback"
    const val A11Y_SERVICE = "a11y_service"
    const val NO_ANIMATION = "no_animation"

    fun offReason(context: Context): String? {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        if (manager?.isTouchExplorationEnabled == true) return TALKBACK
        val others = manager
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .orEmpty()
            .any { it.resolveInfo?.serviceInfo?.packageName != context.packageName }
        if (others) return A11Y_SERVICE
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        return if (scale == 0f) NO_ANIMATION else null
    }
}

class AndroidTrickRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val labLog: LabLog,
) : TrickRecorder {
    override fun offReason(): String? = TrickAccessibility.offReason(context)

    override fun record(event: String, alarmId: Long?, detail: String, kept: Boolean) =
        labLog.record(event, alarmId, detail, kept)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RingModule {
    @Binds
    abstract fun trickRecorder(impl: AndroidTrickRecorder): TrickRecorder
}
