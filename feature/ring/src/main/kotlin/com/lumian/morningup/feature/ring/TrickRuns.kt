package com.lumian.morningup.feature.ring

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 울림 한 번(trickKey) 동안의 버튼 속이기를 앱이 살아 있는 동안 들고 있어요(ISS-048).
 * 울림 중 화면을 껐다 켜서 화면이 다시 그려지거나 액티비티가 새로 떠도 처음 만진 때, 버튼 자리, 끝났는지가 그대로라
 * '처음 만진 때부터 20초'가 이어지고 trick_shown도 한 번만 찍혀요. 다시 울려 trickKey가 바뀌면 새로 만들어요.
 */
@Singleton
class TrickRuns @Inject constructor() {
    private var key: Long? = null
    private var run: TrickRun? = null

    /** [key]가 지난번과 같으면 지난번 값을 주고 [create]는 부르지 않아요. */
    fun get(key: Long, create: () -> TrickRun?): TrickRun? {
        if (this.key == key) return run
        this.key = key
        run = create()
        return run
    }
}
