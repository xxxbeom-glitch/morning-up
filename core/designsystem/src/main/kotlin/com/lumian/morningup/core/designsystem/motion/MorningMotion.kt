package com.lumian.morningup.core.designsystem.motion

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/** 스프링 하나의 값. `dampingRatio`는 감쇠(1이면 튕김 없음), `stiffness`는 강도(클수록 빠름). */
data class SpringToken(val dampingRatio: Float, val stiffness: Float)

/**
 * 움직임 토큰(dev-plan '모션과 인터랙션'). Material 3 Expressive 스프링 체계를 따라요.
 * - spatial: 위치, 크기, 모양이 바뀌는 움직임. 살짝 튕길 수 있어요.
 * - effects: 색, 투명도처럼 튕기면 안 되는 움직임.
 */
object MorningMotion {
    val SpatialFast = SpringToken(dampingRatio = 0.6f, stiffness = 1400f)
    val SpatialDefault = SpringToken(dampingRatio = 0.8f, stiffness = 700f)
    val SpatialSlow = SpringToken(dampingRatio = 0.85f, stiffness = 300f)
    val EffectsFast = SpringToken(dampingRatio = 1f, stiffness = 1600f)
    val EffectsDefault = SpringToken(dampingRatio = 1f, stiffness = 700f)

    /** '애니메이션 줄이기'일 때 위치, 크기 움직임 대신 쓰는 짧은 페이드 길이. */
    const val REDUCED_MOTION_MILLIS = 150

    /** 누를 때 줄어드는 배율. */
    const val PRESS_SCALE = 0.96f

    /** 위치, 크기 움직임. '애니메이션 줄이기'면 튕김 없이 150ms로 바로 가요. */
    fun <T> spatial(token: SpringToken, reduceMotion: Boolean): FiniteAnimationSpec<T> =
        if (reduceMotion) tween(REDUCED_MOTION_MILLIS) else spring(token.dampingRatio, token.stiffness)

    /** 색, 투명도 움직임. '애니메이션 줄이기'여도 그대로 둬요(튕김이 없어서). */
    fun <T> effects(token: SpringToken): FiniteAnimationSpec<T> = spring(token.dampingRatio, token.stiffness)
}
