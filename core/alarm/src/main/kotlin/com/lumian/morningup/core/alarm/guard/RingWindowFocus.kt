package com.lumian.morningup.core.alarm.guard

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 울림 화면(울림, 미션 모두 `RingActivity` 안)이 지금 입력 초점을 갖고 있는지(ISS-060).
 * 전원 메뉴 같은 시스템 창이 위에 떠 있으면 초점이 없어요. 화면 내용은 읽지 않고 이 한 가지만 봐요.
 * 화면이 바꾸고 [PowerGuardService]가 읽어요. 같은 프로세스 안이라 값 하나로 충분해요.
 */
@Singleton
class RingWindowFocus @Inject constructor() {
    @Volatile var focused: Boolean = false
}
